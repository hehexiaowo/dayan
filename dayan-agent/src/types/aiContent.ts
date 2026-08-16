/**
 * AI 内容创作（agent 端）类型定义。
 */

/** 当前渠道可见知识仓库（平台库 + 本渠道库） */
export interface KnowledgeRepoOption {
  id: number
  repoCode: string
  repoName: string
  /** 1=平台 2=渠道 */
  repoType?: number
  channelCode?: string
  /** 百炼远端索引 ID（空 = 未建库） */
  indexId?: string
  docCount?: number
  /** 0=构建中 1=正常 2=远端异常 */
  status?: number
}

/** 知识库文档（勾选用） */
export interface KnowledgeDocOption {
  fileId: string
  fileName: string
  indexStatus?: string
  parseStatus?: string
  /** 来源仓库（前端标注库名） */
  repoId?: number
  repoName?: string
}

/** AI 生成结果（预览用） */
export interface AiGenerateResult {
  title: string
  summary?: string
  contentBody: string
  contentType: number
  warnings?: string[]
  /** 备选标题（模型顺带给出的候选，可切换） */
  alternativeTitles?: string[]
  /** 本次写作引用的知识库片段（核对事实出处） */
  sources?: AiMaterialSource[]
}

/** 知识库引用片段 */
export interface AiMaterialSource {
  repoName: string
  text: string
}

/** 我的内容（agent_content） */
export interface AiContent {
  id: number
  agentCode?: string
  channelCode?: string
  title: string
  summary?: string
  coverImage?: string
  /** 1=图文 2=朋友圈 3=视频脚本 4=小红书笔记 */
  contentType: number
  contentBody: string
  styleCode?: string
  /** 创作目的（product/park/science，AI 创作流水线来源内容携带） */
  purpose?: string
  refContentCode?: string
  refKbFiles?: string
  refGoodsCodes?: string
  /** 目标读者（children/elder/general） */
  audience?: string
  status?: number
  createdAt?: string
  updatedAt?: string
}

/** 创作目的中文标签（内容中心列表/详情展示用） */
export const AI_PURPOSE_TAG: Record<string, string> = {
  product: '产品宣传',
  park: '机构推荐',
  science: '科普获客'
}

export const AI_CONTENT_TYPE_OPTIONS = [
  { value: 1, label: '图文文章', desc: '600-1200 字，含小标题与推荐，适合长文分享' },
  { value: 2, label: '朋友圈文案', desc: '200 字以内短文案，适合直接转发' },
  { value: 3, label: '视频脚本', desc: '60-90 秒口播脚本，含画面/口播/字幕' },
  { value: 4, label: '小红书笔记', desc: '600-800 字，Emoji 列表 + 标签，适合种草' }
] as const

export const AI_STYLE_OPTIONS = [
  { value: 'professional', label: '专业科普', desc: '严谨数据化，面向家庭决策者' },
  { value: 'warm', label: '温情软文', desc: '生活场景切入，情感共鸣' },
  { value: 'authoritative', label: '权威数据', desc: '结论先行，塑造专业可信' },
  { value: 'colloquial', label: '口语化', desc: '短句亲切，适合朋友圈' }
] as const

export function aiContentTypeLabel(type?: number): string {
  return AI_CONTENT_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? '内容'
}

export function aiStyleLabel(style?: string): string {
  return AI_STYLE_OPTIONS.find((o) => o.value === style)?.label ?? ''
}

/** 内置范文模板选项（GET /ai/templates） */
export interface AiRefTemplateOption {
  code: string
  name: string
  desc: string
  excerpt: string
}

export const AI_AUDIENCE_OPTIONS = [
  { value: 'general', label: '通用人群', desc: '客户与子女都易读' },
  { value: 'children', label: '子女决策者', desc: '理性数据，家庭责任视角' },
  { value: 'elder', label: '老人本人', desc: '直白温暖，自身利益视角' }
] as const
