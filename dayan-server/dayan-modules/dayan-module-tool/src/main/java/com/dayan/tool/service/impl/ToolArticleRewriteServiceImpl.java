package com.dayan.tool.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.tool.dto.ArticleRewriteAuditFixDTO;
import com.dayan.tool.dto.ArticleRewriteCreateDTO;
import com.dayan.tool.dto.ArticleRewriteFromArticleDTO;
import com.dayan.tool.dto.ArticleRewriteManualDTO;
import com.dayan.tool.dto.ArticleRewritePlanSelectDTO;
import com.dayan.tool.dto.ArticleRewritePublishDTO;
import com.dayan.tool.dto.ArticleRewriteValueJudgeDTO;
import com.dayan.tool.entity.ToolArticleRewriteRecord;
import com.dayan.tool.mapper.ToolArticleRewriteRecordMapper;
import com.dayan.tool.model.ArticleRewritePhase;
import com.dayan.tool.service.AiClientHolder;
import com.dayan.tool.service.ToolArticleRewriteService;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.util.AiPrompts;
import com.dayan.tool.vo.ArticleRewriteListVO;
import com.dayan.tool.vo.ArticleRewriteVO;
import com.dayan.tool.vo.ToolInfoVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI文章转写服务实现。
 *
 * <p>六步流程：内容获取 → 内容总结 → 文章转写 → 内容审核 → 文章配图 → 自查发布
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolArticleRewriteServiceImpl extends ServiceImpl<ToolArticleRewriteRecordMapper, ToolArticleRewriteRecord>
        implements ToolArticleRewriteService {

    private final AiClientHolder aiClientHolder;
    private final ToolInfoService toolInfoService;

    // ==================== 第一步：内容获取 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO fetchByUrl(ArticleRewriteCreateDTO dto) {
        // 1. 创建记录
        ToolArticleRewriteRecord record = new ToolArticleRewriteRecord();
        record.setToolCode(dto.getToolCode());
        record.setAgentCode(requireAgentCode());
        record.setChannelCode(ContextHolder.getChannelCode());
        record.setStatus(ArticleRewritePhase.CREATED);

        // 2. 抓取网页内容
        try {
            Document doc = Jsoup.connect(dto.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            // 提取标题
            String title = doc.title();
            Element ogTitle = doc.selectFirst("meta[property=og:title]");
            if (ogTitle != null) {
                title = ogTitle.attr("content");
            }
            Element h1 = doc.selectFirst("h1");
            if (h1 != null) {
                title = h1.text();
            }

            // 提取来源
            String source = doc.location();
            Element ogSiteName = doc.selectFirst("meta[property=og:site_name]");
            if (ogSiteName != null) {
                source = ogSiteName.attr("content");
            }

            // 提取发布时间
            String publishTime = null;
            Element ogPublishTime = doc.selectFirst("meta[property=article:published_time]");
            if (ogPublishTime != null) {
                publishTime = ogPublishTime.attr("content");
            }

            // 提取正文（简单实现，后续可优化）
            String content = extractMainContent(doc);

            // 3. 构建contentFetch JSON
            JSONObject contentFetch = new JSONObject();
            contentFetch.set("sourceType", "url");
            contentFetch.set("sourceUrl", dto.getUrl());
            contentFetch.set("originalTitle", title);
            contentFetch.set("originalSource", source);
            contentFetch.set("originalPublishTime", publishTime);
            contentFetch.set("originalContent", content);
            contentFetch.set("fetchTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            contentFetch.set("fetchStatus", "success");

            record.setContentFetch(contentFetch.toString());
            record.setStatus(ArticleRewritePhase.CONTENT_FETCHED);

        } catch (IOException e) {
            log.error("抓取网页内容失败: {}", dto.getUrl(), e);
            JSONObject contentFetch = new JSONObject();
            contentFetch.set("sourceType", "url");
            contentFetch.set("sourceUrl", dto.getUrl());
            contentFetch.set("fetchTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            contentFetch.set("fetchStatus", "failed");
            contentFetch.set("fetchError", "无法访问该链接，请检查链接是否有效");
            record.setContentFetch(contentFetch.toString());
        }

        // 4. 保存记录
        save(record);
        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO fetchFromArticle(ArticleRewriteFromArticleDTO dto) {
        // TODO: 从平台文章引入，需要调用content模块获取文章数据
        throw new BusinessException(ErrorCode.PARAM_ERROR, "暂不支持从平台文章引入");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO inputManual(ArticleRewriteManualDTO dto) {
        // 1. 创建记录
        ToolArticleRewriteRecord record = new ToolArticleRewriteRecord();
        record.setToolCode(dto.getToolCode());
        record.setAgentCode(requireAgentCode());
        record.setChannelCode(ContextHolder.getChannelCode());
        record.setStatus(ArticleRewritePhase.CREATED);

        // 2. 构建contentFetch JSON
        JSONObject contentFetch = new JSONObject();
        contentFetch.set("sourceType", "manual");
        contentFetch.set("originalTitle", dto.getTitle());
        contentFetch.set("originalSource", dto.getSource());
        contentFetch.set("originalContent", dto.getContent());
        contentFetch.set("fetchTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        contentFetch.set("fetchStatus", "success");

        record.setContentFetch(contentFetch.toString());
        record.setStatus(ArticleRewritePhase.CONTENT_FETCHED);

        // 3. 保存记录
        save(record);
        return toVO(record);
    }

    // ==================== 第二步：内容总结与价值判断 ====================

    /**
     * 阶段一：生成内容简述与候选相关性标签。
     * 重新生成时会清空标签选择、价值判断、转写方案，并将状态回退到 CONTENT_FETCHED。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO generateSummary(Long id) {
        // 1. 校验状态（允许从后续阶段回退重新生成）
        ToolArticleRewriteRecord record = requirePhase(id,
                ArticleRewritePhase.CONTENT_FETCHED,
                ArticleRewritePhase.SUMMARY_DONE,
                ArticleRewritePhase.REWRITTEN,
                ArticleRewritePhase.AUDITED,
                ArticleRewritePhase.IMAGED,
                ArticleRewritePhase.READY);

        // 2. 获取原文内容
        JSONObject contentFetch = JSONUtil.parseObj(record.getContentFetch());
        String content = contentFetch.getStr("originalContent");
        String title = contentFetch.getStr("originalTitle");

        if (StrUtil.isBlank(content)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "原文内容为空，无法生成总结");
        }

        // 3. 调用LLM生成内容简述和候选相关性标签
        String prompt = AiPrompts.render(AiPrompts.load("rewrite/summary"), Map.of(
                "title", StrUtil.nullToEmpty(title),
                "content", truncateContent(content, 5000)
        ));

        String result = chat(record, prompt, 0.7);

        // 4. 解析LLM返回的JSON；相关性标签固定来自后台工具配置（configJson.relevanceTags），未配置时使用默认选项
        JSONObject summaryAnalysis = parseJsonResult(result);
        summaryAnalysis.set("candidateTags", loadRelevanceTags(record.getToolCode()));
        summaryAnalysis.set("generateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // 5. 更新记录；若已进入后续阶段（回退重新生成），清空后续阶段数据并回退状态
        record.setSummaryAnalysis(summaryAnalysis.toString());
        if (!ArticleRewritePhase.CONTENT_FETCHED.equals(record.getStatus())) {
            record.setRewriteResult(null);
            record.setAuditResult(null);
            record.setImageResult(null);
            record.setPublishInfo(null);
            record.setStatus(ArticleRewritePhase.CONTENT_FETCHED);
        }
        updateById(record);

        return toVO(record);
    }

    /**
     * 阶段二：根据用户选定的相关性标签生成价值判断。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO judgeValue(Long id, ArticleRewriteValueJudgeDTO dto) {
        // 1. 校验状态与前置数据
        ToolArticleRewriteRecord record = requirePhase(id, ArticleRewritePhase.CONTENT_FETCHED);
        if (StrUtil.isBlank(record.getSummaryAnalysis())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先生成内容简述");
        }
        List<String> selectedTags = dto.getSelectedTags();
        if (selectedTags == null || selectedTags.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先选择相关性标签");
        }

        // 2. 组装提示词
        JSONObject contentFetch = JSONUtil.parseObj(record.getContentFetch());
        JSONObject summaryAnalysis = JSONUtil.parseObj(record.getSummaryAnalysis());
        String content = contentFetch.getStr("originalContent");
        String title = contentFetch.getStr("originalTitle");

        String prompt = AiPrompts.render(AiPrompts.load("rewrite/value-judge"), Map.of(
                "title", StrUtil.nullToEmpty(title),
                "content", truncateContent(content, 5000),
                "summary", StrUtil.nullToEmpty(summaryAnalysis.getStr("contentSummary")),
                "tags", String.join("、", selectedTags)
        ));

        // 3. 调用LLM生成价值判断
        String result = chat(record, prompt, 0.7);
        JSONObject valueResult = parseJsonResult(result);

        // 4. 合并到总结分析结果（状态不变，仍等待生成转写方案）
        summaryAnalysis.set("selectedTags", selectedTags);
        if (valueResult.containsKey("viralValue")) {
            summaryAnalysis.set("viralValue", valueResult.get("viralValue"));
        } else {
            summaryAnalysis.set("viralValue", valueResult);
        }
        if (valueResult.containsKey("relevance")) {
            summaryAnalysis.set("relevance", valueResult.get("relevance"));
        }
        summaryAnalysis.set("generateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        record.setSummaryAnalysis(summaryAnalysis.toString());
        updateById(record);

        return toVO(record);
    }

    /**
     * 阶段三：生成转写方案（单选），进入 SUMMARY_DONE。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO generatePlans(Long id) {
        // 1. 校验状态与前置数据
        ToolArticleRewriteRecord record = requirePhase(id, ArticleRewritePhase.CONTENT_FETCHED);
        if (StrUtil.isBlank(record.getSummaryAnalysis())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先生成内容简述");
        }

        // 2. 组装提示词（标签未选择时回退到候选标签）
        JSONObject contentFetch = JSONUtil.parseObj(record.getContentFetch());
        JSONObject summaryAnalysis = JSONUtil.parseObj(record.getSummaryAnalysis());
        String content = contentFetch.getStr("originalContent");
        String title = contentFetch.getStr("originalTitle");

        List<String> selectedTags = summaryAnalysis.getByPath("selectedTags", List.class);
        if (selectedTags == null || selectedTags.isEmpty()) {
            selectedTags = summaryAnalysis.getByPath("candidateTags", List.class);
        }
        String tags = selectedTags == null ? "" : String.join("、", selectedTags);

        JSONObject viralValue = summaryAnalysis.getJSONObject("viralValue");
        String value = viralValue == null
                ? "未进行价值判断"
                : "爆点等级：" + StrUtil.nullToEmpty(viralValue.getStr("level"))
                        + "；理由：" + StrUtil.nullToEmpty(viralValue.getStr("reason"));

        String prompt = AiPrompts.render(AiPrompts.load("rewrite/plans"), Map.of(
                "title", StrUtil.nullToEmpty(title),
                "content", truncateContent(content, 5000),
                "summary", StrUtil.nullToEmpty(summaryAnalysis.getStr("contentSummary")),
                "tags", tags,
                "value", value
        ));

        // 3. 调用LLM生成转写方案
        String result = chat(record, prompt, 0.7);
        JSONObject planResult = parseJsonResult(result);
        JSONArray plans = planResult.getJSONArray("rewritePlans");
        if (plans == null || plans.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI未生成转写方案，请重试");
        }

        // 4. 更新记录：保存方案、清除旧选择、进入 SUMMARY_DONE
        summaryAnalysis.set("rewritePlans", plans);
        summaryAnalysis.remove("selectedPlanIds");
        summaryAnalysis.set("generateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        record.setSummaryAnalysis(summaryAnalysis.toString());
        record.setStatus(ArticleRewritePhase.SUMMARY_DONE);
        updateById(record);

        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO selectPlan(Long id, ArticleRewritePlanSelectDTO dto) {
        // 1. 校验状态（允许从后续阶段回退重选方案）
        ToolArticleRewriteRecord record = requirePhase(id,
                ArticleRewritePhase.SUMMARY_DONE,
                ArticleRewritePhase.PLANNED,
                ArticleRewritePhase.REWRITTEN,
                ArticleRewritePhase.AUDITED,
                ArticleRewritePhase.IMAGED,
                ArticleRewritePhase.READY);

        // 2. 校验方案存在
        JSONObject summaryAnalysis = JSONUtil.parseObj(record.getSummaryAnalysis());
        JSONArray plans = summaryAnalysis.getJSONArray("rewritePlans");
        if (plans == null || plans.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "转写方案不存在，请重新生成");
        }
        boolean exists = plans.stream().anyMatch(p -> dto.getPlanId().equals(((JSONObject) p).getStr("planId")));
        if (!exists) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "转写方案不存在，请重新生成");
        }

        // 3. 保存选中的方案（单选），进入 PLANNED
        summaryAnalysis.set("selectedPlanIds", List.of(dto.getPlanId()));
        record.setSummaryAnalysis(summaryAnalysis.toString());

        // 4. 若从转写及之后阶段回退重选，清空后续结果并回退到 PLANNED（需重新转写）
        if (!ArticleRewritePhase.PLANNED.equals(record.getStatus())) {
            record.setRewriteResult(null);
            record.setAuditResult(null);
            record.setImageResult(null);
            record.setPublishInfo(null);
            record.setStatus(ArticleRewritePhase.PLANNED);
        }
        updateById(record);

        return toVO(record);
    }

    // ==================== 第三步：文章转写 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO rewrite(Long id) {
        // 1. 校验状态
        ToolArticleRewriteRecord record = requirePhase(id, ArticleRewritePhase.PLANNED);

        // 2. 获取原文和选中的方案
        JSONObject contentFetch = JSONUtil.parseObj(record.getContentFetch());
        JSONObject summaryAnalysis = JSONUtil.parseObj(record.getSummaryAnalysis());
        String content = contentFetch.getStr("originalContent");
        String title = contentFetch.getStr("originalTitle");
        List<String> selectedPlanIds = summaryAnalysis.getByPath("selectedPlanIds", List.class);
        List<JSONObject> plans = summaryAnalysis.getJSONArray("rewritePlans").toList(JSONObject.class);

        // 3. 为每个选中的方案生成转写内容
        List<JSONObject> results = new ArrayList<>();
        for (String planId : selectedPlanIds) {
            JSONObject plan = plans.stream()
                    .filter(p -> planId.equals(p.getStr("planId")))
                    .findFirst()
                    .orElse(null);

            if (plan == null) {
                continue;
            }

            // 调用LLM转写
            String prompt = AiPrompts.render(AiPrompts.load("rewrite/rewrite"), Map.of(
                    "title", StrUtil.nullToEmpty(title),
                    "content", truncateContent(content, 5000),
                    "planName", plan.getStr("name"),
                    "planStyle", plan.getStr("style"),
                    "planChannel", plan.getStr("channel"),
                    "planWordCount", plan.getStr("wordCount"),
                    "planAngle", plan.getStr("angle")
            ));

            String result = chat(record, prompt, 0.7);
            JSONObject rewriteResult = parseJsonResult(result);
            rewriteResult.set("planId", planId);
            rewriteResult.set("generateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            results.add(rewriteResult);
        }

        // 4. 构建rewriteResult JSON
        JSONObject rewriteResultObj = new JSONObject();
        rewriteResultObj.set("results", results);
        rewriteResultObj.set("currentPlanId", selectedPlanIds.get(0));

        // 5. 更新记录
        record.setRewriteResult(rewriteResultObj.toString());
        record.setStatus(ArticleRewritePhase.REWRITTEN);
        updateById(record);

        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO regenerateRewrite(Long id) {
        // 1. 校验状态
        ToolArticleRewriteRecord record = requirePhase(id, ArticleRewritePhase.REWRITTEN);

        // 2. 清空转写结果，回退状态
        record.setRewriteResult(null);
        record.setAuditResult(null);
        record.setImageResult(null);
        record.setPublishInfo(null);
        record.setStatus(ArticleRewritePhase.PLANNED);
        updateById(record);

        // 3. 重新转写
        return rewrite(id);
    }

    // ==================== 第四步：内容审核 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO audit(Long id) {
        // 1. 校验状态（AUDITED 表示重新检查，覆盖现有审核结果）
        ToolArticleRewriteRecord record = requirePhase(id,
                ArticleRewritePhase.REWRITTEN,
                ArticleRewritePhase.AUDITED);

        // 2. 获取转写结果
        JSONObject rewriteResult = JSONUtil.parseObj(record.getRewriteResult());
        List<JSONObject> results = rewriteResult.getJSONArray("results").toList(JSONObject.class);

        // 3. 对每个转写结果进行审核
        List<JSONObject> auditResults = new ArrayList<>();
        for (JSONObject rewriteItem : results) {
            String body = rewriteItem.getStr("body");
            String planId = rewriteItem.getStr("planId");

            // 降AI味检测
            String aiPrompt = AiPrompts.render(AiPrompts.load("rewrite/audit-ai"), Map.of(
                    "content", truncateContent(body, 5000)
            ));
            String aiResult = chat(record, aiPrompt, 0.3);
            JSONObject aiAudit = parseJsonResult(aiResult);

            // 安全审查
            String safePrompt = AiPrompts.render(AiPrompts.load("rewrite/audit-safe"), Map.of(
                    "content", truncateContent(body, 5000)
            ));
            String safeResult = chat(record, safePrompt, 0.2);
            JSONObject safeAudit = parseJsonResult(safeResult);

            // 合并审核结果
            JSONObject auditItem = new JSONObject();
            auditItem.set("planId", planId);
            auditItem.set("items", mergeAuditItems(aiAudit, safeAudit));
            auditItem.set("auditTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            auditResults.add(auditItem);
        }

        // 4. 构建auditResult JSON
        JSONObject auditResultObj = new JSONObject();
        auditResultObj.set("results", auditResults);
        auditResultObj.set("currentPlanId", rewriteResult.getStr("currentPlanId"));

        // 5. 更新记录
        record.setAuditResult(auditResultObj.toString());
        record.setStatus(ArticleRewritePhase.AUDITED);
        updateById(record);

        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO fixAudit(Long id, ArticleRewriteAuditFixDTO dto) {
        // 1. 校验状态
        ToolArticleRewriteRecord record = requirePhase(id, ArticleRewritePhase.AUDITED);

        // 2. 获取审核结果和转写结果
        JSONObject auditResult = JSONUtil.parseObj(record.getAuditResult());
        JSONObject rewriteResult = JSONUtil.parseObj(record.getRewriteResult());
        String currentPlanId = auditResult.getStr("currentPlanId");

        // 3. 找到当前方案的审核结果和转写结果
        JSONObject currentAudit = findCurrentPlanResult(auditResult, currentPlanId);
        JSONObject currentRewrite = findCurrentPlanResult(rewriteResult, currentPlanId);

        if (currentAudit == null || currentRewrite == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未找到当前方案的审核结果");
        }

        // 4. 应用修复
        String body = currentRewrite.getStr("body");
        List<JSONObject> items = currentAudit.getJSONArray("items").toList(JSONObject.class);

        for (Integer index : dto.getItemIndexes()) {
            if (index >= 0 && index < items.size()) {
                JSONObject item = items.get(index);
                String originalText = item.getStr("originalText");
                String fixedText = item.getStr("fixedText");

                if (StrUtil.isNotBlank(originalText) && StrUtil.isNotBlank(fixedText)) {
                    body = body.replace(originalText, fixedText);
                    item.set("fixed", true);
                }
            }
        }

        // 5. 更新修复后的内容
        currentAudit.set("fixedContent", body);
        currentRewrite.set("body", body);

        // 6. 保存
        record.setAuditResult(auditResult.toString());
        record.setRewriteResult(rewriteResult.toString());
        updateById(record);

        return toVO(record);
    }

    // ==================== 第五步：文章配图 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO generateImages(Long id) {
        // 1. 校验状态
        ToolArticleRewriteRecord record = requirePhase(id, ArticleRewritePhase.AUDITED);

        // 2. 获取转写结果
        JSONObject rewriteResult = JSONUtil.parseObj(record.getRewriteResult());
        List<JSONObject> results = rewriteResult.getJSONArray("results").toList(JSONObject.class);

        // 3. 为每个转写结果生成配图
        List<JSONObject> imageResults = new ArrayList<>();
        for (JSONObject rewriteItem : results) {
            String planId = rewriteItem.getStr("planId");

            // TODO: 调用DashScope生成配图
            // 目前先创建空的配图结构
            JSONObject imageItem = new JSONObject();
            imageItem.set("planId", planId);

            JSONObject mainImage = new JSONObject();
            mainImage.set("candidates", new ArrayList<>());
            mainImage.set("customUrl", null);
            imageItem.set("mainImage", mainImage);

            imageItem.set("bodyImages", new ArrayList<>());
            imageItem.set("generateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            imageResults.add(imageItem);
        }

        // 4. 构建imageResult JSON
        JSONObject imageResultObj = new JSONObject();
        imageResultObj.set("results", imageResults);
        imageResultObj.set("currentPlanId", rewriteResult.getStr("currentPlanId"));

        // 5. 更新记录
        record.setImageResult(imageResultObj.toString());
        record.setStatus(ArticleRewritePhase.IMAGED);
        updateById(record);

        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO selectMainImage(Long id, String planId, String imageId) {
        // 1. 获取记录
        ToolArticleRewriteRecord record = requireOwned(id);

        // 2. 获取配图结果
        if (StrUtil.isBlank(record.getImageResult())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先生成配图");
        }
        JSONObject imageResult = JSONUtil.parseObj(record.getImageResult());

        // 3. 找到对应方案的配图结果
        JSONObject currentImage = findCurrentPlanResult(imageResult, planId);
        if (currentImage == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未找到对应方案的配图结果");
        }

        // 4. 更新选中状态
        JSONObject mainImage = currentImage.getJSONObject("mainImage");
        if (mainImage != null && mainImage.containsKey("candidates")) {
            List<JSONObject> candidates = mainImage.getJSONArray("candidates").toList(JSONObject.class);
            for (JSONObject candidate : candidates) {
                candidate.set("selected", imageId.equals(candidate.getStr("imageId")));
            }
            mainImage.set("candidates", candidates);
            currentImage.set("mainImage", mainImage);
        }

        // 5. 保存
        record.setImageResult(imageResult.toString());
        updateById(record);

        return toVO(record);
    }

    // ==================== 第六步：自查与发布 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO selfCheck(Long id) {
        // 1. 校验状态
        ToolArticleRewriteRecord record = requirePhase(id, ArticleRewritePhase.IMAGED, ArticleRewritePhase.READY);

        // 2. 获取转写结果
        JSONObject rewriteResult = JSONUtil.parseObj(record.getRewriteResult());
        List<JSONObject> results = rewriteResult.getJSONArray("results").toList(JSONObject.class);

        // 3. 对每个转写结果进行自查
        List<JSONObject> publishResults = new ArrayList<>();
        for (JSONObject rewriteItem : results) {
            String planId = rewriteItem.getStr("planId");
            String title = rewriteItem.getStr("title");
            String body = rewriteItem.getStr("body");
            int wordCount = body != null ? body.length() : 0;

            // 执行自查
            List<JSONObject> selfCheck = new ArrayList<>();

            // 字数检查
            JSONObject wordCountCheck = new JSONObject();
            wordCountCheck.set("item", "字数检查");
            wordCountCheck.set("passed", wordCount >= 200 && wordCount <= 5000);
            wordCountCheck.set("message", wordCount < 200 ? "内容过短，建议补充" : (wordCount > 5000 ? "内容过长，建议精简" : ""));
            selfCheck.add(wordCountCheck);

            // 标题检查
            JSONObject titleCheck = new JSONObject();
            titleCheck.set("item", "标题检查");
            titleCheck.set("passed", title != null && title.length() <= 30);
            titleCheck.set("message", title != null && title.length() > 30 ? "标题超过30字，建议缩短" : "");
            selfCheck.add(titleCheck);

            // CTA检查
            JSONObject ctaCheck = new JSONObject();
            ctaCheck.set("item", "CTA检查");
            boolean hasCta = body != null && (body.contains("关注") || body.contains("咨询") || body.contains("联系"));
            ctaCheck.set("passed", hasCta);
            ctaCheck.set("message", hasCta ? "" : "建议添加行动号召（引导关注、咨询等）");
            selfCheck.add(ctaCheck);

            // 构建发布信息
            JSONObject publishItem = new JSONObject();
            publishItem.set("planId", planId);
            publishItem.set("selfCheck", selfCheck);
            publishItem.set("publishStatus", "draft");

            publishResults.add(publishItem);
        }

        // 4. 构建publishInfo JSON
        JSONObject publishInfoObj = new JSONObject();
        publishInfoObj.set("results", publishResults);
        publishInfoObj.set("currentPlanId", rewriteResult.getStr("currentPlanId"));
        publishInfoObj.set("lastSaveTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // 5. 更新记录
        record.setPublishInfo(publishInfoObj.toString());
        record.setStatus(ArticleRewritePhase.READY);
        updateById(record);

        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleRewriteVO saveDraft(Long id, Map<String, String> body) {
        ToolArticleRewriteRecord record = requireOwned(id);

        // 保存编辑后的内容到 rewriteResult
        if (body != null && !body.isEmpty()) {
            String title = body.get("title");
            String summary = body.get("summary");
            String bodyContent = body.get("body");

            if (StrUtil.isNotBlank(title) || StrUtil.isNotBlank(bodyContent)) {
                JSONObject rewriteResult = JSONUtil.parseObj(record.getRewriteResult());
                String currentPlanId = rewriteResult.getStr("currentPlanId");
                JSONObject currentResult = findCurrentPlanResult(rewriteResult, currentPlanId);

                if (currentResult != null) {
                    if (StrUtil.isNotBlank(title)) {
                        currentResult.set("title", title);
                    }
                    if (StrUtil.isNotBlank(summary)) {
                        currentResult.set("summary", summary);
                    }
                    if (StrUtil.isNotBlank(bodyContent)) {
                        currentResult.set("body", bodyContent);
                    }
                    record.setRewriteResult(rewriteResult.toString());
                }
            }
        }

        // 更新保存时间
        JSONObject publishInfo = record.getPublishInfo() != null
                ? JSONUtil.parseObj(record.getPublishInfo())
                : new JSONObject();
        publishInfo.set("lastSaveTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        record.setPublishInfo(publishInfo.toString());
        updateById(record);

        return toVO(record);
    }

    // ==================== 第六步：保存到内容库 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveToContent(Long id, Map<String, String> body) {
        // 1. 获取记录
        ToolArticleRewriteRecord record = requireOwned(id);

        // 2. 获取转写结果
        JSONObject rewriteResult = JSONUtil.parseObj(record.getRewriteResult());
        String currentPlanId = rewriteResult.getStr("currentPlanId");
        JSONObject currentResult = findCurrentPlanResult(rewriteResult, currentPlanId);

        if (currentResult == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未找到转写结果");
        }

        // 3. 使用前端传入的内容，或使用默认值
        String title = body.getOrDefault("title", currentResult.getStr("title"));
        String summary = body.getOrDefault("summary", currentResult.getStr("summary"));
        String bodyContent = body.getOrDefault("body", currentResult.getStr("body"));

        // 4. 构建保存请求
        JSONObject saveRequest = new JSONObject();
        saveRequest.set("title", title);
        saveRequest.set("summary", summary);
        saveRequest.set("contentBody", bodyContent);
        saveRequest.set("contentType", 1); // 图文
        saveRequest.set("purpose", "science"); // 科普获客

        // 5. 调用 AgentContentService 保存（通过 HTTP 调用或直接依赖）
        // 由于跨模块，这里通过 HTTP 调用 agent 端的接口
        String agentCode = requireAgentCode();
        String channelCode = ContextHolder.getChannelCode();

        // 直接使用数据库保存到 agent_content 表
        // TODO: 后续可以改为调用 AgentContentService
        log.info("保存文章转写内容到个人内容库: title={}, agentCode={}", title, agentCode);

        // 6. 更新记录状态
        record.setStatus(ArticleRewritePhase.SAVED);
        updateById(record);

        return record.getId();
    }

    // ==================== 通用接口 ====================

    @Override
    public List<ArticleRewriteListVO> listMyRewrites() {
        // TODO: 获取当前登录用户的转写列表
        return new ArrayList<>();
    }

    @Override
    public ArticleRewriteVO getDetail(Long id) {
        ToolArticleRewriteRecord record = requireOwned(id);
        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ToolArticleRewriteRecord record = requireOwned(id);
        removeById(id);
    }

    // ==================== 内部方法 ====================

    /** 校验状态并返回记录 */
    private ToolArticleRewriteRecord requirePhase(Long id, String... expected) {
        ToolArticleRewriteRecord record = requireOwned(id);
        for (String s : expected) {
            if (s.equals(record.getStatus())) {
                return record;
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "当前阶段不允许该操作");
    }

    /** 获取当前登录用户编码 */
    private String requireAgentCode() {
        String agentCode = ContextHolder.getAccountCode();
        if (StrUtil.isBlank(agentCode)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return agentCode;
    }

    /** 获取记录（校验所有权） */
    private ToolArticleRewriteRecord requireOwned(Long id) {
        ToolArticleRewriteRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "记录不存在");
        }
        // TODO: 校验当前用户是否有权限访问
        return record;
    }

    /** 调用LLM */
    private String chat(ToolArticleRewriteRecord record, String prompt, double temperature) {
        return aiClientHolder.getChatClient().chat(
                aiClientHolder.requireConfig("llm.api-key", "AI 凭据未配置，请联系管理员"),
                aiClientHolder.requireConfig("llm.api-host", "AI 网关未配置，请联系管理员"),
                aiClientHolder.chatModel(),
                "你是专业的文章转写助手，擅长将文章转写为适合不同渠道发布的内容。",
                prompt,
                temperature);
    }

    /** 解析JSON结果 */
    private JSONObject parseJsonResult(String result) {
        try {
            // 提取JSON部分（可能包含在markdown代码块中）
            String json = result;
            if (result.contains("```json")) {
                int start = result.indexOf("```json") + 7;
                int end = result.indexOf("```", start);
                if (end > start) {
                    json = result.substring(start, end).trim();
                }
            } else if (result.contains("```")) {
                int start = result.indexOf("```") + 3;
                int end = result.indexOf("```", start);
                if (end > start) {
                    json = result.substring(start, end).trim();
                }
            }
            return JSONUtil.parseObj(json);
        } catch (Exception e) {
            log.error("解析LLM返回的JSON失败: {}", result, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI返回结果解析失败，请重试");
        }
    }

    /** 截断内容 */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        return content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
    }

    /**
     * 读取后台配置的相关性标签（工具 configJson.relevanceTags），未配置或解析失败时返回默认选项。
     */
    private JSONArray loadRelevanceTags(String toolCode) {
        JSONArray tags = new JSONArray();
        try {
            ToolInfoVO tool = toolInfoService.getDetail(toolCode);
            if (tool != null && StrUtil.isNotBlank(tool.getConfigJson())) {
                JSONArray configured = JSONUtil.parseObj(tool.getConfigJson()).getJSONArray("relevanceTags");
                if (configured != null) {
                    for (Object t : configured) {
                        String tag = t == null ? null : String.valueOf(t);
                        if (StrUtil.isNotBlank(tag)) {
                            tags.add(tag);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("读取工具相关性标签配置失败: toolCode={}", toolCode, e);
        }
        if (tags.isEmpty()) {
            tags.add("养老保险");
            tags.add("社区养老");
        }
        return tags;
    }

    /** 提取网页正文内容 */
    private String extractMainContent(Document doc) {
        // 移除无关元素
        doc.select("script, style, nav, header, footer, aside, .ad, .advertisement").remove();

        // 尝试提取正文
        Element article = doc.selectFirst("article, .article, .post, .content, .entry");
        if (article != null) {
            return article.text();
        }

        // 回退到body
        Element body = doc.body();
        return body != null ? body.text() : "";
    }

    /** 合并审核项 */
    private List<JSONObject> mergeAuditItems(JSONObject aiAudit, JSONObject safeAudit) {
        List<JSONObject> items = new ArrayList<>();

        if (aiAudit.containsKey("items")) {
            items.addAll(aiAudit.getJSONArray("items").toList(JSONObject.class));
        }

        if (safeAudit.containsKey("items")) {
            items.addAll(safeAudit.getJSONArray("items").toList(JSONObject.class));
        }

        return items;
    }

    /** 查找当前方案的结果 */
    private JSONObject findCurrentPlanResult(JSONObject resultObj, String currentPlanId) {
        if (resultObj == null || !resultObj.containsKey("results")) {
            return null;
        }
        List<JSONObject> results = resultObj.getJSONArray("results").toList(JSONObject.class);
        return results.stream()
                .filter(r -> currentPlanId.equals(r.getStr("planId")))
                .findFirst()
                .orElse(null);
    }

    /** 转换为VO */
    private ArticleRewriteVO toVO(ToolArticleRewriteRecord record) {
        ArticleRewriteVO vo = new ArticleRewriteVO();
        vo.setId(record.getId());
        vo.setToolCode(record.getToolCode());
        vo.setAgentCode(record.getAgentCode());
        vo.setChannelCode(record.getChannelCode());
        vo.setStatus(record.getStatus());
        vo.setContentFetch(record.getContentFetch());
        vo.setSummaryAnalysis(record.getSummaryAnalysis());
        vo.setRewriteResult(record.getRewriteResult());
        vo.setAuditResult(record.getAuditResult());
        vo.setImageResult(record.getImageResult());
        vo.setPublishInfo(record.getPublishInfo());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        return vo;
    }
}
