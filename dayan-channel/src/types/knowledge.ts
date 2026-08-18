/**
 * 知识仓库类型（Channel 端本渠道知识库管理）。
 *
 * 对齐后端 ChannelKnowledgeController（/channel-api/knowledge/repos）。
 * 从 admin 端 src/types/knowledge.ts 精简：渠道端仅操作本渠道一个仓库，
 * 无平台/渠道归属切换，故去掉 repoType/channelCode/channelName 等归属字段。
 */

/** 仓库状态选项 */
export const KNOWLEDGE_REPO_STATUS_OPTIONS = [
  { value: 0, label: '构建中' },
  { value: 1, label: '正常' },
  { value: 2, label: '异常' }
] as const

export function knowledgeRepoStatusLabel(v?: number): string {
  return v === 1 ? '正常' : v === 2 ? '异常' : '构建中'
}

export function knowledgeRepoStatusTagType(v?: number): 'success' | 'danger' | 'warning' | 'info' {
  return v === 1 ? 'success' : v === 2 ? 'danger' : 'warning'
}

/** 文档解析状态标签 */
export function parseStatusLabel(v?: string): string {
  switch (v) {
    case 'INIT':
      return '待解析'
    case 'PARSING':
      return '解析中'
    case 'PARSE_SUCCESS':
      return '解析成功'
    case 'PARSE_FAILED':
      return '解析失败'
    default:
      return v || '--'
  }
}

/** 索引内文档状态标签 */
export function indexStatusLabel(v?: string): string {
  switch (v) {
    case 'FINISH':
      return '已入库'
    case 'RUNNING':
      return '入库中'
    case 'INSERT_ERROR':
      return '入库失败'
    case 'DELETED':
      return '已删除'
    default:
      return v || '--'
  }
}

export function indexStatusTagType(v?: string): 'success' | 'danger' | 'warning' | 'info' {
  if (v === 'FINISH') return 'success'
  if (v === 'INSERT_ERROR') return 'danger'
  if (v === 'RUNNING') return 'warning'
  return 'info'
}

/** 知识仓库实体（本渠道仓库，字段对齐 KnowledgeRepoVO） */
export interface KnowledgeRepo {
  id?: number
  repoCode: string
  repoName: string
  /** 百炼远端索引 ID */
  indexId?: string
  buildJobId?: string
  description?: string
  docCount?: number
  /** 0=构建中 1=正常 2=远端异常 */
  status?: number
  lastSyncAt?: string
  sortOrder?: number
  createdAt?: string
  updatedAt?: string
}

/** 知识仓库树节点（渠道树 + 每节点知识库归属；对齐 KnowledgeRepoTreeNodeVO） */
export interface KnowledgeRepoTreeNode {
  channelCode: string
  fullName: string
  shortName?: string
  level?: number
  /** 本渠道独立配置的知识仓库（无则为 null） */
  repo: KnowledgeRepo | null
  /** 实际可用仓库（独立库或沿祖先链继承的最近仓库；无则为 null） */
  effectiveRepo: KnowledgeRepo | null
  /** 继承来源渠道编码（独立配置或未继承时为 null） */
  inheritedFrom?: string | null
  /** 继承来源渠道简称 */
  inheritedFromName?: string | null
  /** 子节点（后代渠道） */
  children: KnowledgeRepoTreeNode[]
}

/** 知识库文档（远端代理） */
export interface KnowledgeDoc {
  fileId: string
  fileName: string
  /** INIT/PARSING/PARSE_SUCCESS/PARSE_FAILED */
  parseStatus?: string
  /** INSERT_ERROR/RUNNING/FINISH/DELETED */
  indexStatus?: string
  sizeInBytes?: number
  gmtModified?: number
  documentType?: string
}

/** RAG 问答结果 */
export interface KnowledgeChatResult {
  answer: string
  citations: {
    text: string
    score?: number
  }[]
}

/** 文档切片（对齐 BailianKnowledgeClient.ChunkItem） */
export interface KnowledgeChunk {
  text: string
  score?: number
  metadata?: unknown
}
