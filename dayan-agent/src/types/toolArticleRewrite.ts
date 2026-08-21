/**
 * AI文章转写功能类型定义。
 */

/** 转写状态 */
export type RewriteStatus =
  | 'CREATED'
  | 'CONTENT_FETCHED'
  | 'SUMMARY_DONE'
  | 'PLANNED'
  | 'REWRITTEN'
  | 'AUDITED'
  | 'IMAGED'
  | 'READY'
  | 'PUBLISHED'

/** 状态标签 */
export const REWRITE_STATUS_LABELS: Record<RewriteStatus, string> = {
  CREATED: '待获取内容',
  CONTENT_FETCHED: '待判断',
  SUMMARY_DONE: '待确认方案',
  PLANNED: '待转写',
  REWRITTEN: '待审查',
  AUDITED: '待配图',
  IMAGED: '待完成',
  READY: '待完成',
  PUBLISHED: '已发布',
}

// ==================== 第一步：内容获取 ====================

/** 内容获取结果 */
export interface ContentFetch {
  sourceType: 'url' | 'manual' | 'article'
  sourceUrl?: string
  articleId?: number
  originalTitle?: string
  originalSource?: string
  originalPublishTime?: string
  originalContent?: string
  fetchTime?: string
  fetchStatus: 'success' | 'failed'
  fetchError?: string
}

// ==================== 第二步：总结与分析 ====================

/** 价值判断 */
export interface ValueJudgment {
  level: 'high' | 'medium' | 'low'
  reason: string
}

/** 相关性判断 */
export interface RelevanceJudgment {
  level: 'strong' | 'weak' | 'none'
  detail: string
}

/** 转写方案 */
export interface RewritePlan {
  planId: string
  name: string
  style: string
  channel: string
  wordCount: string
  reason: string
  angle: string
}

/** 总结与分析结果（分三步生成：内容简述→价值判断→转写方案） */
export interface SummaryAnalysis {
  /** 内容简述 */
  contentSummary?: string
  /** 候选相关性标签（内容简述生成时由AI提炼） */
  candidateTags?: string[]
  /** 用户选定的相关性标签 */
  selectedTags?: string[]
  /** 价值判断 */
  viralValue?: ValueJudgment
  /** 相关性判断 */
  relevance?: RelevanceJudgment
  /** 转写方案 */
  rewritePlans?: RewritePlan[]
  /** 选中的转写方案（单选） */
  selectedPlanIds?: string[]
  generateTime?: string
}

// ==================== 第三步：转写结果 ====================

/** 单个方案的转写结果 */
export interface RewriteResultItem {
  planId: string
  title: string
  body: string
  summary: string
  keywords: string[]
  channelAdaptation: string
  wordCount: number
  generateTime: string
}

/** 转写结果 */
export interface RewriteResult {
  results?: RewriteResultItem[]
  currentPlanId?: string
}

// ==================== 第四步：审核结果 ====================

/** 审核项 */
export interface AuditItem {
  dimension: 'ai_taste' | 'safety'
  item: string
  originalText: string
  description: string
  severity: 'warning' | 'error'
  suggestion: string
  fixedText?: string
  fixed: boolean
}

/** 单个方案的审核结果 */
export interface AuditResultItem {
  planId: string
  items?: AuditItem[]
  fixedContent?: string
  auditTime?: string
}

/** 审核结果 */
export interface AuditResult {
  results?: AuditResultItem[]
  currentPlanId?: string
}

// ==================== 第五步：配图结果 ====================

/** 图片候选 */
export interface ImageCandidate {
  imageId: string
  url: string
  selected: boolean
}

/** 文章内图 */
export interface BodyImage {
  imageId: string
  url: string
  position: number
  paragraph?: string
}

/** 主图 */
export interface MainImage {
  candidates?: ImageCandidate[]
  customUrl?: string
}

/** 单个方案的配图结果 */
export interface ImageResultItem {
  planId: string
  mainImage?: MainImage
  bodyImages?: BodyImage[]
  generateTime?: string
}

/** 配图结果 */
export interface ImageResult {
  results?: ImageResultItem[]
  currentPlanId?: string
}

// ==================== 第六步：发布信息 ====================

/** 自查项 */
export interface SelfCheckItem {
  item: string
  passed: boolean
  message: string
}

/** 发布渠道 */
export type PublishChannel = 'wechat' | 'xhs'

/** 发布状态 */
export type PublishStatus = 'draft' | 'publishing' | 'published' | 'failed'

/** 单个方案的发布信息 */
export interface PublishResultItem {
  planId: string
  selfCheck?: SelfCheckItem[]
  publishChannel?: PublishChannel
  publishUrl?: string
  publishStatus: PublishStatus
  publishTime?: string
  publishError?: string
}

/** 发布信息 */
export interface PublishInfo {
  results?: PublishResultItem[]
  currentPlanId?: string
  lastSaveTime?: string
}

// ==================== 完整记录 ====================

/** 文章转写记录 */
export interface ArticleRewriteRecord {
  id: number
  toolCode: string
  agentCode: string
  channelCode: string
  status: RewriteStatus
  contentFetch?: ContentFetch
  summaryAnalysis?: SummaryAnalysis
  rewriteResult?: RewriteResult
  auditResult?: AuditResult
  imageResult?: ImageResult
  publishInfo?: PublishInfo
  createdAt?: string
  updatedAt?: string
}

/** 文章转写列表项 */
export interface ArticleRewriteListItem {
  id: number
  toolCode: string
  status: RewriteStatus
  originalTitle?: string
  rewriteTitle?: string
  createdAt?: string
  updatedAt?: string
}

// ==================== 工具函数 ====================

/** 提取错误信息（兼容网络错误 e.message 为 undefined 的情况） */
export function getErrMsg(e: any, fallback: string): string {
  if (!e) return fallback
  if (typeof e === 'string') return e || fallback
  if (e.message && typeof e.message === 'string' && e.message.trim()) return e.message
  if (e.msg && typeof e.msg === 'string' && e.msg.trim()) return e.msg
  return fallback
}

/** 根据状态路由到对应步骤页 */
export function rewritePhaseStep(status: RewriteStatus): string {
  const base = '/pages/acquisition/tools/article-rewrite/'
  switch (status) {
    case 'CREATED':
      return base + 'step-content'
    case 'CONTENT_FETCHED':
      return base + 'step-summary'
    case 'SUMMARY_DONE':
      return base + 'step-plan'
    case 'PLANNED':
      return base + 'step-rewrite'
    case 'REWRITTEN':
      return base + 'step-audit'
    case 'AUDITED':
      return base + 'step-image'
    case 'IMAGED':
    case 'READY':
      return base + 'step-publish'
    case 'PUBLISHED':
      return base + 'step-publish'
    default:
      return base + 'step-content'
  }
}

/** 获取当前方案的结果（currentPlanId 为空时回退到第一个结果） */
export function getCurrentPlanResult<T extends { planId: string }>(
  results: T[] | undefined,
  currentPlanId: string | undefined
): T | undefined {
  if (!results || !results.length) return undefined
  if (!currentPlanId) return results[0]
  return results.find((r) => r.planId === currentPlanId) || results[0]
}
