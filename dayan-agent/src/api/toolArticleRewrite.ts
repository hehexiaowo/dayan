import request from '@/utils/request'
import type {
  ArticleRewriteRecord,
  ArticleRewriteListItem,
  PublishChannel,
} from '@/types/toolArticleRewrite'

// ==================== 第一步：内容获取 ====================

/** 通过URL链接抓取内容 */
export function fetchByUrl(toolCode: string, url: string): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: '/tools/article-rewrite/fetch-url',
    method: 'POST',
    data: { toolCode, url },
  })
}

/** 从平台文章引入 */
export function fetchFromArticle(toolCode: string, articleId: number): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: '/tools/article-rewrite/fetch-article',
    method: 'POST',
    data: { toolCode, articleId },
  })
}

/** 手动输入内容 */
export function inputManual(
  toolCode: string,
  title: string,
  source: string,
  content: string
): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: '/tools/article-rewrite/input-manual',
    method: 'POST',
    data: { toolCode, title, source, content },
  })
}

// ==================== 第二步：内容总结与价值判断 ====================

/** 生成内容简述与候选相关性标签 */
export function generateSummary(id: number): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/summary`,
    method: 'POST',
  })
}

/** 根据选定的相关性标签生成价值判断 */
export function judgeValue(id: number, selectedTags: string[]): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/summary/value`,
    method: 'POST',
    data: { selectedTags },
  })
}

/** 生成转写方案 */
export function generatePlans(id: number): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/summary/plans`,
    method: 'POST',
  })
}

/** 选择转写方案（单选） */
export function selectPlan(id: number, planId: string): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/plan/select`,
    method: 'POST',
    data: { planId },
  })
}

// ==================== 第三步：文章转写 ====================

/** 执行转写 */
export function rewrite(id: number): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/rewrite`,
    method: 'POST',
  })
}

/** 重新转写 */
export function regenerateRewrite(id: number): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/rewrite/regenerate`,
    method: 'POST',
  })
}

// ==================== 第四步：内容审核 ====================

/** 执行审核 */
export function audit(id: number): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/audit`,
    method: 'POST',
  })
}

/** 一键修复审核问题 */
export function fixAudit(id: number, itemIndexes: number[]): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/audit/fix`,
    method: 'POST',
    data: { itemIndexes },
  })
}

// ==================== 第五步：文章配图 ====================

/** 生成配图 */
export function generateImages(id: number): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/images/generate`,
    method: 'POST',
  })
}

/** 保存主图选择 */
export function saveMainImage(id: number, planId: string, imageId: string): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/images/select`,
    method: 'POST',
    data: { planId, imageId },
  })
}

// ==================== 第六步：自查与发布 ====================

/** 执行自查 */
export function selfCheck(id: number): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/self-check`,
    method: 'POST',
  })
}

/** 保存草稿（可含编辑内容） */
export function saveDraft(id: number, body?: { title?: string; summary?: string; body?: string }): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/save`,
    method: 'POST',
    data: body || {},
  })
}

/** 发布到渠道 */
export function publish(id: number, channel: PublishChannel): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}/publish`,
    method: 'POST',
    data: { channel },
  })
}

// ==================== 通用接口 ====================

/** 我的转写列表 */
export function getRewriteList(): Promise<ArticleRewriteListItem[]> {
  return request<ArticleRewriteListItem[]>({
    url: '/tools/article-rewrite/list',
    method: 'GET',
  })
}

/** 转写项目详情（恢复草稿） */
export function getRewriteDetail(id: number): Promise<ArticleRewriteRecord> {
  return request<ArticleRewriteRecord>({
    url: `/tools/article-rewrite/${id}`,
    method: 'GET',
  })
}

/** 删除转写项目 */
export function deleteRewrite(id: number): Promise<void> {
  return request<void>({
    url: `/tools/article-rewrite/${id}`,
    method: 'DELETE',
  })
}
