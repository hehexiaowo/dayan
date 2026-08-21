package com.dayan.tool.controller.agent;

import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import java.util.Map;
import com.dayan.tool.dto.ArticleRewriteAuditFixDTO;
import com.dayan.tool.dto.ArticleRewriteCreateDTO;
import com.dayan.tool.dto.ArticleRewriteFromArticleDTO;
import com.dayan.tool.dto.ArticleRewriteManualDTO;
import com.dayan.tool.dto.ArticleRewritePlanSelectDTO;
import com.dayan.tool.dto.ArticleRewritePublishDTO;
import com.dayan.tool.dto.ArticleRewriteValueJudgeDTO;
import com.dayan.tool.service.ToolArticleRewriteService;
import com.dayan.tool.vo.ArticleRewriteListVO;
import com.dayan.tool.vo.ArticleRewriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 文章转写六步流程。路径 {@code /agent-api/tools/article-rewrite/...}。
 * agentCode 服务端从登录上下文注入，防越权；写操作记操作日志。
 */
@Tag(name = "Agent 文章转写")
@RestController
@RequestMapping("/tools/article-rewrite")
@RequiredArgsConstructor
public class AgentToolArticleRewriteController {

    private final ToolArticleRewriteService rewriteService;

    // ==================== 第一步：内容获取 ====================

    @Operation(summary = "通过URL链接抓取内容")
    @OperationLog(module = "文章转写", action = "抓取URL内容")
    @PostMapping("/fetch-url")
    public R<ArticleRewriteVO> fetchByUrl(@RequestBody @Valid ArticleRewriteCreateDTO dto) {
        return R.ok(rewriteService.fetchByUrl(dto));
    }

    @Operation(summary = "从平台文章引入")
    @OperationLog(module = "文章转写", action = "从文章引入")
    @PostMapping("/fetch-article")
    public R<ArticleRewriteVO> fetchFromArticle(@RequestBody @Valid ArticleRewriteFromArticleDTO dto) {
        return R.ok(rewriteService.fetchFromArticle(dto));
    }

    @Operation(summary = "手动输入内容")
    @OperationLog(module = "文章转写", action = "手动输入内容")
    @PostMapping("/input-manual")
    public R<ArticleRewriteVO> inputManual(@RequestBody @Valid ArticleRewriteManualDTO dto) {
        return R.ok(rewriteService.inputManual(dto));
    }

    // ==================== 第二步：内容总结与价值判断 ====================

    @Operation(summary = "生成内容简述与候选相关性标签")
    @OperationLog(module = "文章转写", action = "生成内容简述")
    @PostMapping("/{id}/summary")
    public R<ArticleRewriteVO> generateSummary(@PathVariable Long id) {
        return R.ok(rewriteService.generateSummary(id));
    }

    @Operation(summary = "根据选定的相关性标签生成价值判断")
    @OperationLog(module = "文章转写", action = "生成价值判断")
    @PostMapping("/{id}/summary/value")
    public R<ArticleRewriteVO> judgeValue(@PathVariable Long id, @RequestBody @Valid ArticleRewriteValueJudgeDTO dto) {
        return R.ok(rewriteService.judgeValue(id, dto));
    }

    @Operation(summary = "生成转写方案")
    @OperationLog(module = "文章转写", action = "生成转写方案")
    @PostMapping("/{id}/summary/plans")
    public R<ArticleRewriteVO> generatePlans(@PathVariable Long id) {
        return R.ok(rewriteService.generatePlans(id));
    }

    @Operation(summary = "选择转写方案（单选）")
    @OperationLog(module = "文章转写", action = "选择方案")
    @PostMapping("/{id}/plan/select")
    public R<ArticleRewriteVO> selectPlan(@PathVariable Long id, @RequestBody @Valid ArticleRewritePlanSelectDTO dto) {
        return R.ok(rewriteService.selectPlan(id, dto));
    }

    // ==================== 第三步：文章转写 ====================

    @Operation(summary = "执行转写（为每个选中的方案生成转写内容）")
    @OperationLog(module = "文章转写", action = "执行转写")
    @PostMapping("/{id}/rewrite")
    public R<ArticleRewriteVO> rewrite(@PathVariable Long id) {
        return R.ok(rewriteService.rewrite(id));
    }

    @Operation(summary = "重新转写（清除当前转写结果，重新生成）")
    @OperationLog(module = "文章转写", action = "重新转写")
    @PostMapping("/{id}/rewrite/regenerate")
    public R<ArticleRewriteVO> regenerateRewrite(@PathVariable Long id) {
        return R.ok(rewriteService.regenerateRewrite(id));
    }

    // ==================== 第四步：内容审核 ====================

    @Operation(summary = "执行审核（降AI味检测 + 安全审查）")
    @OperationLog(module = "文章转写", action = "执行审核")
    @PostMapping("/{id}/audit")
    public R<ArticleRewriteVO> audit(@PathVariable Long id) {
        return R.ok(rewriteService.audit(id));
    }

    @Operation(summary = "一键修复审核问题")
    @OperationLog(module = "文章转写", action = "修复审核问题")
    @PostMapping("/{id}/audit/fix")
    public R<ArticleRewriteVO> fixAudit(@PathVariable Long id, @RequestBody @Valid ArticleRewriteAuditFixDTO dto) {
        return R.ok(rewriteService.fixAudit(id, dto));
    }

    // ==================== 第五步：文章配图 ====================

    @Operation(summary = "生成配图")
    @OperationLog(module = "文章转写", action = "生成配图")
    @PostMapping("/{id}/images/generate")
    public R<ArticleRewriteVO> generateImages(@PathVariable Long id) {
        return R.ok(rewriteService.generateImages(id));
    }

    @Operation(summary = "保存主图选择")
    @OperationLog(module = "文章转写", action = "保存主图选择")
    @PostMapping("/{id}/images/select")
    public R<ArticleRewriteVO> selectMainImage(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(rewriteService.selectMainImage(id, body.get("planId"), body.get("imageId")));
    }

    // ==================== 第六步：保存 ====================

    @Operation(summary = "保存到个人内容库")
    @OperationLog(module = "文章转写", action = "保存到内容库")
    @PostMapping("/{id}/save-to-content")
    public R<Long> saveToContent(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(rewriteService.saveToContent(id, body));
    }

    @Operation(summary = "保存草稿（可含编辑内容）")
    @OperationLog(module = "文章转写", action = "保存草稿")
    @PostMapping("/{id}/save")
    public R<ArticleRewriteVO> saveDraft(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return R.ok(rewriteService.saveDraft(id, body));
    }

    // ==================== 通用接口 ====================

    @Operation(summary = "我的转写列表")
    @GetMapping("/list")
    public R<List<ArticleRewriteListVO>> list() {
        return R.ok(rewriteService.listMyRewrites());
    }

    @Operation(summary = "转写项目详情（恢复草稿）")
    @GetMapping("/{id}")
    public R<ArticleRewriteVO> detail(@PathVariable Long id) {
        return R.ok(rewriteService.getDetail(id));
    }

    @Operation(summary = "删除转写项目")
    @OperationLog(module = "文章转写", action = "删除项目")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        rewriteService.delete(id);
        return R.ok();
    }
}
