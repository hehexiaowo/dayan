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
