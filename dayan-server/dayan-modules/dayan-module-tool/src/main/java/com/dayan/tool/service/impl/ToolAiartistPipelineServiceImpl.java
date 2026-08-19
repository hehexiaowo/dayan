package com.dayan.tool.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dayan.tool.dto.AiMaterialBlockDTO;
import com.dayan.tool.dto.AiOutlineConfirmDTO;
import com.dayan.tool.dto.AiOutlineRegenDTO;
import com.dayan.tool.dto.AiReviseDTO;
import com.dayan.tool.dto.AiStrategyConfirmDTO;
import com.dayan.tool.dto.AiTitleRegenDTO;
import com.dayan.tool.dto.ToolAiartistContentCmd;
import com.dayan.tool.entity.ToolAiartist;
import com.dayan.tool.model.ToolAiartistPhase;
import com.dayan.tool.model.AiPurpose;
import com.dayan.tool.model.ToolAiartistPipelineConfig;
import com.dayan.tool.service.AiClientHolder;
import com.dayan.tool.service.ToolAiartistContentSaver;
import com.dayan.tool.service.ToolAiartistService;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.service.ToolAiartistPipelineService;
import com.dayan.tool.service.AiGenerateProgressListener;
import com.dayan.tool.service.AiImageProgressListener;
import com.dayan.tool.util.AiPrompts;
import com.dayan.tool.util.LlmJson;
import com.dayan.tool.vo.*;
import com.dayan.system.service.SystemKnowledgeRepoService;
import com.dayan.system.vo.SystemKnowledgeChatVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.oss.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 六阶段流水线编排。阶段温度、合规禁语、素材上限、篇幅窗口、配图规格等
 * 全部来自所属创作分类的 tool_info.config_json.pipeline（缺失回落内置默认值）。
 *
 * <p>状态机：CREATED→DIGESTED→STRATEGY_CONFIRMED→OUTLINE_CONFIRMED→BODY_DONE→IMAGES_DONE→SAVED；
 * strategy/titles 重入会清空下游产物（改策略=全文重做）。全部更新单行 updateById，无长事务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolAiartistPipelineServiceImpl implements ToolAiartistPipelineService {

    /** 正文配图占位符：[AI_IMAGE_COVER] / [AI_IMAGE_1..N] */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[AI_IMAGE_[A-Z0-9_]+]");

    private final ToolAiartistService projectService;
    private final StorageService storageService;
    private final AiClientHolder aiClientHolder;
    private final ToolAiartistContentSaver contentSaver;
    private final ToolInfoService toolInfoService;
    private final SystemKnowledgeRepoService knowledgeRepoService;

    @Override
    public ToolAiartistVO digest(Long id) {
        ToolAiartist p = projectService.requireOwned(id);
        List<String> warnings = new ArrayList<>();
        ToolAiartistPipelineConfig cfg = pipelineConfig(p);
        String material = materialsText(p, warnings);
        AiFactDigestVO digest = LlmJson.parse(chat(p, render(stagePrompt("digest", cfg), Map.of(
                "purpose_rule", purposeRule(p.getPurpose(), cfg),
                "material", material)), cfg.getDigestTemp()),
                AiFactDigestVO.class);
        p.setFactDigest(JSONUtil.toJsonStr(digest));
        p.setWarnings(JSONUtil.toJsonStr(warnings));
        if (ToolAiartistPhase.CREATED.equals(p.getStatus())) {
            p.setStatus(ToolAiartistPhase.DIGESTED);
        }
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiartistVO strategy(Long id) {
        ToolAiartist p = requirePhase(id, ToolAiartistPhase.CREATED, ToolAiartistPhase.DIGESTED, ToolAiartistPhase.STRATEGY_CONFIRMED);
        resetDownstreamIfNeeded(p);
        ensureDigest(p);
        List<String> warnings = new ArrayList<>();
        ToolAiartistPipelineConfig cfg = pipelineConfig(p);
        String material = materialsText(p, warnings);
        AiStrategyBundleVO out = LlmJson.parse(chat(p, render(stagePrompt("strategy", cfg), Map.of(
                "purpose_rule", purposeRule(p.getPurpose(), cfg),
                "form", formInstruction(p.getContentType(), cfg),
                "style", styleInstruction(p.getStyleCode(), cfg),
                "audience", audienceInstruction(p.getAudience(), cfg),
                "topic", StrUtil.blankToDefault(p.getTopic(), "（从素材归纳）"),
                "fact_digest", digestText(p),
                "material", material)), cfg.getStrategyTemp()),
                AiStrategyBundleVO.class);
        AiStrategyVO strategy = out.getStrategyPanel() == null ? new AiStrategyVO() : out.getStrategyPanel();
        strategy.setCoreExecutionPrompt(out.getCoreExecutionPrompt());
        p.setStrategy(JSONUtil.toJsonStr(strategy));
        p.setTitles(JSONUtil.toJsonStr(sanitizeTitles(out.getGeneratedTitles(), cfg)));
        p.setWarnings(JSONUtil.toJsonStr(warnings));
        p.setStatus(ToolAiartistPhase.DIGESTED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiartistVO regenerateTitles(Long id, AiTitleRegenDTO dto) {
        ToolAiartist p = requirePhase(id, ToolAiartistPhase.DIGESTED, ToolAiartistPhase.STRATEGY_CONFIRMED);
        resetDownstreamIfNeeded(p);
        ToolAiartistPipelineConfig cfg = pipelineConfig(p);
        AiStrategyVO strategy = parseStrategy(p);
        String prompt = render(stagePrompt("titles-regen", cfg), Map.of(
                "strategy_panel", strategyText(strategy),
                "previous_titles", StrUtil.blankToDefault(p.getTitles(), "（无）"),
                "feedback", StrUtil.blankToDefault(dto == null ? null : dto.getFeedback(), "换一批差异化角度"),
                "title_limit", String.valueOf(cfg.titleLimitOf(p.getContentType()))));
        AiStrategyBundleVO out = LlmJson.parse(chat(p, prompt, cfg.getTitlesTemp()), AiStrategyBundleVO.class);
        p.setTitles(JSONUtil.toJsonStr(sanitizeTitles(out.getGeneratedTitles(), cfg)));
        p.setStatus(ToolAiartistPhase.DIGESTED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiartistVO confirmStrategy(Long id, AiStrategyConfirmDTO dto) {
        ToolAiartist p = requirePhase(id, ToolAiartistPhase.DIGESTED, ToolAiartistPhase.STRATEGY_CONFIRMED);
        resetDownstreamIfNeeded(p);
        AiStrategyVO strategy = parseStrategy(p);
        if (StrUtil.isNotBlank(dto.getTargetAudience())) strategy.setTargetAudience(dto.getTargetAudience());
        if (StrUtil.isNotBlank(dto.getCorePainPoint())) strategy.setCorePainPoint(dto.getCorePainPoint());
        if (StrUtil.isNotBlank(dto.getViralLogic())) strategy.setViralLogic(dto.getViralLogic());
        if (StrUtil.isNotBlank(dto.getAdvantageHook())) strategy.setAdvantageHook(dto.getAdvantageHook());
        p.setStrategy(JSONUtil.toJsonStr(strategy));
        p.setSelectedTitle(dto.getSelectedTitle().trim());
        p.setStatus(ToolAiartistPhase.STRATEGY_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiartistVO outline(Long id) {
        ToolAiartist p = requirePhase(id, ToolAiartistPhase.STRATEGY_CONFIRMED, ToolAiartistPhase.OUTLINE_CONFIRMED);
        if (Integer.valueOf(2).equals(p.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "朋友圈文案无大纲阶段，请直接生成正文");
        }
        clearBodyDownstream(p);
        AiStrategyVO strategy = parseStrategy(p);
        AiOutlineVO outline = callOutline(p, strategy, materialsText(p, new ArrayList<>()), null);
        p.setOutline(JSONUtil.toJsonStr(outline));
        p.setStatus(ToolAiartistPhase.STRATEGY_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiartistVO regenerateOutline(Long id, AiOutlineRegenDTO dto) {
        // 反馈为空 = 直接重跑 outline()
        if (dto == null || StrUtil.isBlank(dto.getFeedback())) {
            return outline(id);
        }
        ToolAiartist p = requirePhase(id, ToolAiartistPhase.STRATEGY_CONFIRMED, ToolAiartistPhase.OUTLINE_CONFIRMED);
        if (Integer.valueOf(2).equals(p.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "朋友圈文案无大纲阶段，请直接生成正文");
        }
        clearBodyDownstream(p);
        AiStrategyVO strategy = parseStrategy(p);
        AiOutlineVO outline = callOutline(p, strategy, materialsText(p, new ArrayList<>()), dto.getFeedback());
        p.setOutline(JSONUtil.toJsonStr(outline));
        p.setStatus(ToolAiartistPhase.STRATEGY_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiartistVO confirmOutline(Long id, AiOutlineConfirmDTO dto) {
        ToolAiartist p = requirePhase(id, ToolAiartistPhase.STRATEGY_CONFIRMED, ToolAiartistPhase.OUTLINE_CONFIRMED);
        AiOutlineVO outline;
        try {
            outline = JSONUtil.toBean(dto.getOutline(), AiOutlineVO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "大纲 JSON 解析失败");
        }
        sanitizeOutline(outline);
        p.setOutline(JSONUtil.toJsonStr(outline));
        p.setStatus(ToolAiartistPhase.OUTLINE_CONFIRMED);
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiartistVO bodyStream(Long id, AiGenerateProgressListener listener) {
        ToolAiartist p = projectService.requireOwned(id);
        // 朋友圈无大纲阶段，策略确认后直接写正文；其余形态需大纲确认后进入（BODY_DONE=重生成）
        if (Integer.valueOf(2).equals(p.getContentType())) {
            checkPhase(p, ToolAiartistPhase.STRATEGY_CONFIRMED, ToolAiartistPhase.BODY_DONE);
        } else {
            checkPhase(p, ToolAiartistPhase.OUTLINE_CONFIRMED, ToolAiartistPhase.BODY_DONE);
        }
        long startMillis = System.currentTimeMillis();
        if (ToolAiartistPhase.BODY_DONE.equals(p.getStatus())) {
            p.setImages(null); // 重生成正文使旧配图失效
        }
        AiStrategyVO strategy = parseStrategy(p);
        ToolAiartistPipelineConfig cfg = pipelineConfig(p);
        List<String> warnings = new ArrayList<>();
        notifyStage(listener, "material", "素材就绪…");
        String material = materialsText(p, warnings);
        // 正文前知识库自动检索补充（校验+补充定位；失败/无命中降级不阻断）
        material = supplementKnowledge(p, cfg, material, strategy, warnings);
        // 1. 正文（SSE 流式）
        notifyStage(listener, "body", "正在撰写正文…");
        Map<String, String> bodyVars = new HashMap<>();
        bodyVars.put("target_audience", StrUtil.nullToEmpty(strategy.getTargetAudience()));
        bodyVars.put("core_pain_point", StrUtil.nullToEmpty(strategy.getCorePainPoint()));
        bodyVars.put("viral_logic", StrUtil.nullToEmpty(strategy.getViralLogic()));
        bodyVars.put("advantage_hook", StrUtil.nullToEmpty(strategy.getAdvantageHook()));
        bodyVars.put("core_execution_prompt", StrUtil.nullToEmpty(strategy.getCoreExecutionPrompt()));
        bodyVars.put("purpose_rule", purposeRule(p.getPurpose(), cfg));
        bodyVars.put("platform_rules", platformRules(p.getContentType(), cfg));
        bodyVars.put("selected_title", StrUtil.nullToEmpty(p.getSelectedTitle()));
        bodyVars.put("outline_json", outlineText(p));
        bodyVars.put("fact_digest", digestText(p));
        bodyVars.put("material", material);
        String bodyPrompt = render(stagePrompt("body", cfg), bodyVars);
        String body = listener == null
                ? chat(p, bodyPrompt, cfg.getBodyTemp())
                : aiClientHolder.chatClient().chatStream(
                        aiClientHolder.requireConfig("llm.api-key", "AI 凭据未配置，请联系管理员"),
                        aiClientHolder.requireConfig("llm.api-host", "AI 网关未配置，请联系管理员"),
                        aiClientHolder.chatModel(), categoryPrefix(cfg) + systemPromptText(cfg), bodyPrompt, cfg.getBodyTemp(), listener::onDelta);
        body = cleanBody(body);
        warnings.addAll(ruleCheck(body, p, cfg));
        // 2. 审计（独立 LLM 关卡）
        notifyStage(listener, "audit", "事实核查与合规审计…");
        String auditOut = chat(p, render(stagePrompt("audit", cfg), Map.of(
                "fact_digest", digestText(p),
                "material", material,
                "body", body)), cfg.getAuditTemp());
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
        String polishOut = chat(p, render(stagePrompt("polish", cfg), Map.of(
                "core_pain_point", StrUtil.nullToEmpty(strategy.getCorePainPoint()),
                "advantage_hook", StrUtil.nullToEmpty(strategy.getAdvantageHook()),
                "platform_rules", platformRules(p.getContentType(), cfg),
                "body", auditedBody)), cfg.getPolishTemp());
        String polished = extractTag(polishOut, "revised_article");
        String finalBody = auditedBody;
        if (StrUtil.isNotBlank(polished)) {
            polished = cleanBody(polished);
            if (plainLength(polished) >= plainLength(auditedBody) * cfg.getPolishKeepRatio()) {
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
        p.setStatus(ToolAiartistPhase.BODY_DONE);
        projectService.updateById(p);
        log.info("AI 正文完成 projectId={} type={} bodyLen={} auditItems={} warnings={} costMs={}",
                id, p.getContentType(), plainLength(finalBody), auditLog.size(), warnings.size(),
                System.currentTimeMillis() - startMillis);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiartistVO revise(Long id, AiReviseDTO dto) {
        ToolAiartist p = requirePhase(id, ToolAiartistPhase.BODY_DONE, ToolAiartistPhase.IMAGES_DONE);
        ToolAiartistPipelineConfig cfg = pipelineConfig(p);
        AiStrategyVO strategy = parseStrategy(p);
        StringBuilder prompt = new StringBuilder(render(stagePrompt("revise", cfg), Map.of(
                "core_pain_point", StrUtil.nullToEmpty(strategy.getCorePainPoint()),
                "body", StrUtil.nullToEmpty(p.getBody()),
                "feedback", dto.getFeedback().trim())));
        if (StrUtil.isNotBlank(dto.getAnchor())) {
            prompt.append("\n【定位】重点修正包含「").append(dto.getAnchor().trim()).append("」的段落。");
        }
        String revised = cleanBody(chat(p, prompt.toString(), cfg.getReviseTemp()));
        if (StrUtil.isBlank(revised)) {
            throw new BusinessException(ErrorCode.BUSINESS, "修订失败，请重试");
        }
        p.setBody(revised);
        List<AiAuditItemVO> auditLog = p.getAuditLog() == null ? new ArrayList<>()
                : new ArrayList<>(JSONUtil.toList(JSONUtil.parseArray(p.getAuditLog()), AiAuditItemVO.class));
        auditLog.add(auditItem("人工勘误", dto.getFeedback().trim()));
        p.setAuditLog(JSONUtil.toJsonStr(auditLog));
        if (ToolAiartistPhase.IMAGES_DONE.equals(p.getStatus())) {
            p.setStatus(ToolAiartistPhase.BODY_DONE); // 正文已变，配图需重做
            p.setImages(null);
        }
        projectService.updateById(p);
        return projectService.toVO(p);
    }

    @Override
    public ToolAiartistVO imagesStream(Long id, AiImageProgressListener listener) {
        ToolAiartist p = requirePhase(id, ToolAiartistPhase.BODY_DONE, ToolAiartistPhase.IMAGES_DONE);
        ToolAiartistPipelineConfig cfg = pipelineConfig(p);
        List<String> placeholders = extractPlaceholders(p);
        if (placeholders.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS, "正文没有配图位，无需生成配图");
        }
        String apiKey = aiClientHolder.requireConfig("llm.api-key", "AI 凭据未配置，请联系管理员");
        String imageModel = StrUtil.blankToDefault(aiClientHolder.getConfig("llm.image-model"), "qwen-image-plus");
        String apiBase = StrUtil.blankToDefault(aiClientHolder.getConfig("llm.image-api-base"), "https://dashscope.aliyuncs.com");
        List<String> warnings = new ArrayList<>();
        if (StrUtil.isNotBlank(p.getWarnings())) {
            try {
                warnings.addAll(JSONUtil.toList(JSONUtil.parseArray(p.getWarnings()), String.class));
            } catch (Exception ignored) {
                // 旧 warnings 格式异常时丢弃，避免阻塞配图
            }
        }
        // 初始化配图结果：素材自带图（机构图集/范文封面）直接引用为 done，其余位保留 prompt 待 AI 生成
        List<AiImageVO> images = new ArrayList<>();
        List<ToolAiartistRefsVO.MaterialImageRef> materialImages = refs(p).getMaterialImages();
        int materialUsed = 0;
        for (String ph : placeholders) {
            AiImageVO img = new AiImageVO();
            img.setPlaceholder(ph);
            AiOutlineVO.AiImageSpec spec = specOf(p, ph);
            img.setSize(spec == null ? defaultSize(cfg, p.getContentType(), ph)
                    : StrUtil.blankToDefault(spec.getSize(), defaultSize(cfg, p.getContentType(), ph)));
            if (spec != null) {
                img.setPrompt(spec.getPrompt());
                img.setPromptZh(spec.getImagePromptZh());
            }
            ToolAiartistRefsVO.MaterialImageRef src = materialImageFor(ph, materialImages);
            if (src != null) {
                img.setUrl(src.getUrl());
                img.setPrompt(StrUtil.blankToDefault(img.getPrompt(), src.getName()));
                img.setPromptZh(StrUtil.blankToDefault(img.getPromptZh(), src.getName()));
                img.setStatus("done");
                materialUsed++;
            } else {
                img.setStatus("pending");
            }
            images.add(img);
        }
        p.setImages(JSONUtil.toJsonStr(images));
        projectService.updateById(p);
        notifyImageStage(listener, "images",
                materialUsed > 0
                        ? "已引用 " + materialUsed + " 张素材图，其余 " + (placeholders.size() - materialUsed) + " 张由 AI 生成…"
                        : "开始生成 " + placeholders.size() + " 张配图…");
        int consecutiveFailures = 0;
        int success = 0;
        for (AiImageVO img : images) {
            if ("done".equals(img.getStatus())) {
                success++; // 素材图直用，跳过 AI 生成
                continue;
            }
            if (consecutiveFailures >= cfg.getImageRetryAfterFailures()) {
                img.setStatus("skipped");
                continue;
            }
            String prompt = StrUtil.blankToDefault(img.getPrompt(),
                    cfg.getImageFallbackPrompt().replace("{promptZh}", safeAscii(img.getPromptZh())));
            img.setStatus("generating");
            fireImage(listener, img.getPlaceholder(), "generating", null, null);
            try {
                String taskId = aiClientHolder.imageClient().submit(apiKey, apiBase, imageModel, prompt, img.getSize());
                String url = aiClientHolder.imageClient().pollImageUrl(apiKey, apiBase, taskId, cfg.getImagePollTimeoutMs());
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
                if (consecutiveFailures >= cfg.getImageRetryAfterFailures()) {
                    warnings.add("配图连续失败，已降级为 prompt 清单（见配图卡片的中文描述），请自行出图");
                }
            }
            p.setImages(JSONUtil.toJsonStr(images));
            projectService.updateById(p); // 逐张落库，中断可续看结果
        }
        if (success > 0) {
            p.setStatus(ToolAiartistPhase.IMAGES_DONE);
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
        ToolAiartist p = projectService.requireOwned(id);
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
    boolean imagesAllSettledWithoutSuccess(ToolAiartist p) {
        if (StrUtil.isBlank(p.getImages())) {
            return false;
        }
        return JSONUtil.toList(JSONUtil.parseArray(p.getImages()), AiImageVO.class).stream()
                .allMatch(i -> "failed".equals(i.getStatus()) || "skipped".equals(i.getStatus()));
    }

    @Override
    public Long saveToContent(Long id) {
        ToolAiartist p = requirePhase(id, ToolAiartistPhase.IMAGES_DONE, ToolAiartistPhase.BODY_DONE);
        if (StrUtil.isBlank(p.getBody())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "正文尚未生成");
        }
        if (countPlaceholders(p.getBody()) > 0 && !ToolAiartistPhase.IMAGES_DONE.equals(p.getStatus())
                && !imagesAllSettledWithoutSuccess(p)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "正文含配图位，请先完成配图（或处理失败降级）再保存");
        }
        ToolAiartistRefsVO refs = refs(p);
        ToolAiartistContentCmd cmd = new ToolAiartistContentCmd();
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
                    .map(ToolAiartistRefsVO.CodeNameRef::getCode).toList()));
        }
        // 封面取已生成的 cover 图
        if (StrUtil.isNotBlank(p.getImages())) {
            JSONUtil.toList(JSONUtil.parseArray(p.getImages()), AiImageVO.class).stream()
                    .filter(i -> "[AI_IMAGE_COVER]".equals(i.getPlaceholder()) && "done".equals(i.getStatus())
                            && StrUtil.isNotBlank(i.getFileKey()))
                    .findFirst().ifPresent(i -> cmd.setCoverImage(i.getFileKey()));
        }
        Long contentId = contentSaver.save(cmd);
        p.setStatus(ToolAiartistPhase.SAVED);
        projectService.updateById(p);
        return contentId;
    }

    // ---------- 配图阶段工具 ----------

    /** 正文占位符（有序去重）；contentType=3 无正文占位符 → 取 outline.coverImage（有 prompt 才算） */
    List<String> extractPlaceholders(ToolAiartist p) {
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
    AiOutlineVO.AiImageSpec specOf(ToolAiartist p, String placeholder) {
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

    /** 占位符 → 素材自带图：封面位取 cover 候选，正文插图位按序取 body 候选（不足时回退 cover 图），无则 null（走 AI 生成） */
    ToolAiartistRefsVO.MaterialImageRef materialImageFor(String placeholder,
                                                         List<ToolAiartistRefsVO.MaterialImageRef> materialImages) {
        if (materialImages == null || materialImages.isEmpty()) {
            return null;
        }
        if ("[AI_IMAGE_COVER]".equals(placeholder)) {
            return materialImages.stream()
                    .filter(i -> "cover".equals(i.getRole()) && StrUtil.isNotBlank(i.getUrl()))
                    .findFirst().orElse(null);
        }
        Matcher m = Pattern.compile("\\[AI_IMAGE_(\\d+)]").matcher(placeholder);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            List<ToolAiartistRefsVO.MaterialImageRef> body = materialImages.stream()
                    .filter(i -> "body".equals(i.getRole()) && StrUtil.isNotBlank(i.getUrl())).toList();
            if (n >= 1 && n <= body.size()) {
                return body.get(n - 1);
            }
            // body 素材图不足时回退 cover 图（有素材图优先于 AI 生成）
            List<ToolAiartistRefsVO.MaterialImageRef> cover = materialImages.stream()
                    .filter(i -> "cover".equals(i.getRole()) && StrUtil.isNotBlank(i.getUrl())).toList();
            int k = n - body.size();
            return k >= 1 && k <= cover.size() ? cover.get(k - 1) : null;
        }
        return null;
    }

    String defaultSize(ToolAiartistPipelineConfig cfg, Integer contentType, String placeholder) {
        if ("[AI_IMAGE_COVER]".equals(placeholder)) {
            return Integer.valueOf(4).equals(contentType) ? cfg.getCoverSizeXhs() : cfg.getCoverSizeDefault();
        }
        return cfg.getNodeSize();
    }

    /** 正文占位符 → <img>（done 的图）/ 剔除（失败/未生成）；朋友圈等无图形态原样返回 */
    String finalizeBody(ToolAiartist p) {
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

    String outlineText(ToolAiartist p) {
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
    List<String> ruleCheck(String body, ToolAiartist p, ToolAiartistPipelineConfig cfg) {
        List<String> warnings = new ArrayList<>();
        int len = plainLength(body);
        int type = p.getContentType() == null ? 1 : p.getContentType();
        int[] win = cfg.lengthWindowOf(type);
        int min = win[0];
        int max = win[1];
        if (len < min) warnings.add("正文偏短（约 " + len + " 字），建议重新生成");
        if (len > max) warnings.add("正文超长（约 " + len + " 字），建议重新生成或手动精简");
        String plain = body.replaceAll("<[^>]+>", "");
        for (String banned : cfg.getBannedPhrases()) {
            if (plain.contains(banned)) {
                warnings.add("正文疑似含不合规用语「" + banned + "」，请人工复核");
                break;
            }
        }
        if (AiPurpose.PRODUCT.equals(p.getPurpose()) && p.getMaterialRefs() != null) {
            ToolAiartistRefsVO refs = refs(p);
            if (refs.getGoods() != null && !refs.getGoods().isEmpty()) {
                boolean mentioned = refs.getGoods().stream()
                        .map(ToolAiartistRefsVO.CodeNameRef::getName)
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
    private AiOutlineVO callOutline(ToolAiartist p, AiStrategyVO strategy,
                                    String material, String extraDirective) {
        ToolAiartistPipelineConfig cfg = pipelineConfig(p);
        String prompt = outlinePrompt(p, strategy, material, cfg);
        if (StrUtil.isNotBlank(extraDirective)) {
            prompt = prompt + "\n\n【重生成要求（最高优先级）】" + extraDirective.trim();
        }
        AiOutlineVO outline = LlmJson.parse(chat(p, prompt, cfg.getOutlineTemp()), AiOutlineVO.class);
        sanitizeOutline(outline);
        if (StrUtil.isBlank(extraDirective) && needsImageRetry(p, outline)) {
            AiOutlineVO retry = LlmJson.parse(chat(p, prompt + "\n\n【重生成要求（最高优先级）】"
                    + "按配图位规划为 3-4 个主体节点补充 imageInsertion：英文 prompt 以 Warm/Bright/Muted lifestyle photograph "
                    + "开头、≤60 词、单一场景并含摄影术语；size 用 1280*720；无需配图的节点保持 null。", cfg.getOutlineTemp()),
                    AiOutlineVO.class);
            sanitizeOutline(retry);
            return retry;
        }
        return outline;
    }

    /** 图文/小红书一个配图位都没有 → 重试（朋友圈/脚本无正文配图位，不重试） */
    private boolean needsImageRetry(ToolAiartist p, AiOutlineVO outline) {
        if (Integer.valueOf(2).equals(p.getContentType()) || Integer.valueOf(3).equals(p.getContentType())) {
            return false;
        }
        return outline.getNodes().stream().noneMatch(n -> n.getImageInsertion() != null);
    }

    /** 大纲 prompt 渲染（生成/重生成共用，重生成在尾部追加反馈指令） */
    private String outlinePrompt(ToolAiartist p, AiStrategyVO strategy,
                                 String material, ToolAiartistPipelineConfig cfg) {
        Map<String, String> vars = new HashMap<>();
        vars.put("core_execution_prompt", StrUtil.nullToEmpty(strategy.getCoreExecutionPrompt()));
        vars.put("target_audience", StrUtil.nullToEmpty(strategy.getTargetAudience()));
        vars.put("core_pain_point", StrUtil.nullToEmpty(strategy.getCorePainPoint()));
        vars.put("viral_logic", StrUtil.nullToEmpty(strategy.getViralLogic()));
        vars.put("advantage_hook", StrUtil.nullToEmpty(strategy.getAdvantageHook()));
        vars.put("purpose_rule", purposeRule(p.getPurpose(), cfg));
        vars.put("platform_rules", platformRules(p.getContentType(), cfg));
        vars.put("selected_title", StrUtil.nullToEmpty(p.getSelectedTitle()));
        vars.put("topic", StrUtil.blankToDefault(p.getTopic(), "（从素材归纳）"));
        vars.put("fact_digest", digestText(p));
        vars.put("material", material);
        vars.put("image_count_hint", cfg.imageCountHintOf(p.getContentType()));
        return render(stagePrompt("outline", cfg), vars);
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
    void clearBodyDownstream(ToolAiartist p) {
        if (!ToolAiartistPhase.STRATEGY_CONFIRMED.equals(p.getStatus())) {
            p.setBody(null);
            p.setAuditLog(null);
            p.setScores(null);
            p.setImages(null);
            p.setStatus(ToolAiartistPhase.STRATEGY_CONFIRMED);
        }
    }

    // ---------- 公共设施（后续任务复用） ----------

    /** 渲染提示词模板（{{var}} 替换；模板由 stagePrompt 提供，来自资源或分类覆盖配置） */
    String render(String template, Map<String, String> vars) {
        return AiPrompts.render(template, vars);
    }

    /** 阶段提示词：分类覆盖配置（config_json.pipeline.prompts）优先，缺失回落 ai-prompts 资源 */
    String stagePrompt(String name, ToolAiartistPipelineConfig cfg) {
        String override = cfg.getPrompts().get(name);
        return StrUtil.isNotBlank(override) ? override : AiPrompts.load(name);
    }

    /** 分类流水线配置（config_json.pipeline 全量，缺失/异常回落内置默认值） */
    ToolAiartistPipelineConfig pipelineConfig(ToolAiartist p) {
        if (p == null || StrUtil.isBlank(p.getToolCode())) {
            return new ToolAiartistPipelineConfig();
        }
        try {
            return toolInfoService.getAiartistPipelineConfig(p.getToolCode());
        } catch (Exception e) {
            log.warn("读取创作分类流水线配置失败 toolCode={}: {}", p.getToolCode(), e.getMessage());
            return new ToolAiartistPipelineConfig();
        }
    }

    /** 非流式 chat（全局系统提示词 + 分类人设，均配置优先、资源兜底） */
    String chat(ToolAiartist p, String prompt, double temperature) {
        ToolAiartistPipelineConfig cfg = pipelineConfig(p);
        return aiClientHolder.chatClient().chat(
                aiClientHolder.requireConfig("llm.api-key", "AI 凭据未配置，请联系管理员"),
                aiClientHolder.requireConfig("llm.api-host", "AI 网关未配置，请联系管理员"),
                aiClientHolder.chatModel(), categoryPrefix(cfg) + systemPromptText(cfg), prompt, temperature);
    }

    /** 创作分类人设前缀（config_json.systemPrompt），配置缺失时返回空串 */
    String categoryPrefix(ToolAiartistPipelineConfig cfg) {
        String sp = cfg.getSystemPrompt();
        return StrUtil.isBlank(sp) ? "" : "【创作分类设定】" + sp + "\n\n";
    }

    /** 全局系统提示词：pipeline.system 优先，回落 ai-prompts/system.md */
    String systemPromptText(ToolAiartistPipelineConfig cfg) {
        return StrUtil.isNotBlank(cfg.getSystem()) ? cfg.getSystem() : AiPrompts.load("system");
    }

    /** 目的规则：pipeline.purposeRules 优先，回落 ai-prompts/purpose/*.md */
    String purposeRule(String purpose, ToolAiartistPipelineConfig cfg) {
        String p = AiPurpose.PRODUCT.equals(purpose) || AiPurpose.PARK.equals(purpose)
                || AiPurpose.SCIENCE.equals(purpose) ? purpose : AiPurpose.SCIENCE;
        String override = cfg.getPurposeRules().get(p);
        return StrUtil.isNotBlank(override) ? override : AiPrompts.load("purpose/" + p);
    }

    String formInstruction(Integer contentType, ToolAiartistPipelineConfig cfg) {
        String s = cfg.getFormInstructions().get(contentType);
        if (s == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "内容形态取值 1-4");
        return s;
    }

    String styleInstruction(String styleCode, ToolAiartistPipelineConfig cfg) {
        return StrUtil.blankToDefault(cfg.getStyleInstructions().get(styleCode), "自然流畅的行业写作风格");
    }

    String audienceInstruction(String audience, ToolAiartistPipelineConfig cfg) {
        return StrUtil.blankToDefault(cfg.getAudienceInstructions().get(audience),
                cfg.getAudienceInstructions().getOrDefault("general", "40-70 岁客户及其子女：通俗易懂"));
    }

    /** 平台规则：pipeline.platformRules 优先，回落 ai-prompts/platform/*.md */
    String platformRules(Integer contentType, ToolAiartistPipelineConfig cfg) {
        String key = switch (contentType) {
            case 2 -> "moment";
            case 3 -> "script";
            case 4 -> "xhs";
            default -> "mp";
        };
        String override = cfg.getPlatformRules().get(key);
        return StrUtil.isNotBlank(override) ? override : AiPrompts.load("platform/" + key);
    }

    ToolAiartistRefsVO refs(ToolAiartist p) {
        return StrUtil.isBlank(p.getMaterialRefs()) ? new ToolAiartistRefsVO()
                : JSONUtil.toBean(p.getMaterialRefs(), ToolAiartistRefsVO.class);
    }

    AiStrategyVO parseStrategy(ToolAiartist p) {
        if (StrUtil.isBlank(p.getStrategy())) {
            throw new BusinessException(ErrorCode.BUSINESS, "请先生成策略");
        }
        return JSONUtil.toBean(p.getStrategy(), AiStrategyVO.class);
    }

    String digestText(ToolAiartist p) {
        return StrUtil.isBlank(p.getFactDigest()) ? "（无）" : p.getFactDigest();
    }

    String strategyText(AiStrategyVO s) {
        return "受众画像：" + s.getTargetAudience() + "\n核心痛点：" + s.getCorePainPoint()
                + "\n爆款逻辑：" + s.getViralLogic() + "\n优势放大器：" + s.getAdvantageHook();
    }

    /** 从 STRATEGY_CONFIRMED 重入：清空下游产物（改策略=全文重做） */
    void resetDownstreamIfNeeded(ToolAiartist p) {
        if (ToolAiartistPhase.STRATEGY_CONFIRMED.equals(p.getStatus())
                || ToolAiartistPhase.OUTLINE_CONFIRMED.equals(p.getStatus())
                || ToolAiartistPhase.BODY_DONE.equals(p.getStatus())
                || ToolAiartistPhase.IMAGES_DONE.equals(p.getStatus())) {
            p.setOutline(null);
            p.setBody(null);
            p.setAuditLog(null);
            p.setScores(null);
            p.setImages(null);
            p.setSelectedTitle(null);
            p.setStatus(ToolAiartistPhase.DIGESTED);
        }
    }

    /** factDigest 为空先消化（strategy 自动前置） */
    void ensureDigest(ToolAiartist p) {
        if (StrUtil.isBlank(p.getFactDigest())) {
            List<String> warnings = new ArrayList<>();
            ToolAiartistPipelineConfig cfg = pipelineConfig(p);
            AiFactDigestVO digest = LlmJson.parse(chat(p, render(stagePrompt("digest", cfg), Map.of(
                    "purpose_rule", purposeRule(p.getPurpose(), cfg),
                    "material", materialsText(p, warnings))), cfg.getDigestTemp()),
                    AiFactDigestVO.class);
            p.setFactDigest(JSONUtil.toJsonStr(digest));
            p.setWarnings(JSONUtil.toJsonStr(warnings));
            if (ToolAiartistPhase.CREATED.equals(p.getStatus())) {
                p.setStatus(ToolAiartistPhase.DIGESTED);
            }
        }
    }

    /** 状态守卫：加载并校验（仅允许预期状态进入） */
    ToolAiartist requirePhase(Long id, String... expected) {
        ToolAiartist p = projectService.requireOwned(id);
        return checkPhase(p, expected);
    }

    /** 已加载实体的状态守卫（免二次查库） */
    ToolAiartist checkPhase(ToolAiartist p, String... expected) {
        for (String s : expected) {
            if (s.equals(p.getStatus())) return p;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "当前阶段不允许该操作（status=" + p.getStatus() + "）");
    }

    /** 标题清洗：去空、限条数、分数夹取、tag 规范化 */
    List<AiTitleVO> sanitizeTitles(List<AiTitleVO> titles, ToolAiartistPipelineConfig cfg) {
        if (titles == null || titles.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS, "模型未返回标题，请重试");
        }
        return titles.stream()
                .filter(t -> t != null && StrUtil.isNotBlank(t.getTitle()))
                .peek(t -> {
                    if (t.getViralScore() == null) t.setViralScore(cfg.getScoreMin());
                    t.setViralScore(Math.max(cfg.getScoreMin(), Math.min(cfg.getScoreMax(), t.getViralScore())));
                    t.setTag(normalizeTag(t.getTag()));
                })
                .limit(cfg.getTitleCountLimit())
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

    /** 前端供材快照渲染为素材文本（超上限截断+warning） */
    String materialsText(ToolAiartist p, List<String> warnings) {
        int materialMax = pipelineConfig(p).getMaterialMax();
        if (StrUtil.isBlank(p.getMaterials())) {
            return "（无素材）";
        }
        StringBuilder sb = new StringBuilder();
        for (AiMaterialBlockDTO b : JSONUtil.toList(p.getMaterials(), AiMaterialBlockDTO.class)) {
            sb.append("【").append(b.getTitle()).append("】\n").append(b.getText()).append("\n\n");
            if (sb.length() >= materialMax) {
                warnings.add("素材总量超限，已截断");
                break;
            }
        }
        return sb.length() > materialMax ? sb.substring(0, materialMax) : sb.toString().trim();
    }

    /**
     * 正文前知识库自动检索补充：以主题+策略为 query 检索分类绑定的知识库（config_json.repoIds），
     * 结果作为「知识补充」段追加到素材文本。正文/审计/润色共用同一份 material，
     * 检索结果同时成为事实供给与审计比对基准。检索失败或未命中降级为 warning，不阻断。
     */
    String supplementKnowledge(ToolAiartist p, ToolAiartistPipelineConfig cfg, String material,
                               AiStrategyVO strategy, List<String> warnings) {
        if (cfg.getRepoIds() == null || cfg.getRepoIds().isEmpty()) {
            return material;
        }
        String query = buildKnowledgeQuery(p, strategy);
        if (StrUtil.isBlank(query)) {
            return material;
        }
        int materialMax = cfg.getMaterialMax();
        List<String> parts = new ArrayList<>();
        for (Long repoId : cfg.getRepoIds()) {
            try {
                List<SystemKnowledgeChatVO.Citation> cites = knowledgeRepoService.retrieve(repoId, query, 5);
                if (cites != null) {
                    for (SystemKnowledgeChatVO.Citation c : cites) {
                        String text = c.getText();
                        if (StrUtil.isNotBlank(text)) {
                            parts.add("[" + (parts.size() + 1) + "] " + text.replaceAll("\\s+", " ").trim());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("正文前知识检索失败 repoId={}: {}", repoId, e.getMessage());
            }
        }
        if (parts.isEmpty()) {
            warnings.add("知识库检索未命中，正文将基于素材快照生成");
            return material;
        }
        StringBuilder sb = new StringBuilder(material);
        sb.append("\n\n【知识补充（正文前自动检索）】\n");
        for (String part : parts) {
            if (sb.length() >= materialMax) {
                warnings.add("知识补充超限，已截断");
                break;
            }
            sb.append(part).append('\n');
        }
        return sb.toString();
    }

    /** 知识检索 query：主题 + 策略面板（受众/痛点/爆款逻辑），截断防超长 */
    String buildKnowledgeQuery(ToolAiartist p, AiStrategyVO strategy) {
        StringBuilder q = new StringBuilder();
        if (StrUtil.isNotBlank(p.getTopic())) {
            q.append(p.getTopic()).append(' ');
        }
        if (strategy != null) {
            if (StrUtil.isNotBlank(strategy.getTargetAudience())) q.append(strategy.getTargetAudience()).append(' ');
            if (StrUtil.isNotBlank(strategy.getCorePainPoint())) q.append(strategy.getCorePainPoint()).append(' ');
            if (StrUtil.isNotBlank(strategy.getViralLogic())) q.append(strategy.getViralLogic()).append(' ');
        }
        String s = q.toString().trim();
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
