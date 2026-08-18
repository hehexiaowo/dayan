package com.dayan.tool.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dayan.tool.dto.AiMaterialBlockDTO;
import com.dayan.tool.dto.AiOutlineConfirmDTO;
import com.dayan.tool.dto.AiOutlineRegenDTO;
import com.dayan.tool.dto.AiReviseDTO;
import com.dayan.tool.dto.AiStrategyConfirmDTO;
import com.dayan.tool.dto.AiTitleRegenDTO;
import com.dayan.tool.dto.ToolAiCreatorContentCmd;
import com.dayan.tool.entity.ToolAiCreator;
import com.dayan.tool.model.ToolAiCreatorPhase;
import com.dayan.tool.model.AiPurpose;
import com.dayan.tool.service.AiClientHolder;
import com.dayan.tool.service.ToolAiCreatorContentSaver;
import com.dayan.tool.service.ToolAiCreatorService;
import com.dayan.tool.service.ToolAiCreatorPipelineService;
import com.dayan.tool.service.AiGenerateProgressListener;
import com.dayan.tool.service.AiImageProgressListener;
import com.dayan.tool.util.AiPrompts;
import com.dayan.tool.util.LlmJson;
import com.dayan.tool.vo.*;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.oss.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class ToolAiCreatorPipelineServiceImpl implements ToolAiCreatorPipelineService {

    private static final double DIGEST_TEMPERATURE = 0.2;
    private static final double STRATEGY_TEMPERATURE = 0.7;
    private static final double OUTLINE_TEMPERATURE = 0.5;
    private static final double BODY_TEMPERATURE = 0.6;
    private static final double AUDIT_TEMPERATURE = 0.2;
    private static final double POLISH_TEMPERATURE = 0.5;
    private static final double REVISE_TEMPERATURE = 0.3;

    /** 前端供材快照渲染总量上限（超出截断并记 warning） */
    private static final int MATERIAL_TOTAL_MAX = 8000;
    /** 单张配图轮询上限 */
    private static final long IMAGE_POLL_TIMEOUT_MS = 90_000L;
    /** 正文配图占位符：[AI_IMAGE_COVER] / [AI_IMAGE_1..N] */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[AI_IMAGE_[A-Z0-9_]+]");

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

    private final ToolAiCreatorService projectService;
    private final StorageService storageService;
    private final AiClientHolder aiClientHolder;
    private final ToolAiCreatorContentSaver contentSaver;

    @Override
    public ToolAiCreatorVO digest(Long id) {
        ToolAiCreator p = projectService.requireOwned(id);
        List<String> warnings = new ArrayList<>();
        String material = materialsText(p, warnings);
        AiFactDigestVO digest = LlmJson.parse(chat(render("digest", Map.of(
                "purpose_rule", purposeRule(p.getPurpose()),
                "material", material)), DIGEST_TEMPERATURE),
                AiFactDigestVO.class);
        p.setFactDigest(JSONUtil.toJsonStr(digest));
        p.setWarnings(JSONUtil.toJsonStr(warnings));
        if (ToolAiCreatorPhase.CREATED.equals(p.getStatus())) {
            p.setStatus(ToolAiCreatorPhase.DIGESTED);
        }
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiCreatorVO strategy(Long id) {
        ToolAiCreator p = requirePhase(id, ToolAiCreatorPhase.CREATED, ToolAiCreatorPhase.DIGESTED, ToolAiCreatorPhase.STRATEGY_CONFIRMED);
        resetDownstreamIfNeeded(p);
        ensureDigest(p);
        List<String> warnings = new ArrayList<>();
        String material = materialsText(p, warnings);
        AiStrategyBundleVO out = LlmJson.parse(chat(render("strategy", Map.of(
                "purpose_rule", purposeRule(p.getPurpose()),
                "form", formInstruction(p.getContentType()),
                "style", styleInstruction(p.getStyleCode()),
                "audience", audienceInstruction(p.getAudience()),
                "topic", StrUtil.blankToDefault(p.getTopic(), "（从素材归纳）"),
                "fact_digest", digestText(p),
                "material", material)), STRATEGY_TEMPERATURE),
                AiStrategyBundleVO.class);
        AiStrategyVO strategy = out.getStrategyPanel() == null ? new AiStrategyVO() : out.getStrategyPanel();
        strategy.setCoreExecutionPrompt(out.getCoreExecutionPrompt());
        p.setStrategy(JSONUtil.toJsonStr(strategy));
        p.setTitles(JSONUtil.toJsonStr(sanitizeTitles(out.getGeneratedTitles())));
        p.setWarnings(JSONUtil.toJsonStr(warnings));
        p.setStatus(ToolAiCreatorPhase.DIGESTED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiCreatorVO regenerateTitles(Long id, AiTitleRegenDTO dto) {
        ToolAiCreator p = requirePhase(id, ToolAiCreatorPhase.DIGESTED, ToolAiCreatorPhase.STRATEGY_CONFIRMED);
        resetDownstreamIfNeeded(p);
        AiStrategyVO strategy = parseStrategy(p);
        String prompt = render("titles-regen", Map.of(
                "strategy_panel", strategyText(strategy),
                "previous_titles", StrUtil.blankToDefault(p.getTitles(), "（无）"),
                "feedback", StrUtil.blankToDefault(dto == null ? null : dto.getFeedback(), "换一批差异化角度"),
                "title_limit", String.valueOf(TITLE_LIMITS.getOrDefault(p.getContentType(), 30))));
        AiStrategyBundleVO out = LlmJson.parse(chat(prompt, STRATEGY_TEMPERATURE), AiStrategyBundleVO.class);
        p.setTitles(JSONUtil.toJsonStr(sanitizeTitles(out.getGeneratedTitles())));
        p.setStatus(ToolAiCreatorPhase.DIGESTED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiCreatorVO confirmStrategy(Long id, AiStrategyConfirmDTO dto) {
        ToolAiCreator p = requirePhase(id, ToolAiCreatorPhase.DIGESTED, ToolAiCreatorPhase.STRATEGY_CONFIRMED);
        resetDownstreamIfNeeded(p);
        AiStrategyVO strategy = parseStrategy(p);
        if (StrUtil.isNotBlank(dto.getTargetAudience())) strategy.setTargetAudience(dto.getTargetAudience());
        if (StrUtil.isNotBlank(dto.getCorePainPoint())) strategy.setCorePainPoint(dto.getCorePainPoint());
        if (StrUtil.isNotBlank(dto.getViralLogic())) strategy.setViralLogic(dto.getViralLogic());
        if (StrUtil.isNotBlank(dto.getAdvantageHook())) strategy.setAdvantageHook(dto.getAdvantageHook());
        p.setStrategy(JSONUtil.toJsonStr(strategy));
        p.setSelectedTitle(dto.getSelectedTitle().trim());
        p.setStatus(ToolAiCreatorPhase.STRATEGY_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiCreatorVO outline(Long id) {
        ToolAiCreator p = requirePhase(id, ToolAiCreatorPhase.STRATEGY_CONFIRMED, ToolAiCreatorPhase.OUTLINE_CONFIRMED);
        if (Integer.valueOf(2).equals(p.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "朋友圈文案无大纲阶段，请直接生成正文");
        }
        clearBodyDownstream(p);
        AiStrategyVO strategy = parseStrategy(p);
        AiOutlineVO outline = callOutline(p, strategy, materialsText(p, new ArrayList<>()), null);
        p.setOutline(JSONUtil.toJsonStr(outline));
        p.setStatus(ToolAiCreatorPhase.STRATEGY_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiCreatorVO regenerateOutline(Long id, AiOutlineRegenDTO dto) {
        // 反馈为空 = 直接重跑 outline()
        if (dto == null || StrUtil.isBlank(dto.getFeedback())) {
            return outline(id);
        }
        ToolAiCreator p = requirePhase(id, ToolAiCreatorPhase.STRATEGY_CONFIRMED, ToolAiCreatorPhase.OUTLINE_CONFIRMED);
        if (Integer.valueOf(2).equals(p.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "朋友圈文案无大纲阶段，请直接生成正文");
        }
        clearBodyDownstream(p);
        AiStrategyVO strategy = parseStrategy(p);
        AiOutlineVO outline = callOutline(p, strategy, materialsText(p, new ArrayList<>()), dto.getFeedback());
        p.setOutline(JSONUtil.toJsonStr(outline));
        p.setStatus(ToolAiCreatorPhase.STRATEGY_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiCreatorVO confirmOutline(Long id, AiOutlineConfirmDTO dto) {
        ToolAiCreator p = requirePhase(id, ToolAiCreatorPhase.STRATEGY_CONFIRMED, ToolAiCreatorPhase.OUTLINE_CONFIRMED);
        AiOutlineVO outline;
        try {
            outline = JSONUtil.toBean(dto.getOutline(), AiOutlineVO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "大纲 JSON 解析失败");
        }
        sanitizeOutline(outline);
        p.setOutline(JSONUtil.toJsonStr(outline));
        p.setStatus(ToolAiCreatorPhase.OUTLINE_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiCreatorVO bodyStream(Long id, AiGenerateProgressListener listener) {
        ToolAiCreator p = projectService.requireOwned(id);
        // 朋友圈无大纲阶段，策略确认后直接写正文；其余形态需大纲确认后进入（BODY_DONE=重生成）
        if (Integer.valueOf(2).equals(p.getContentType())) {
            checkPhase(p, ToolAiCreatorPhase.STRATEGY_CONFIRMED, ToolAiCreatorPhase.BODY_DONE);
        } else {
            checkPhase(p, ToolAiCreatorPhase.OUTLINE_CONFIRMED, ToolAiCreatorPhase.BODY_DONE);
        }
        long startMillis = System.currentTimeMillis();
        if (ToolAiCreatorPhase.BODY_DONE.equals(p.getStatus())) {
            p.setImages(null); // 重生成正文使旧配图失效
        }
        AiStrategyVO strategy = parseStrategy(p);
        List<String> warnings = new ArrayList<>();
        notifyStage(listener, "material", "素材就绪…");
        String material = materialsText(p, warnings);
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
        bodyVars.put("material", material);
        String bodyPrompt = render("body", bodyVars);
        String body = listener == null
                ? chat(bodyPrompt, BODY_TEMPERATURE)
                : aiClientHolder.chatClient().chatStream(
                        aiClientHolder.requireConfig("api-key", "AI 凭据未配置，请联系管理员"),
                        aiClientHolder.requireConfig("api-host", "AI 网关未配置，请联系管理员"),
                        aiClientHolder.chatModel(), AiPrompts.load("system"), bodyPrompt, BODY_TEMPERATURE, listener::onDelta);
        body = cleanBody(body);
        warnings.addAll(ruleCheck(body, p));
        // 2. 审计（独立 LLM 关卡）
        notifyStage(listener, "audit", "事实核查与合规审计…");
        String auditOut = chat(render("audit", Map.of(
                "fact_digest", digestText(p),
                "material", material,
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
        PolicyFix policyFix = enforcePolicyNumbers(auditedBody, material);
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
        PolicyFix finalFix = enforcePolicyNumbers(finalBody, material);
        if (finalFix.note() != null) {
            finalBody = finalFix.body();
            auditLog.add(auditItem("程序修正", finalFix.note()));
            warnings.add(finalFix.note() + "，请复核表述通顺度");
        }
        p.setBody(finalBody);
        p.setAuditLog(JSONUtil.toJsonStr(auditLog));
        p.setScores(JSONUtil.toJsonStr(scores));
        p.setWarnings(JSONUtil.toJsonStr(warnings));
        p.setStatus(ToolAiCreatorPhase.BODY_DONE);
        projectService.updateById(p);
        log.info("AI 正文完成 projectId={} type={} bodyLen={} auditItems={} warnings={} costMs={}",
                id, p.getContentType(), plainLength(finalBody), auditLog.size(), warnings.size(),
                System.currentTimeMillis() - startMillis);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiCreatorVO revise(Long id, AiReviseDTO dto) {
        ToolAiCreator p = requirePhase(id, ToolAiCreatorPhase.BODY_DONE, ToolAiCreatorPhase.IMAGES_DONE);
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
        if (ToolAiCreatorPhase.IMAGES_DONE.equals(p.getStatus())) {
            p.setStatus(ToolAiCreatorPhase.BODY_DONE); // 正文已变，配图需重做
            p.setImages(null);
        }
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiCreatorVO imagesStream(Long id, AiImageProgressListener listener) {
        ToolAiCreator p = requirePhase(id, ToolAiCreatorPhase.BODY_DONE, ToolAiCreatorPhase.IMAGES_DONE);
        List<String> placeholders = extractPlaceholders(p);
        if (placeholders.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS, "正文没有配图位，无需生成配图");
        }
        String apiKey = aiClientHolder.requireConfig("api-key", "AI 凭据未配置，请联系管理员");
        String imageModel = StrUtil.blankToDefault(aiClientHolder.getConfig("image-model"), "qwen-image-plus");
        String apiBase = StrUtil.blankToDefault(aiClientHolder.getConfig("image-api-base"), "https://dashscope.aliyuncs.com");
        List<String> warnings = new ArrayList<>();
        if (StrUtil.isNotBlank(p.getWarnings())) {
            try {
                warnings.addAll(JSONUtil.toList(JSONUtil.parseArray(p.getWarnings()), String.class));
            } catch (Exception ignored) {
                // 旧 warnings 格式异常时丢弃，避免阻塞配图
            }
        }
        // 初始化配图结果（保留 prompt 供降级清单）
        List<AiImageVO> images = new ArrayList<>();
        for (String ph : placeholders) {
            AiImageVO img = new AiImageVO();
            img.setPlaceholder(ph);
            AiOutlineVO.AiImageSpec spec = specOf(p, ph);
            img.setSize(spec == null ? defaultSize(p.getContentType(), ph)
                    : StrUtil.blankToDefault(spec.getSize(), defaultSize(p.getContentType(), ph)));
            if (spec != null) {
                img.setPrompt(spec.getPrompt());
                img.setPromptZh(spec.getImagePromptZh());
            }
            img.setStatus("pending");
            images.add(img);
        }
        p.setImages(JSONUtil.toJsonStr(images));
        projectService.updateById(p);
        notifyImageStage(listener, "images", "开始生成 " + placeholders.size() + " 张配图…");
        int consecutiveFailures = 0;
        int success = 0;
        for (AiImageVO img : images) {
            if (consecutiveFailures >= 2) {
                img.setStatus("skipped");
                continue;
            }
            String prompt = StrUtil.blankToDefault(img.getPrompt(),
                    "Warm lifestyle photograph, elderly care concept related to: " + safeAscii(img.getPromptZh())
                            + ", single subject, shallow depth of field");
            img.setStatus("generating");
            fireImage(listener, img.getPlaceholder(), "generating", null, null);
            try {
                String taskId = aiClientHolder.imageClient().submit(apiKey, apiBase, imageModel, prompt, img.getSize());
                String url = aiClientHolder.imageClient().pollImageUrl(apiKey, apiBase, taskId, IMAGE_POLL_TIMEOUT_MS);
                byte[] bytes = aiClientHolder.imageClient().download(url);
                String fileKey = storageService.upload("ai-creation", p.getChannelCode(),
                        new ByteArrayInputStream(bytes), bytes.length, "image/png",
                        img.getPlaceholder().replaceAll("[^A-Za-z0-9_]", "").toLowerCase() + ".png");
                img.setFileKey(fileKey);
                img.setUrl("/agent-api/v1/files/preview/" + fileKey);
                img.setStatus("done");
                success++;
                consecutiveFailures = 0;
                fireImage(listener, img.getPlaceholder(), "done", img.getUrl(), null);
            } catch (Exception e) {
                consecutiveFailures++;
                img.setStatus("failed");
                img.setError(e.getMessage());
                fireImage(listener, img.getPlaceholder(), "failed", null, e.getMessage());
                log.warn("AI 配图单张失败 projectId={} placeholder={}: {}", id, img.getPlaceholder(), e.getMessage());
                if (consecutiveFailures >= 2) {
                    warnings.add("配图连续失败，已降级为 prompt 清单（见配图卡片的中文描述），请自行出图");
                }
            }
            p.setImages(JSONUtil.toJsonStr(images));
            projectService.updateById(p); // 逐张落库，中断可续看结果
        }
        if (success > 0) {
            p.setStatus(ToolAiCreatorPhase.IMAGES_DONE);
        } else {
            warnings.add("配图全部失败，可重试或使用 prompt 清单自行出图");
        }
        p.setWarnings(warnings.isEmpty() ? null : JSONUtil.toJsonStr(warnings));
        projectService.updateById(p);
        notifyImageStage(listener, "done", "配图完成（成功 " + success + "/" + placeholders.size() + "）");
        return projectService.toVO(p);
    }

    @Override
    public String previewHtml(Long id) {
        ToolAiCreator p = projectService.requireOwned(id);
        if (StrUtil.isBlank(p.getBody())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "正文尚未生成");
        }
        String body = finalizeBody(p);
        String title = StrUtil.blankToDefault(p.getSelectedTitle(), "AI 生成图文");
        boolean htmlBody = Integer.valueOf(1).equals(p.getContentType());
        String content;
        if (htmlBody) {
            content = body;
        } else {
            content = "<p>" + htmlEscape(body).replace("\n", "<br/>") + "</p>";
        }
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>%s</title>
                <style>
                  body { background:#f5f5f5; font-family:-apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif; color:#333; line-height:1.8; margin:0; }
                  .aip { max-width:677px; margin:20px auto; background:#fff; padding:30px 24px 60px; border-radius:8px; box-shadow:0 1px 3px rgba(0,0,0,.08); }
                  .aip h1 { font-size:22px; line-height:1.5; margin:0 8px 16px; text-align:center; }
                  .aip img { width:100%%; border-radius:6px; margin:20px 0 8px; box-shadow:0 1px 4px rgba(0,0,0,.1); }
                  .aip p { font-size:16px; margin:0 0 18px; text-align:justify; }
                  .aip h2 { font-size:18px; margin:32px 0 12px; }
                  .aip strong { color:#1a1a1a; }
                </style>
                </head>
                <body>
                <div class="aip">
                <h1>%s</h1>
                %s
                </div>
                </body>
                </html>
                """.formatted(htmlEscape(title), htmlEscape(title), content);
    }

    /** 配图整体降级：生成过但全部 failed/skipped（无 pending/generating/done），允许剔除占位符后保存 */
    boolean imagesAllSettledWithoutSuccess(ToolAiCreator p) {
        if (StrUtil.isBlank(p.getImages())) {
            return false;
        }
        return JSONUtil.toList(JSONUtil.parseArray(p.getImages()), AiImageVO.class).stream()
                .allMatch(i -> "failed".equals(i.getStatus()) || "skipped".equals(i.getStatus()));
    }

    @Override
    public Long saveToContent(Long id) {
        ToolAiCreator p = requirePhase(id, ToolAiCreatorPhase.IMAGES_DONE, ToolAiCreatorPhase.BODY_DONE);
        if (StrUtil.isBlank(p.getBody())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "正文尚未生成");
        }
        if (countPlaceholders(p.getBody()) > 0 && !ToolAiCreatorPhase.IMAGES_DONE.equals(p.getStatus())
                && !imagesAllSettledWithoutSuccess(p)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "正文含配图位，请先完成配图（或处理失败降级）再保存");
        }
        ToolAiCreatorRefsVO refs = refs(p);
        ToolAiCreatorContentCmd cmd = new ToolAiCreatorContentCmd();
        cmd.setTitle(p.getSelectedTitle());
        cmd.setContentType(p.getContentType());
        cmd.setContentBody(finalizeBody(p));
        cmd.setStyleCode(p.getStyleCode());
        cmd.setAudience(p.getAudience());
        cmd.setPurpose(p.getPurpose());
        cmd.setRefContentCode(refs.getRefContentCode());
        if (refs.getKbFiles() != null && !refs.getKbFiles().isEmpty()) {
            cmd.setRefKbFiles(JSONUtil.toJsonStr(refs.getKbFiles()));
        }
        if (refs.getGoods() != null && !refs.getGoods().isEmpty()) {
            cmd.setRefGoodsCodes(JSONUtil.toJsonStr(refs.getGoods().stream()
                    .map(ToolAiCreatorRefsVO.CodeNameRef::getCode).toList()));
        }
        // 封面取已生成的 cover 图
        if (StrUtil.isNotBlank(p.getImages())) {
            JSONUtil.toList(JSONUtil.parseArray(p.getImages()), AiImageVO.class).stream()
                    .filter(i -> "[AI_IMAGE_COVER]".equals(i.getPlaceholder()) && "done".equals(i.getStatus())
                            && StrUtil.isNotBlank(i.getFileKey()))
                    .findFirst().ifPresent(i -> cmd.setCoverImage(i.getFileKey()));
        }
        Long contentId = contentSaver.save(cmd);
        p.setStatus(ToolAiCreatorPhase.SAVED);
        projectService.updateById(p);
        return contentId;
    }

    // ---------- 配图阶段工具 ----------

    /** 正文占位符（有序去重）；contentType=3 无正文占位符 → 取 outline.coverImage（有 prompt 才算） */
    List<String> extractPlaceholders(ToolAiCreator p) {
        List<String> list = new ArrayList<>();
        if (StrUtil.isNotBlank(p.getBody())) {
            Matcher m = PLACEHOLDER_PATTERN.matcher(p.getBody());
            while (m.find()) {
                if (!list.contains(m.group())) list.add(m.group());
            }
        }
        if (list.isEmpty() && Integer.valueOf(3).equals(p.getContentType())) {
            AiOutlineVO outline = StrUtil.isBlank(p.getOutline()) ? null : JSONUtil.toBean(p.getOutline(), AiOutlineVO.class);
            if (outline != null && outline.getCoverImage() != null
                    && StrUtil.isNotBlank(outline.getCoverImage().getPrompt())) {
                list.add("[AI_IMAGE_COVER]");
            }
        }
        return list;
    }

    int countPlaceholders(String body) {
        if (body == null) return 0;
        int c = 0;
        Matcher m = PLACEHOLDER_PATTERN.matcher(body);
        while (m.find()) c++;
        return c;
    }

    /** 占位符 → 大纲配图规格（COVER→coverImage；N→第 N 个带 imageInsertion 的节点） */
    AiOutlineVO.AiImageSpec specOf(ToolAiCreator p, String placeholder) {
        if (StrUtil.isBlank(p.getOutline())) return null;
        AiOutlineVO outline = JSONUtil.toBean(p.getOutline(), AiOutlineVO.class);
        if ("[AI_IMAGE_COVER]".equals(placeholder)) {
            return outline.getCoverImage();
        }
        List<AiOutlineVO.AiOutlineNode> withImage = outline.getNodes() == null ? List.of()
                : outline.getNodes().stream().filter(n -> n.getImageInsertion() != null).toList();
        Matcher m = Pattern.compile("\\[AI_IMAGE_(\\d+)]").matcher(placeholder);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            return n >= 1 && n <= withImage.size() ? withImage.get(n - 1).getImageInsertion() : null;
        }
        return null;
    }

    String defaultSize(Integer contentType, String placeholder) {
        if ("[AI_IMAGE_COVER]".equals(placeholder)) {
            return Integer.valueOf(4).equals(contentType) ? "1080*1440" : "1024*1024";
        }
        return "1280*720";
    }

    /** 正文占位符 → <img>（done 的图）/ 剔除（失败/未生成）；朋友圈等无图形态原样返回 */
    String finalizeBody(ToolAiCreator p) {
        String body = StrUtil.nullToEmpty(p.getBody());
        if (countPlaceholders(body) == 0 || StrUtil.isBlank(p.getImages())) {
            return body.replaceAll(Pattern.quote("[AI_IMAGE_COVER]") + "|" + PLACEHOLDER_PATTERN.pattern(), "");
        }
        Map<String, AiImageVO> map = new LinkedHashMap<>();
        for (AiImageVO img : JSONUtil.toList(JSONUtil.parseArray(p.getImages()), AiImageVO.class)) {
            map.put(img.getPlaceholder(), img);
        }
        Matcher m = PLACEHOLDER_PATTERN.matcher(body);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String ph = m.group();
            AiImageVO img = map.get(ph);
            String replacement = "";
            if (img != null && "done".equals(img.getStatus()) && StrUtil.isNotBlank(img.getUrl())) {
                replacement = "[AI_IMAGE_COVER]".equals(ph)
                        ? "<img class=\"aip-cover\" src=\"" + img.getUrl() + "\" alt=\"封面\">"
                        : "<img class=\"aip-img\" src=\"" + img.getUrl() + "\" alt=\"插图\">";
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    String htmlEscape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** 中文场景描述转 ASCII（降级 prompt 用，非 ASCII 全部折叠为空格） */
    String safeAscii(String zh) {
        return zh == null ? "senior care warm scene" : zh.replaceAll("[^\\x20-\\x7E]", " ").trim();
    }

    private void notifyImageStage(AiImageProgressListener l, String stage, String message) {
        if (l != null) l.onStage(stage, message);
    }

    private void fireImage(AiImageProgressListener l, String ph, String state, String url, String error) {
        if (l != null) l.onImage(ph, state, url, error);
    }

    // ---------- 正文阶段私有工具 ----------

    String outlineText(ToolAiCreator p) {
        if (Integer.valueOf(2).equals(p.getContentType())) {
            return "（朋友圈文案无大纲，按策略直出）";
        }
        return StrUtil.blankToDefault(p.getOutline(), "（无大纲）");
    }

    /** 正文清洗：去围栏/前置标记行/Markdown 加粗残留/首尾空白 */
    String cleanBody(String raw) {
        if (raw == null) return "";
        String text = raw.replaceAll("```[a-zA-Z]*", "").replaceAll("```", "");
        text = text.replaceAll("(?m)^【(标题|摘要)】.*$", "");
        // 模型偶发混用标记语法：真 HTML 片段（图文）把 **x** 转 <strong>；纯文本形态（朋友圈/小红书/脚本）剥星号与混入的 strong 标签
        if (text.contains("<p") || text.contains("<h2") || text.contains("<div")) {
            text = text.replaceAll("\\*\\*([^*\\n]{1,200}?)\\*\\*", "<strong>$1</strong>");
        } else {
            text = text.replaceAll("\\*\\*([^*\\n]{1,200}?)\\*\\*", "$1")
                    .replaceAll("</?strong>", "");
        }
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
    List<String> ruleCheck(String body, ToolAiCreator p) {
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
            ToolAiCreatorRefsVO refs = refs(p);
            if (refs.getGoods() != null && !refs.getGoods().isEmpty()) {
                boolean mentioned = refs.getGoods().stream()
                        .map(ToolAiCreatorRefsVO.CodeNameRef::getName)
                        .anyMatch(nm -> StrUtil.isNotBlank(nm) && body.contains(nm));
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
    private AiOutlineVO callOutline(ToolAiCreator p, AiStrategyVO strategy,
                                    String material, String extraDirective) {
        String prompt = outlinePrompt(p, strategy, material);
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
    private boolean needsImageRetry(ToolAiCreator p, AiOutlineVO outline) {
        if (Integer.valueOf(2).equals(p.getContentType()) || Integer.valueOf(3).equals(p.getContentType())) {
            return false;
        }
        return outline.getNodes().stream().noneMatch(n -> n.getImageInsertion() != null);
    }

    /** 大纲 prompt 渲染（生成/重生成共用，重生成在尾部追加反馈指令） */
    private String outlinePrompt(ToolAiCreator p, AiStrategyVO strategy,
                                 String material) {
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
        vars.put("material", material);
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
    void clearBodyDownstream(ToolAiCreator p) {
        if (!ToolAiCreatorPhase.STRATEGY_CONFIRMED.equals(p.getStatus())) {
            p.setBody(null);
            p.setAuditLog(null);
            p.setScores(null);
            p.setImages(null);
            p.setStatus(ToolAiCreatorPhase.STRATEGY_CONFIRMED);
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
        return aiClientHolder.chatClient().chat(
                aiClientHolder.requireConfig("api-key", "AI 凭据未配置，请联系管理员"),
                aiClientHolder.requireConfig("api-host", "AI 网关未配置，请联系管理员"),
                aiClientHolder.chatModel(), AiPrompts.load("system"), prompt, temperature);
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

    ToolAiCreatorRefsVO refs(ToolAiCreator p) {
        return StrUtil.isBlank(p.getMaterialRefs()) ? new ToolAiCreatorRefsVO()
                : JSONUtil.toBean(p.getMaterialRefs(), ToolAiCreatorRefsVO.class);
    }

    AiStrategyVO parseStrategy(ToolAiCreator p) {
        if (StrUtil.isBlank(p.getStrategy())) {
            throw new BusinessException(ErrorCode.BUSINESS, "请先生成策略");
        }
        return JSONUtil.toBean(p.getStrategy(), AiStrategyVO.class);
    }

    String digestText(ToolAiCreator p) {
        return StrUtil.isBlank(p.getFactDigest()) ? "（无）" : p.getFactDigest();
    }

    String strategyText(AiStrategyVO s) {
        return "受众画像：" + s.getTargetAudience() + "\n核心痛点：" + s.getCorePainPoint()
                + "\n爆款逻辑：" + s.getViralLogic() + "\n优势放大器：" + s.getAdvantageHook();
    }

    /** 从 STRATEGY_CONFIRMED 重入：清空下游产物（改策略=全文重做） */
    void resetDownstreamIfNeeded(ToolAiCreator p) {
        if (ToolAiCreatorPhase.STRATEGY_CONFIRMED.equals(p.getStatus())
                || ToolAiCreatorPhase.OUTLINE_CONFIRMED.equals(p.getStatus())
                || ToolAiCreatorPhase.BODY_DONE.equals(p.getStatus())
                || ToolAiCreatorPhase.IMAGES_DONE.equals(p.getStatus())) {
            p.setOutline(null);
            p.setBody(null);
            p.setAuditLog(null);
            p.setScores(null);
            p.setImages(null);
            p.setSelectedTitle(null);
            p.setStatus(ToolAiCreatorPhase.DIGESTED);
        }
    }

    /** factDigest 为空先消化（strategy 自动前置） */
    void ensureDigest(ToolAiCreator p) {
        if (StrUtil.isBlank(p.getFactDigest())) {
            List<String> warnings = new ArrayList<>();
            AiFactDigestVO digest = LlmJson.parse(chat(render("digest", Map.of(
                    "purpose_rule", purposeRule(p.getPurpose()),
                    "material", materialsText(p, warnings))), DIGEST_TEMPERATURE),
                    AiFactDigestVO.class);
            p.setFactDigest(JSONUtil.toJsonStr(digest));
            p.setWarnings(JSONUtil.toJsonStr(warnings));
            if (ToolAiCreatorPhase.CREATED.equals(p.getStatus())) {
                p.setStatus(ToolAiCreatorPhase.DIGESTED);
            }
        }
    }

    /** 状态守卫：加载并校验（仅允许预期状态进入） */
    ToolAiCreator requirePhase(Long id, String... expected) {
        ToolAiCreator p = projectService.requireOwned(id);
        return checkPhase(p, expected);
    }

    /** 已加载实体的状态守卫（免二次查库） */
    ToolAiCreator checkPhase(ToolAiCreator p, String... expected) {
        for (String s : expected) {
            if (s.equals(p.getStatus())) return p;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "当前阶段不允许该操作（status=" + p.getStatus() + "）");
    }

    /** 标题清洗：去空、限 5 条、分数夹取、tag 规范化 */
    List<AiTitleVO> sanitizeTitles(List<AiTitleVO> titles) {
        if (titles == null || titles.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS, "模型未返回标题，请重试");
        }
        return titles.stream()
                .filter(t -> t != null && StrUtil.isNotBlank(t.getTitle()))
                .peek(t -> {
                    if (t.getViralScore() == null) t.setViralScore(80);
                    t.setViralScore(Math.max(70, Math.min(99, t.getViralScore())));
                    t.setTag(normalizeTag(t.getTag()));
                })
                .limit(5)
                .toList();
    }

    /** tag 规范化：模型常把候选值列表整串抄回（如 kb_number|doc_logic|emotion_hook） */
    String normalizeTag(String tag) {
        if (StrUtil.isBlank(tag)) {
            return "doc_logic";
        }
        if (tag.contains("kb_number")) {
            return "kb_number";
        }
        if (tag.contains("emotion_hook")) {
            return "emotion_hook";
        }
        return "doc_logic";
    }

    /** 前端供材快照渲染为素材文本（8000 字上限，超限截断+warning） */
    String materialsText(ToolAiCreator p, List<String> warnings) {
        if (StrUtil.isBlank(p.getMaterials())) {
            return "（无素材）";
        }
        StringBuilder sb = new StringBuilder();
        for (AiMaterialBlockDTO b : JSONUtil.toList(p.getMaterials(), AiMaterialBlockDTO.class)) {
            sb.append("【").append(b.getTitle()).append("】\n").append(b.getText()).append("\n\n");
            if (sb.length() >= MATERIAL_TOTAL_MAX) {
                warnings.add("素材总量超限，已截断");
                break;
            }
        }
        return sb.length() > MATERIAL_TOTAL_MAX ? sb.substring(0, MATERIAL_TOTAL_MAX) : sb.toString().trim();
    }
}
