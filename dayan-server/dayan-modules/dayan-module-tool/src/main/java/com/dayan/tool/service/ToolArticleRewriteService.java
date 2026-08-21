package com.dayan.tool.service;

import com.dayan.tool.dto.ArticleRewriteAuditFixDTO;
import com.dayan.tool.dto.ArticleRewriteCreateDTO;
import com.dayan.tool.dto.ArticleRewriteFromArticleDTO;
import com.dayan.tool.dto.ArticleRewriteManualDTO;
import com.dayan.tool.dto.ArticleRewritePlanSelectDTO;
import com.dayan.tool.dto.ArticleRewritePublishDTO;
import com.dayan.tool.dto.ArticleRewriteValueJudgeDTO;
import com.dayan.tool.vo.ArticleRewriteListVO;
import com.dayan.tool.vo.ArticleRewriteVO;

import java.util.List;
import java.util.Map;

/**
 * AI文章转写服务接口。
 *
 * <p>六步流程：内容获取 → 内容总结 → 文章转写 → 内容审核 → 文章配图 → 自查发布
 */
public interface ToolArticleRewriteService {

    // ==================== 第一步：内容获取 ====================

    /** 通过URL链接抓取内容 */
    ArticleRewriteVO fetchByUrl(ArticleRewriteCreateDTO dto);

    /** 从平台文章引入 */
    ArticleRewriteVO fetchFromArticle(ArticleRewriteFromArticleDTO dto);

    /** 手动输入内容 */
    ArticleRewriteVO inputManual(ArticleRewriteManualDTO dto);

    // ==================== 第二步：内容总结与价值判断 ====================

    /** 生成内容简述与候选相关性标签（重新生成时清空后续阶段数据） */
    ArticleRewriteVO generateSummary(Long id);

    /** 根据用户选定的相关性标签生成价值判断 */
    ArticleRewriteVO judgeValue(Long id, ArticleRewriteValueJudgeDTO dto);

    /** 生成转写方案（进入 SUMMARY_DONE） */
    ArticleRewriteVO generatePlans(Long id);

    /** 选择转写方案（单选） */
    ArticleRewriteVO selectPlan(Long id, ArticleRewritePlanSelectDTO dto);

    // ==================== 第三步：文章转写 ====================

    /** 执行转写（为每个选中的方案生成转写内容） */
    ArticleRewriteVO rewrite(Long id);

    /** 重新转写（清除当前转写结果，重新生成） */
    ArticleRewriteVO regenerateRewrite(Long id);

    // ==================== 第四步：内容审核 ====================

    /** 执行审核（降AI味检测 + 安全审查） */
    ArticleRewriteVO audit(Long id);

    /** 一键修复审核问题 */
    ArticleRewriteVO fixAudit(Long id, ArticleRewriteAuditFixDTO dto);

    // ==================== 第五步：文章配图 ====================

    /** 生成配图 */
    ArticleRewriteVO generateImages(Long id);

    /** 保存主图选择 */
    ArticleRewriteVO selectMainImage(Long id, String planId, String imageId);

    // ==================== 第六步：自查与发布 ====================

    /** 执行自查 */
    ArticleRewriteVO selfCheck(Long id);

    /** 保存草稿 */
    ArticleRewriteVO saveDraft(Long id, Map<String, String> body);

    /** 保存到个人内容库 */
    Long saveToContent(Long id, Map<String, String> body);

    // ==================== 通用接口 ====================

    /** 获取转写项目列表 */
    List<ArticleRewriteListVO> listMyRewrites();

    /** 获取转写项目详情（恢复草稿） */
    ArticleRewriteVO getDetail(Long id);

    /** 删除转写项目 */
    void delete(Long id);
}
