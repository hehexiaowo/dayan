import type { PageQuery } from './common'

/**
 * 代理人相关类型。
 *
 * 字段对齐后端 Agent 域 Entity（com.dayan.agent.entity.Agent），
 * 渠道后台视角取本渠道代理人子集。
 */

/** 代理人等级（DB 现有注释权威）：1 普通 / 2 银牌 / 3 金牌 / 4 钻石 */
export enum AgentLevel {
  /** 普通代理人 */
  LEVEL_1 = 1,
  /** 银牌代理人 */
  LEVEL_2 = 2,
  /** 金牌代理人 */
  LEVEL_3 = 3,
  /** 钻石代理人 */
  LEVEL_4 = 4
}

/** 代理人等级选项 */
export const AGENT_LEVEL_OPTIONS = [
  { label: '普通', value: AgentLevel.LEVEL_1 },
  { label: '银牌', value: AgentLevel.LEVEL_2 },
  { label: '金牌', value: AgentLevel.LEVEL_3 },
  { label: '钻石', value: AgentLevel.LEVEL_4 }
] as const

/** 代理人状态：1 启用 / 0 禁用 */
export enum AgentStatus {
  /** 启用 */
  ENABLED = 1,
  /** 禁用 */
  DISABLED = 0
}

/** 代理人状态选项 */
export const AGENT_STATUS_OPTIONS = [
  { label: '启用', value: AgentStatus.ENABLED },
  { label: '禁用', value: AgentStatus.DISABLED }
] as const

/** 代理人实体（渠道视角子集，对齐 AgentInfoVO）。 */
export interface Agent {
  id?: number
  /** 代理人编码（主键业务码） */
  agentCode?: string
  /** 代理人全名（对齐后端 AgentInfoVO.fullName） */
  fullName?: string
  /** 手机号 */
  phone?: string
  /** 代理人等级（1-4） */
  agentLevel?: AgentLevel
  /** 代理人状态（1 启用 / 0 禁用，对齐后端 status 字段） */
  status?: AgentStatus
  /** 所属渠道编码 */
  channelCode?: string
}

/** 代理人分页查询参数 */
export interface AgentQuery extends PageQuery {
  /** 代理人编码（模糊匹配，可选） */
  agentCode?: string
  /** 代理人全名（模糊匹配，可选，对齐后端 fullName） */
  fullName?: string
  /** 手机号（模糊匹配，可选） */
  phone?: string
  /** 代理人等级（可选） */
  agentLevel?: AgentLevel
  /** 代理人状态（可选，对齐后端 status） */
  status?: AgentStatus
}

// ==================== 代理人账号 ====================

/** 代理人账号 */
export interface AgentAccount {
  id?: number
  agentCode: string
  channelCode?: string
  username?: string
  realName?: string
  phone?: string
  avatar?: string
  agentLevel?: number
  isCertified?: number
  accountStatus?: number
  lastLoginTime?: string
  createdAt?: string
  updatedAt?: string
}

/** 代理人账号查询 */
export interface AgentAccountQuery extends PageQuery {
  agentCode?: string
  keyword?: string
  accountStatus?: number
}

// ==================== 客户线索（代理人-客户绑定）====================

/** 客户线索 */
export interface AgentClientRel {
  id?: number
  agentCode: string
  clientCode: string
  clientName?: string
  clientPhone?: string
  bindType?: number
  bindTime?: string
  status?: number
}

/** 客户线索查询 */
export interface AgentClientRelQuery extends PageQuery {
  agentCode?: string
  clientCode?: string
  bindType?: number
  status?: number
}

/** 绑定类型选项 */
export const BIND_TYPE_OPTIONS = [
  { value: 1, label: '主动绑定' },
  { value: 2, label: '邀请绑定' }
]

// ==================== 分享记录 ====================

/** 分享记录 */
export interface ShareRecord {
  id?: number
  shareCode: string
  agentCode: string
  shareType?: number
  bizCode?: string
  shareChannel?: number
  clientCode?: string
  clientName?: string
  viewCount?: number
  shareTime?: string
}

/** 分享记录查询 */
export interface ShareRecordQuery extends PageQuery {
  agentCode?: string
  shareCode?: string
  shareType?: number
  clientCode?: string
}

/**
 * 分享类型（agent_share_record.share_type，与 DDL 对齐）：
 * 1=内容 / 2=场景 / 3=机构 / 4=权益 / 5=课程。
 */
export const SHARE_TYPE_OPTIONS = [
  { value: 1, label: '内容' },
  { value: 2, label: '场景' },
  { value: 3, label: '机构' },
  { value: 4, label: '权益' },
  { value: 5, label: '课程' }
]

/**
 * 分享渠道（agent_share_record.share_channel，与 DDL 对齐）：
 * 1=微信 / 2=朋友圈 / 3=复制链接 / 4=二维码 / 5=短信。
 */
export const SHARE_CHANNEL_OPTIONS = [
  { label: '微信', value: 1 },
  { label: '朋友圈', value: 2 },
  { label: '复制链接', value: 3 },
  { label: '二维码', value: 4 },
  { label: '短信', value: 5 }
] as const

/** 分享渠道标签文本。 */
export function shareChannelLabel(v?: number): string {
  const found = SHARE_CHANNEL_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}
