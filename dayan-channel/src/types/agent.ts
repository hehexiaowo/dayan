import type { PageQuery } from './common'

/**
 * 代理人相关类型。
 *
 * 字段对齐后端 Agent 域 Entity（com.dayan.agent.entity.Agent），
 * 渠道后台视角取本渠道代理人子集。
 */

/** 代理人等级：1 一级 / 2 二级 / 3 三级 / 4 四级 */
export enum AgentLevel {
  /** 一级代理人 */
  LEVEL_1 = 1,
  /** 二级代理人 */
  LEVEL_2 = 2,
  /** 三级代理人 */
  LEVEL_3 = 3,
  /** 四级代理人 */
  LEVEL_4 = 4
}

/** 代理人等级选项 */
export const AGENT_LEVEL_OPTIONS = [
  { label: '一级', value: AgentLevel.LEVEL_1 },
  { label: '二级', value: AgentLevel.LEVEL_2 },
  { label: '三级', value: AgentLevel.LEVEL_3 },
  { label: '四级', value: AgentLevel.LEVEL_4 }
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
export interface AgentQuery {
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
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
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
  lastLoginAt?: string
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

/** 分享类型选项 */
export const SHARE_TYPE_OPTIONS = [
  { value: 1, label: '内容分享' },
  { value: 2, label: '场景分享' },
  { value: 3, label: '权益分享' }
]
