package com.dayan.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dayan.agent.dto.AiOutlineConfirmDTO;
import com.dayan.agent.dto.AiOutlineRegenDTO;
import com.dayan.agent.dto.AiReviseDTO;
import com.dayan.agent.dto.AiStrategyConfirmDTO;
import com.dayan.agent.dto.AiTitleRegenDTO;
import com.dayan.agent.entity.AiCreationProject;
import com.dayan.agent.model.AiProjectPhase;
import com.dayan.agent.model.AiPurpose;
import com.dayan.agent.service.AiCreationProjectService;
import com.dayan.agent.service.AiCreationPipelineService;
import com.dayan.agent.service.AiGenerateProgressListener;
import com.dayan.agent.service.AiMaterialAssembler;
import com.dayan.agent.util.AiPrompts;
import com.dayan.agent.util.LlmJson;
import com.dayan.agent.vo.*;
import com.dayan.common.aliyun.bailian.BailianChatClient;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.system.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 六阶段流水线编排。阶段温度：digest 0.2 / strategy 0.7 / titles 0.7 / outline 0.5 /
 * body 0.6 / audit 0.2 / polish 0.5 / revise 0.3。
 *
 * <p>状态机：CREATED→DIGESTED→STRATEGY_CONFIRMED→OUTLINE_CONFIRMED→BODY_DONE→IMAGES_DONE→SAVED；
 * strategy/titles 重入会清空下游产物（改策略=全文重做）。全部更新单行 updateById，无长事务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCreationPipelineServiceImpl implements AiCreationPipelineService {

    private static final double DIGEST_TEMPERATURE = 0.2;
    private static final double STRATEGY_TEMPERATURE = 0.7;
    private static final double OUTLINE_TEMPERATURE = 0.5;
    private static final double BODY_TEMPERATURE = 0.6;
    private static final double AUDIT_TEMPERATURE = 0.2;
    private static final double POLISH_TEMPERATURE = 0.5;
    private static final double REVISE_TEMPERATURE = 0.3;

    /** 生成后自检禁语清单（与 system.md 合规红线一致） */
    private static final List<String> BANNED_PHRASES = List.of(
            "保证收益", "稳赚", "包赚", "最高级", "国家级", "顶级", "100%", "百分百", "绝对", "秒杀", "史上");

    private static final Map<Integer, String> FORM_INSTRUCTIONS = Map.of(
            1, "微信公众号精品图文（1200-1500 字，HTML 片段 <h2>/<p>，标题 ≤30 字）",
            2, "朋友圈文案（≤200 字纯文本 + 1-2 emoji + 1 个 #话题标签，标题=首句钩子 ≤20 字）",
            3, "短视频口播脚本（60-90 秒，【画面】【口播】【字幕】分镜，标题 ≤15 字）",
            4, "小红书笔记（600-800 字，Emoji 列表 + #标签段，标题 ≤20 字）");
    private static final Map<String, String> STYLE_INSTRUCTIONS = Map.of(
            "professional", "专业科普风格：用词严谨、逻辑清晰、多用数据与术语，面向对养老品质有要求的家庭决策者",
            "warm", "温情软文风格：以长辈/家庭的真实生活场景切入，情感细腻、语气温暖，引发共鸣",
            "authoritative", "权威报告风格：结论先行、分点论述、数据化表达，塑造平台专业可信形象",
            "colloquial", "口语化风格：短句、亲切、像朋友聊天");
    private static final Map<String, String> AUDIENCE_INSTRUCTIONS = Map.of(
            "children", "为父母养老做决策的子女（30-50 岁）：理性、数据与家庭责任视角，专业可信赖",
            "elder", "老人本人（55-75 岁）：直白温暖、短句、从老人自身利益出发，避免术语",
            "general", "40-70 岁客户及其子女：通俗易懂");
    /** 标题字数上限（titles-regen 用） */
    private static final Map<Integer, Integer> TITLE_LIMITS = Map.of(1, 30, 2, 20, 3, 15, 4, 20);

    private final AiCreationProjectService projectService;
    private final AiMaterialAssembler materialAssembler;
    private final SystemConfigService systemConfigService;
    private final GoodsInfoService goodsInfoService;
    private final BailianChatClient bailianChatClient = new BailianChatClient();

    @Override
    public AiProjectVO digest(Long id) {
        AiCreationProject p = projectService.requireOwned(id);
        List<String> warnings = new ArrayList<>();
        AiMaterialAssembler.MaterialBundle bundle = materialAssembler.assemble(
                p.getChannelCode(), refs(p), p.getTopic(), warnings);
        AiFactDigestVO digest = LlmJson.parse(chat(render("digest", Map.of(
                "purpose_rule", purposeRule(p.getPurpose()),
                "material", StrUtil.blankToDefault(bundle.blocks(), "（无素材）"))), DIGEST_TEMPERATURE),
                AiFactDigestVO.class);
        p.setFactDigest(JSONUtil.toJsonStr(digest));
        p.setWarnings(JSONUtil.toJsonStr(warnings));
        if (AiProjectPhase.CREATED.equals(p.getStatus())) {
            p.setStatus(AiProjectPhase.DIGESTED);
        }
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public AiProjectVO strategy(Long id) {
        AiCreationProject p = requirePhase(id, AiProjectPhase.CREATED, AiProjectPhase.DIGESTED, AiProjectPhase.STRATEGY_CONFIRMED);
        resetDownstreamIfNeeded(p);
        ensureDigest(p);
        List<String> warnings = new ArrayList<>();
        AiMaterialAssembler.MaterialBundle bundle = materialAssembler.assemble(
                p.getChannelCode(), refs(p), p.getTopic(), warnings);
        AiStrategyBundleVO out = LlmJson.parse(chat(render("strategy", Map.of(
                "purpose_rule", purposeRule(p.getPurpose()),
                "form", formInstruction(p.getContentType()),
                "style", styleInstruction(p.getStyleCode()),
                "audience", audienceInstruction(p.getAudience()),
                "topic", StrUtil.blankToDefault(p.getTopic(), "（从素材归纳）"),
                "fact_digest", digestText(p),
                "material", StrUtil.blankToDefault(bundle.blocks(), "（无素材）"))), STRATEGY_TEMPERATURE),
                AiStrategyBundleVO.class);
        AiStrategyVO strategy = out.getStrategyPanel() == null ? new AiStrategyVO() : out.getStrategyPanel();
        strategy.setCoreExecutionPrompt(out.getCoreExecutionPrompt());
        p.setStrategy(JSONUtil.toJsonStr(strategy));
        p.setTitles(JSONUtil.toJsonStr(sanitizeTitles(out.getGeneratedTitles())));
        p.setWarnings(JSONUtil.toJsonStr(warnings));
        p.setStatus(AiProjectPhase.DIGESTED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public AiProjectVO regenerateTitles(Long id, AiTitleRegenDTO dto) {
        AiCreationProject p = requirePhase(id, AiProjectPhase.DIGESTED, AiProjectPhase.STRATEGY_CONFIRMED);
        resetDownstreamIfNeeded(p);
        AiStrategyVO strategy = parseStrategy(p);
        String prompt = render("titles-regen", Map.of(
                "strategy_panel", strategyText(strategy),
                "previous_titles", StrUtil.blankToDefault(p.getTitles(), "（无）"),
                "feedback", StrUtil.blankToDefault(dto == null ? null : dto.getFeedback(), "换一批差异化角度"),
                "title_limit", String.valueOf(TITLE_LIMITS.getOrDefault(p.getContentType(), 30))));
        AiStrategyBundleVO out = LlmJson.parse(chat(prompt, STRATEGY_TEMPERATURE), AiStrategyBundleVO.class);
        p.setTitles(JSONUtil.toJsonStr(sanitizeTitles(out.getGeneratedTitles())));
        p.setStatus(AiProjectPhase.DIGESTED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public AiProjectVO confirmStrategy(Long id, AiStrategyConfirmDTO dto) {
        AiCreationProject p = requirePhase(id, AiProjectPhase.DIGESTED, AiProjectPhase.STRATEGY_CONFIRMED);
        resetDownstreamIfNeeded(p);
        AiStrategyVO strategy = parseStrategy(p);
        if (StrUtil.isNotBlank(dto.getTargetAudience())) strategy.setTargetAudience(dto.getTargetAudience());
        if (StrUtil.isNotBlank(dto.getCorePainPoint())) strategy.setCorePainPoint(dto.getCorePainPoint());
        if (StrUtil.isNotBlank(dto.getViralLogic())) strategy.setViralLogic(dto.getViralLogic());
        if (StrUtil.isNotBlank(dto.getAdvantageHook())) strategy.setAdvantageHook(dto.getAdvantageHook());
        p.setStrategy(JSONUtil.toJsonStr(strategy));
        p.setSelectedTitle(dto.getSelectedTitle().trim());
        p.setStatus(AiProjectPhase.STRATEGY_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public AiProjectVO outline(Long id) {
        AiCreationProject p = requirePhase(id, AiProjectPhase.STRATEGY_CONFIRMED, AiProjectPhase.OUTLINE_CONFIRMED);
        if (Integer.valueOf(2).equals(p.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "朋友圈文案无大纲阶段，请直接生成正文");
        }
        clearBodyDownstream(p);
        AiStrategyVO strategy = parseStrategy(p);
        List<String> warnings = new ArrayList<>();
        AiMaterialAssembler.MaterialBundle bundle = materialAssembler.assemble(
                p.getChannelCode(), refs(p), p.getTopic(), warnings);
        AiOutlineVO outline = callOutline(p, strategy, bundle, null);
        p.setOutline(JSONUtil.toJsonStr(outline));
        p.setStatus(AiProjectPhase.STRATEGY_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public AiProjectVO regenerateOutline(Long id, AiOutlineRegenDTO dto) {
        // 反馈为空 = 直接重跑 outline()
        if (dto == null || StrUtil.isBlank(dto.getFeedback())) {
            return outline(id);
        }
        AiCreationProject p = requirePhase(id, AiProjectPhase.STRATEGY_CONFIRMED, AiProjectPhase.OUTLINE_CONFIRMED);
        if (Integer.valueOf(2).equals(p.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "朋友圈文案无大纲阶段，请直接生成正文");
        }
        clearBodyDownstream(p);
        AiStrategyVO strategy = parseStrategy(p);
        List<String> warnings = new ArrayList<>();
        AiMaterialAssembler.MaterialBundle bundle = materialAssembler.assemble(
                p.getChannelCode(), refs(p), p.getTopic(), warnings);
        AiOutlineVO outline = callOutline(p, strategy, bundle, dto.getFeedback());
        p.setOutline(JSONUtil.toJsonStr(outline));
        p.setStatus(AiProjectPhase.STRATEGY_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public AiProjectVO confirmOutline(Long id, AiOutlineConfirmDTO dto) {
        AiCreationProject p = requirePhase(id, AiProjectPhase.STRATEGY_CONFIRMED, AiProjectPhase.OUTLINE_CONFIRMED);
        AiOutlineVO outline;
        try {
            outline = JSONUtil.toBean(dto.getOutline(), AiOutlineVO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "大纲 JSON 解析失败");
        }
        sanitizeOutline(outline);
        p.setOutline(JSONUtil.toJsonStr(outline));
        p.setStatus(AiProjectPhase.OUTLINE_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public AiProjectVO bodyStream(Long id, AiGenerateProgressListener listener) {
        AiCreationProject p = projectService.requireOwned(id);
        // 朋友圈无大纲阶段，策略确认后直接写正文；其余形态需大纲确认后进入（BODY_DONE=重生成）
        if (Integer.valueOf(2).equals(p.getContentType())) {
            checkPhase(p, AiProjectPhase.STRATEGY_CONFIRMED, AiProjectPhase.BODY_DONE);
        } else {
            checkPhase(p, AiProjectPhase.OUTLINE_CONFIRMED, AiProjectPhase.BODY_DONE);
        }
        long startMillis = System.currentTimeMillis();
        if (AiProjectPhase.BODY_DONE.equals(p.getStatus())) {
            p.setImages(null); // 重生成正文使旧配图失效
        }
        AiStrategyVO strategy = parseStrategy(p);
        List<String> warnings = new ArrayList<>();
        notifyStage(listener, "material", "素材就绪…");
        AiMaterialAssembler.MaterialBundle bundle = materialAssembler.assemble(
                p.getChannelCode(), refs(p), p.getTopic(), warnings);
        // 1. 正文（SSE 流式）
        notifyStage(listener, "body", "正在撰写正文…");
        Map<String, String> bodyVars = new java.util.HashMap<>();
        bodyVars.put("target_audience", StrUtil.nullToEmpty(strategy.getTargetAudience()));
        bodyVars.put("core_pain_point", StrUtil.nullToEmpty(strategy.getCorePainPoint()));
        bodyVars.put("viral_logic", StrUtil.nullToEmpty(strategy.getViralLogic()));
        bodyVars.put("advantage_hook", StrUtil.nullToEmpty(strategy.getAdvantageHook()));
        bodyVars.put("core_execution_prompt", StrUtil.nullToEmpty(strategy.getCoreExecutionPrompt()));
        bodyVars.put("purpose_rule", purposeRule(p.getPurpose()));
        bodyVars.put("platform_rules", platformRules(p.getContentType()));
        bodyVars.put("selected_title", StrUtil.nullToEmpty(p.getSelectedTitle()));
        bodyVars.put("outline_json", outlineText(p));
        bodyVars.put("fact_digest", digestText(p));
        bodyVars.put("material", StrUtil.blankToDefault(bundle.blocks(), "（无素材）"));
        String bodyPrompt = render("body", bodyVars);
        String body = listener == null
                ? chat(bodyPrompt, BODY_TEMPERATURE)
                : bailianChatClient.chatStream(
                        requireConfig("llm.api-key", "AI 凭据未配置，请联系管理员"),
                        requireConfig("llm.api-host", "AI 网关未配置，请联系管理员"),
                        model(), AiPrompts.load("system"), bodyPrompt, BODY_TEMPERATURE, listener::onDelta);
        body = cleanBody(body);
        warnings.addAll(ruleCheck(body, p));
        // 2. 审计（独立 LLM 关卡）
        notifyStage(listener, "audit", "事实核查与合规审计…");
        String auditOut = chat(render("audit", Map.of(
                "fact_digest", digestText(p),
                "material", StrUtil.blankToDefault(bundle.blocks(), "（无素材）"),
                "body", body)), AUDIT_TEMPERATURE);
        String auditedBody = extractTag(auditOut, "revised_article");
        List<AiAuditItemVO> auditLog;
        if (StrUtil.isBlank(auditedBody)) {
            auditedBody = body;
            auditLog = new ArrayList<>();
            auditLog.add(auditItem("解析", "审计输出解析失败，保留原稿"));
        } else {
            auditedBody = cleanBody(auditedBody);
            auditLog = new ArrayList<>(parseAuditLogs(auditOut));
            if (auditLog.isEmpty()) {
                auditLog.add(auditItem("解析", "审计日志缺失，默认通过"));
            }
        }
        // 政策文号确定性兜底：审计模型偶发"日志声称已修但正文未改"，程序化替换素材外文号
        PolicyFix policyFix = enforcePolicyNumbers(auditedBody, bundle.blocks());
        if (policyFix.note() != null) {
            auditedBody = policyFix.body();
            auditLog.add(auditItem("程序修正", policyFix.note()));
            warnings.add(policyFix.note() + "，请复核表述通顺度");
        }
        // 3. 润色 + 打分（防删减：字数 < 95% 丢弃润色版）
        notifyStage(listener, "polish", "润色去 AI 味 + 五维打分…");
        String polishOut = chat(render("polish", Map.of(
                "core_pain_point", StrUtil.nullToEmpty(strategy.getCorePainPoint()),
                "advantage_hook", StrUtil.nullToEmpty(strategy.getAdvantageHook()),
                "platform_rules", platformRules(p.getContentType()),
                "body", auditedBody)), POLISH_TEMPERATURE);
        String polished = extractTag(polishOut, "revised_article");
        String finalBody = auditedBody;
        if (StrUtil.isNotBlank(polished)) {
            polished = cleanBody(polished);
            if (plainLength(polished) >= plainLength(auditedBody) * 0.95) {
                finalBody = polished;
            } else {
                warnings.add("润色版篇幅不足原文 95%，已保留审计版正文");
            }
        } else {
            warnings.add("润色输出解析失败，已保留审计版正文");
        }
        AiScoresVO scores = parseScores(polishOut);
        if (scores == null) {
            scores = new AiScoresVO();
        }
        String critique = extractTag(polishOut, "editor_critique");
        if (StrUtil.isNotBlank(critique)) {
            scores.setEditorCritique(critique);
        }
        // 终稿政策文号兜底（润色可能从策略锚点回带幻觉，程序化替换素材外文号）
        PolicyFix finalFix = enforcePolicyNumbers(finalBody, bundle.blocks());
        if (finalFix.note() != null) {
            finalBody = finalFix.body();
            auditLog.add(auditItem("程序修正", finalFix.note()));
            warnings.add(finalFix.note() + "，请复核表述通顺度");
        }
        p.setBody(finalBody);
        p.setAuditLog(JSONUtil.toJsonStr(auditLog));
        p.setScores(JSONUtil.toJsonStr(scores));
        p.setWarnings(JSONUtil.toJsonStr(warnings));
        p.setStatus(AiProjectPhase.BODY_DONE);
        projectService.updateById(p);
        log.info("AI 正文完成 projectId={} type={} bodyLen={} auditItems={} warnings={} costMs={}",
                id, p.getContentType(), plainLength(finalBody), auditLog.size(), warnings.size(),
                System.currentTimeMillis() - startMillis);
        return projectService.toVO(p);
    }

    @Override
    public AiProjectVO revise(Long id, AiReviseDTO dto) {
        AiCreationProject p = requirePhase(id, AiProjectPhase.BODY_DONE, AiProjectPhase.IMAGES_DONE);
        AiStrategyVO strategy = parseStrategy(p);
        StringBuilder prompt = new StringBuilder(render("revise", Map.of(
                "core_pain_point", StrUtil.nullToEmpty(strategy.getCorePainPoint()),
                "body", StrUtil.nullToEmpty(p.getBody()),
                "feedback", dto.getFeedback().trim())));
        if (StrUtil.isNotBlank(dto.getAnchor())) {
            prompt.append("\n【定位】重点修正包含「").append(dto.getAnchor().trim()).append("」的段落。");
        }
        String revised = cleanBody(chat(prompt.toString(), REVISE_TEMPERATURE));
        if (StrUtil.isBlank(revised)) {
            throw new BusinessException(ErrorCode.BUSINESS, "修订失败，请重试");
        }
        p.setBody(revised);
        List<AiAuditItemVO> auditLog = p.getAuditLog() == null ? new ArrayList<>()
                : new ArrayList<>(JSONUtil.toList(JSONUtil.parseArray(p.getAuditLog()), AiAuditItemVO.class));
        auditLog.add(auditItem("人工勘误", dto.getFeedback().trim()));
        p.setAuditLog(JSONUtil.toJsonStr(auditLog));
        if (AiProjectPhase.IMAGES_DONE.equals(p.getStatus())) {
            p.setStatus(AiProjectPhase.BODY_DONE); // 正文已变，配图需重做
            p.setImages(null);
        }
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    // ---------- 正文阶段私有工具 ----------

    String outlineText(AiCreationProject p) {
        if (Integer.valueOf(2).equals(p.getContentType())) {
            return "（朋友圈文案无大纲，按策略直出）";
        }
        return StrUtil.blankToDefault(p.getOutline(), "（无大纲）");
    }

    /** 正文清洗：去围栏/前置标记行/首尾空白 */
    String cleanBody(String raw) {
        if (raw == null) return "";
        String text = raw.replaceAll("```[a-zA-Z]*", "").replaceAll("```", "");
        text = text.replaceAll("(?m)^【(标题|摘要)】.*$", "");
        return text.trim();
    }

    /** XML 标签提取（audit/polish 输出），找不到返回 null */
    String extractTag(String out, String tag) {
        if (out == null) return null;
        int l = out.indexOf("<" + tag + ">");
        int r = out.indexOf("</" + tag + ">");
        if (l < 0 || r <= l) return null;
        return out.substring(l + tag.length() + 2, r).trim();
    }

    List<AiAuditItemVO> parseAuditLogs(String out) {
        String json = extractTag(out, "audit_logs");
        if (StrUtil.isBlank(json)) return List.of();
        try {
            return JSONUtil.toList(JSONUtil.parseArray(json), AiAuditItemVO.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    AiScoresVO parseScores(String out) {
        String json = extractTag(out, "scores");
        if (StrUtil.isBlank(json)) return null;
        try {
            return JSONUtil.toBean(json, AiScoresVO.class);
        } catch (Exception e) {
            return null;
        }
    }

    int plainLength(String html) {
        if (html == null) return 0;
        return html.replaceAll("<[^>]+>", "").replaceAll("\\s", "").length();
    }

    /** 规则自检（不重写，产出 warnings）：篇幅窗口/禁语/商品融入 */
    List<String> ruleCheck(String body, AiCreationProject p) {
        List<String> warnings = new ArrayList<>();
        int len = plainLength(body);
        int type = p.getContentType() == null ? 1 : p.getContentType();
        int min = switch (type) { case 2 -> 30; case 4 -> 350; case 3 -> 400; default -> 800; };
        int max = switch (type) { case 2 -> 400; case 4 -> 1500; case 3 -> 2500; default -> 2500; };
        if (len < min) warnings.add("正文偏短（约 " + len + " 字），建议重新生成");
        if (len > max) warnings.add("正文超长（约 " + len + " 字），建议重新生成或手动精简");
        String plain = body.replaceAll("<[^>]+>", "");
        for (String banned : BANNED_PHRASES) {
            if (plain.contains(banned)) {
                warnings.add("正文疑似含不合规用语「" + banned + "」，请人工复核");
                break;
            }
        }
        if (AiPurpose.PRODUCT.equals(p.getPurpose()) && p.getMaterialRefs() != null) {
            AiMaterialRefsVO refs = refs(p);
            if (refs.getGoodsCodes() != null && !refs.getGoodsCodes().isEmpty()) {
                boolean mentioned = refs.getGoodsCodes().stream()
                        .map(c -> { try { return goodsInfoService.getDetail(c).getGoodsName(); } catch (Exception e) { return ""; } })
                        .anyMatch(n -> StrUtil.isNotBlank(n) && body.contains(n));
                if (!mentioned) warnings.add("勾选的权益商品未融入正文，建议重新生成");
            }
        }
        return warnings;
    }

    AiAuditItemVO auditItem(String type, String message) {
        AiAuditItemVO item = new AiAuditItemVO();
        item.setType(type);
        item.setMessage(message);
        return item;
    }

    /** 政策文号模式：如 国发〔2024〕99号 / 卫健委发[2023]12号 */
    private static final java.util.regex.Pattern POLICY_NUMBER_PATTERN =
            java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]{1,6}[〔\\[]\\d{4}[〕\\]]\\d{1,3}号");

    record PolicyFix(String body, String note) {}

    /** 素材外政策文号确定性替换为"近期出台的相关政策"（note 为 null 表示无需修正） */
    PolicyFix enforcePolicyNumbers(String body, String material) {
        if (body == null || material == null) {
            return new PolicyFix(StrUtil.nullToEmpty(body), null);
        }
        java.util.regex.Matcher m = POLICY_NUMBER_PATTERN.matcher(body);
        StringBuilder out = new StringBuilder();
        List<String> removed = new ArrayList<>();
        while (m.find()) {
            String token = m.group();
            if (material.contains(token)) {
                m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(token));
            } else {
                removed.add(token);
                m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement("近期出台的相关政策"));
            }
        }
        m.appendTail(out);
        if (removed.isEmpty()) {
            return new PolicyFix(body, null);
        }
        return new PolicyFix(out.toString(), "替换素材外政策文号：" + String.join("、", removed));
    }

    void notifyStage(AiGenerateProgressListener listener, String stage, String message) {
        if (listener != null) listener.onStage(stage, message);
    }

    /** 大纲 LLM 调用（生成/重生成共用；图文/小红书若未规划任何节点配图位，带强调指令重试一次） */
    private AiOutlineVO callOutline(AiCreationProject p, AiStrategyVO strategy,
                                    AiMaterialAssembler.MaterialBundle bundle, String extraDirective) {
        String prompt = outlinePrompt(p, strategy, bundle);
        if (StrUtil.isNotBlank(extraDirective)) {
            prompt = prompt + "\n\n【重生成要求（最高优先级）】" + extraDirective.trim();
        }
        AiOutlineVO outline = LlmJson.parse(chat(prompt, OUTLINE_TEMPERATURE), AiOutlineVO.class);
        sanitizeOutline(outline);
        if (StrUtil.isBlank(extraDirective) && needsImageRetry(p, outline)) {
            AiOutlineVO retry = LlmJson.parse(chat(prompt + "\n\n【重生成要求（最高优先级）】"
                    + "按配图位规划为 3-4 个主体节点补充 imageInsertion：英文 prompt 以 Warm/Bright/Muted lifestyle photograph "
                    + "开头、≤60 词、单一场景并含摄影术语；size 用 1280*720；无需配图的节点保持 null。", OUTLINE_TEMPERATURE),
                    AiOutlineVO.class);
            sanitizeOutline(retry);
            return retry;
        }
        return outline;
    }

    /** 图文/小红书一个配图位都没有 → 重试（朋友圈/脚本无正文配图位，不重试） */
    private boolean needsImageRetry(AiCreationProject p, AiOutlineVO outline) {
        if (Integer.valueOf(2).equals(p.getContentType()) || Integer.valueOf(3).equals(p.getContentType())) {
            return false;
        }
        return outline.getNodes().stream().noneMatch(n -> n.getImageInsertion() != null);
    }

    /** 大纲 prompt 渲染（生成/重生成共用，重生成在尾部追加反馈指令） */
    private String outlinePrompt(AiCreationProject p, AiStrategyVO strategy,
                                 AiMaterialAssembler.MaterialBundle bundle) {
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("core_execution_prompt", StrUtil.nullToEmpty(strategy.getCoreExecutionPrompt()));
        vars.put("target_audience", StrUtil.nullToEmpty(strategy.getTargetAudience()));
        vars.put("core_pain_point", StrUtil.nullToEmpty(strategy.getCorePainPoint()));
        vars.put("viral_logic", StrUtil.nullToEmpty(strategy.getViralLogic()));
        vars.put("advantage_hook", StrUtil.nullToEmpty(strategy.getAdvantageHook()));
        vars.put("purpose_rule", purposeRule(p.getPurpose()));
        vars.put("platform_rules", platformRules(p.getContentType()));
        vars.put("selected_title", StrUtil.nullToEmpty(p.getSelectedTitle()));
        vars.put("topic", StrUtil.blankToDefault(p.getTopic(), "（从素材归纳）"));
        vars.put("fact_digest", digestText(p));
        vars.put("material", StrUtil.blankToDefault(bundle.blocks(), "（无素材）"));
        vars.put("image_count_hint", imageCountHint(p.getContentType()));
        return render("outline", vars);
    }

    /** 大纲清洗：节点非空、补 id、配图规格兜底 */
    void sanitizeOutline(AiOutlineVO outline) {
        if (outline == null || outline.getNodes() == null || outline.getNodes().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS, "模型未返回大纲节点，请重试");
        }
        for (int i = 0; i < outline.getNodes().size(); i++) {
            AiOutlineVO.AiOutlineNode node = outline.getNodes().get(i);
            if (StrUtil.isBlank(node.getId())) {
                node.setId("node_" + (i + 1));
            }
            if (node.getImageInsertion() != null) {
                if (StrUtil.isBlank(node.getImageInsertion().getSource())) {
                    node.getImageInsertion().setSource("ai_generated");
                }
                if (StrUtil.isBlank(node.getImageInsertion().getSize())) {
                    node.getImageInsertion().setSize("1280*720");
                }
            }
        }
        if (outline.getCoverImage() != null && StrUtil.isBlank(outline.getCoverImage().getSize())) {
            outline.getCoverImage().setSize("1024*1024");
        }
    }

    /** outline/body 重入前清空 body 侧产物（保留 strategy/titles） */
    void clearBodyDownstream(AiCreationProject p) {
        if (!AiProjectPhase.STRATEGY_CONFIRMED.equals(p.getStatus())) {
            p.setBody(null);
            p.setAuditLog(null);
            p.setScores(null);
            p.setImages(null);
            p.setStatus(AiProjectPhase.STRATEGY_CONFIRMED);
        }
    }

    String imageCountHint(Integer contentType) {
        return switch (contentType) {
            case 3 -> "仅规划 coverImage 1 张（1024*1024），所有 nodes 的 imageInsertion 必须为 null";
            case 4 -> "coverImage 1 张（1080*1440）+ 节点配位合计 2-4 张（1280*720）";
            default -> "coverImage 1 张（1024*1024）+ 正文节点配图 3-4 张（1280*720）";
        };
    }

    // ---------- 公共设施（后续任务复用） ----------

    /** 渲染 ai-prompts/{name}.md */
    String render(String name, Map<String, String> vars) {
        return AiPrompts.render(AiPrompts.load(name), vars);
    }

    /** 非流式 chat（system.md 为系统提示） */
    String chat(String prompt, double temperature) {
        return bailianChatClient.chat(
                requireConfig("llm.api-key", "AI 凭据未配置，请联系管理员"),
                requireConfig("llm.api-host", "AI 网关未配置，请联系管理员"),
                model(), AiPrompts.load("system"), prompt, temperature);
    }

    String purposeRule(String purpose) {
        String p = AiPurpose.PRODUCT.equals(purpose) || AiPurpose.PARK.equals(purpose)
                || AiPurpose.SCIENCE.equals(purpose) ? purpose : AiPurpose.SCIENCE;
        return AiPrompts.load("purpose/" + p);
    }

    String formInstruction(Integer contentType) {
        String s = FORM_INSTRUCTIONS.get(contentType);
        if (s == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "内容形态取值 1-4");
        return s;
    }

    String styleInstruction(String styleCode) {
        return StrUtil.blankToDefault(STYLE_INSTRUCTIONS.get(styleCode), "自然流畅的行业写作风格");
    }

    String audienceInstruction(String audience) {
        return StrUtil.blankToDefault(AUDIENCE_INSTRUCTIONS.get(audience), AUDIENCE_INSTRUCTIONS.get("general"));
    }

    /** platform/{mp|xhs|moment|script}.md */
    String platformRules(Integer contentType) {
        return switch (contentType) {
            case 2 -> AiPrompts.load("platform/moment");
            case 3 -> AiPrompts.load("platform/script");
            case 4 -> AiPrompts.load("platform/xhs");
            default -> AiPrompts.load("platform/mp");
        };
    }

    AiMaterialRefsVO refs(AiCreationProject p) {
        return StrUtil.isBlank(p.getMaterialRefs()) ? new AiMaterialRefsVO()
                : JSONUtil.toBean(p.getMaterialRefs(), AiMaterialRefsVO.class);
    }

    AiStrategyVO parseStrategy(AiCreationProject p) {
        if (StrUtil.isBlank(p.getStrategy())) {
            throw new BusinessException(ErrorCode.BUSINESS, "请先生成策略");
        }
        return JSONUtil.toBean(p.getStrategy(), AiStrategyVO.class);
    }

    String digestText(AiCreationProject p) {
        return StrUtil.isBlank(p.getFactDigest()) ? "（无）" : p.getFactDigest();
    }

    String strategyText(AiStrategyVO s) {
        return "受众画像：" + s.getTargetAudience() + "\n核心痛点：" + s.getCorePainPoint()
                + "\n爆款逻辑：" + s.getViralLogic() + "\n优势放大器：" + s.getAdvantageHook();
    }

    /** 从 STRATEGY_CONFIRMED 重入：清空下游产物（改策略=全文重做） */
    void resetDownstreamIfNeeded(AiCreationProject p) {
        if (AiProjectPhase.STRATEGY_CONFIRMED.equals(p.getStatus())
                || AiProjectPhase.OUTLINE_CONFIRMED.equals(p.getStatus())
                || AiProjectPhase.BODY_DONE.equals(p.getStatus())
                || AiProjectPhase.IMAGES_DONE.equals(p.getStatus())) {
            p.setOutline(null);
            p.setBody(null);
            p.setAuditLog(null);
            p.setScores(null);
            p.setImages(null);
            p.setSelectedTitle(null);
            p.setStatus(AiProjectPhase.DIGESTED);
        }
    }

    /** factDigest 为空先消化（strategy 自动前置） */
    void ensureDigest(AiCreationProject p) {
        if (StrUtil.isBlank(p.getFactDigest())) {
            List<String> warnings = new ArrayList<>();
            AiMaterialAssembler.MaterialBundle bundle = materialAssembler.assemble(
                    p.getChannelCode(), refs(p), p.getTopic(), warnings);
            AiFactDigestVO digest = LlmJson.parse(chat(render("digest", Map.of(
                    "purpose_rule", purposeRule(p.getPurpose()),
                    "material", StrUtil.blankToDefault(bundle.blocks(), "（无素材）"))), DIGEST_TEMPERATURE),
                    AiFactDigestVO.class);
            p.setFactDigest(JSONUtil.toJsonStr(digest));
            p.setWarnings(JSONUtil.toJsonStr(warnings));
            if (AiProjectPhase.CREATED.equals(p.getStatus())) {
                p.setStatus(AiProjectPhase.DIGESTED);
            }
        }
    }

    /** 状态守卫：加载并校验（仅允许预期状态进入） */
    AiCreationProject requirePhase(Long id, String... expected) {
        AiCreationProject p = projectService.requireOwned(id);
        return checkPhase(p, expected);
    }

    /** 已加载实体的状态守卫（免二次查库） */
    AiCreationProject checkPhase(AiCreationProject p, String... expected) {
        for (String s : expected) {
            if (s.equals(p.getStatus())) return p;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "当前阶段不允许该操作（status=" + p.getStatus() + "）");
    }

    /** 标题清洗：去空、限 5 条、分数夹取 */
    List<AiTitleVO> sanitizeTitles(List<AiTitleVO> titles) {
        if (titles == null || titles.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS, "模型未返回标题，请重试");
        }
        return titles.stream()
                .filter(t -> t != null && StrUtil.isNotBlank(t.getTitle()))
                .peek(t -> {
                    if (t.getViralScore() == null) t.setViralScore(80);
                    t.setViralScore(Math.max(70, Math.min(99, t.getViralScore())));
                })
                .limit(5)
                .toList();
    }

    String requireConfig(String key, String message) {
        String value = getConfig(key);
        if (StrUtil.isBlank(value)) throw new BusinessException(ErrorCode.BUSINESS, message);
        return value;
    }

    String getConfig(String key) {
        return systemConfigService.getValue("llm", key);
    }

    String model() {
        return StrUtil.blankToDefault(getConfig("llm.chat-model"), "qwen-plus");
    }
}
