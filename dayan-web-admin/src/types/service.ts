/**
 * 服务域相关类型（管家信息 + 服务会话）。
 *
 * 字段对齐后端 com.dayan.butler.entity.ButlerInfo 及
 * com.dayan.service.entity.ServiceSession 及对应 QueryDTO。
 * - Integer → number
 * - LocalDateTime → string
 */
import type { PageQuery } from '@/types/common'

// ==================== 管家信息 ====================

/**
 * 管家等级（butler_info.butler_level）。
 *
 * 1=初级 / 2=中级 / 3=高级 / 4=专家。
 */
export const BUTLER_LEVEL_OPTIONS = [
  { label: '初级', value: 1 },
  { label: '中级', value: 2 },
  { label: '高级', value: 3 },
  { label: '专家', value: 4 }
] as const

/**
 * 管家信息实体（后端 ButlerInfoVO）。
 *
 * 仅 8 个业务字段，是最简单的标准 CRUD 实体。
 */
export interface ButlerInfo {
  id?: number
  /** 管家编码（服务端生成，编辑时只读） */
  butlerCode?: string
  /** 管家姓名（必填） */
  fullName: string
  /** 手机号 */
  phone?: string
  /** 头像 URL */
  avatar?: string
  /** 所属组织编码 */
  organCode?: string
  /** 管家等级：1初级/2中级/3高级/4专家 */
  butlerLevel?: number
  /** 状态：1启用 / 0禁用 */
  status?: number
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 管家信息分页查询参数（后端 ButlerInfoQueryDTO）。
 */
export interface ButlerInfoQuery extends PageQuery {
  butlerCode?: string
  fullName?: string
  phone?: string
  organCode?: string
  butlerLevel?: number
  status?: number
}

// ==================== 服务会话 ====================

/**
 * 服务会话状态（service_session.session_status）。
 *
 * 0=待受理 / 1=已受理 / 2=需求提交 / 3=方案确认 / 4=服务中 / 5=已完成 / 6=已取消。
 */
export enum SessionStatus {
  /** 待受理 */
  PENDING = 0,
  /** 已受理 */
  ACCEPTED = 1,
  /** 需求提交 */
  DEMAND_SUBMITTED = 2,
  /** 方案确认 */
  SOLUTION_CONFIRMED = 3,
  /** 服务中 */
  IN_SERVICE = 4,
  /** 已完成 */
  COMPLETED = 5,
  /** 已取消 */
  CANCELLED = 6
}

/** 服务会话状态选项 */
export const SESSION_STATUS_OPTIONS = [
  { label: '待受理', value: SessionStatus.PENDING },
  { label: '已受理', value: SessionStatus.ACCEPTED },
  { label: '需求提交', value: SessionStatus.DEMAND_SUBMITTED },
  { label: '方案确认', value: SessionStatus.SOLUTION_CONFIRMED },
  { label: '服务中', value: SessionStatus.IN_SERVICE },
  { label: '已完成', value: SessionStatus.COMPLETED },
  { label: '已取消', value: SessionStatus.CANCELLED }
] as const

/**
 * 服务类型（service_session.service_type）。
 *
 * 1=上门服务 / 2=电话咨询 / 3=远程协助 / 4=机构驻点 / 5=其他。
 */
export const SERVICE_TYPE_OPTIONS = [
  { label: '上门服务', value: 1 },
  { label: '电话咨询', value: 2 },
  { label: '远程协助', value: 3 },
  { label: '机构驻点', value: 4 },
  { label: '其他', value: 5 }
] as const

/**
 * 服务会话实体（后端 ServiceSessionVO）。
 */
export interface ServiceSession {
  id?: number
  /** 会话编码（主键） */
  sessionCode?: string
  /** 关联权益编码 */
  equityCode?: string
  /** 客户编码 */
  clientCode?: string
  /** 服务管家编码 */
  butlerCode?: string
  /** 服务管家姓名（快照） */
  butlerFullName?: string
  /** 服务类型 */
  serviceType?: number
  /** 服务标题 */
  serviceTitle?: string
  /** 服务描述 */
  serviceDescription?: string
  /** 优先级 */
  priority?: number
  /** 来源类型 */
  sourceType?: number
  /** 来源编码 */
  sourceCode?: string
  /** 关联养老机构编码 */
  parkCode?: string
  /** 关联养老机构名称（快照） */
  parkFullName?: string
  /** 关联代理人编码 */
  agentCode?: string
  /** 关联渠道编码 */
  channelCode?: string
  /** 受理时间 */
  acceptTime?: string
  /** 完成时间 */
  completeTime?: string
  /** 关闭时间 */
  closeTime?: string
  /** 总服务时长（小时） */
  totalDuration?: number
  /** 服务接触次数 */
  touchCount?: number
  /** 是否满意 */
  isSatisfied?: number
  /** 综合评分 */
  overallRating?: number
  /** 会话状态 */
  sessionStatus?: SessionStatus
  /** 子状态 */
  subStatus?: string
  /** 关闭原因 */
  closeReason?: string
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 服务会话分页查询参数（后端 ServiceSessionQueryDTO）。
 */
export interface ServiceSessionQuery extends PageQuery {
  sessionCode?: string
  equityCode?: string
  clientCode?: string
  butlerCode?: string
  serviceType?: number
  parkCode?: string
  agentCode?: string
  channelCode?: string
  sessionStatus?: SessionStatus
  subStatus?: string
  sourceType?: number
}
