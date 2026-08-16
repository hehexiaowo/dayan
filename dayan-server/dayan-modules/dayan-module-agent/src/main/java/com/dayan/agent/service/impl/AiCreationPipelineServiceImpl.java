package com.dayan.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dayan.agent.dto.AiStrategyConfirmDTO;
import com.dayan.agent.dto.AiTitleRegenDTO;
import com.dayan.agent.entity.AiCreationProject;
import com.dayan.agent.model.AiProjectPhase;
import com.dayan.agent.model.AiPurpose;
import com.dayan.agent.service.AiCreationProjectService;
import com.dayan.agent.service.AiCreationPipelineService;
import com.dayan.agent.service.AiMaterialAssembler;
import com.dayan.agent.util.AiPrompts;
import com.dayan.agent.util.LlmJson;
import com.dayan.agent.vo.*;
import com.dayan.common.aliyun.bailian.BailianChatClient;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
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

    /** 状态守卫：仅允许预期状态进入 */
    AiCreationProject requirePhase(Long id, String... expected) {
        AiCreationProject p = projectService.requireOwned(id);
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
