/**
 * 权益相关类型。
 *
 * 字段对齐后端 Equity 域 Entity（com.dayan.equity.entity.Equity），
 * 渠道后台视角取本渠道权益子集。
 */

/**
 * 权益状态：0 待生效 / 1 生效中 / 2 已冻结 / 3 已失效 / 4 已退订 /
 *          5 已过期 / 6 已核销 / 7 已作废
 */
export enum EquityStatus {
  /** 待生效 */
  PENDING = 0,
  /** 生效中 */
  ACTIVE = 1,
  /** 已冻结 */
  FROZEN = 2,
  /** 已失效 */
  INVALID = 3,
  /** 已退订 */
  UNSUBSCRIBED = 4,
  /** 已过期 */
  EXPIRED = 5,
  /** 已核销 */
  VERIFIED = 6,
  /** 已作废 */
  REVOKED = 7
}

/** 权益状态选项 */
export const EQUITY_STATUS_OPTIONS = [
  { label: '待生效', value: EquityStatus.PENDING },
  { label: '生效中', value: EquityStatus.ACTIVE },
  { label: '已冻结', value: EquityStatus.FROZEN },
  { label: '已失效', value: EquityStatus.INVALID },
  { label: '已退订', value: EquityStatus.UNSUBSCRIBED },
  { label: '已过期', value: EquityStatus.EXPIRED },
  { label: '已核销', value: EquityStatus.VERIFIED },
  { label: '已作废', value: EquityStatus.REVOKED }
] as const

/**
 * 权益实体（渠道视角子集）。
 */
export interface Equity {
  id?: number
  /** 权益编码（主键业务码） */
  equityCode?: string
  /** 权益状态（0-7） */
  equityStatus?: EquityStatus
  /** 权益类型 */
  equityType?: string
  /** 权益价值（分） */
  equityValue?: number
  /** 到期时间（yyyy-MM-dd HH:mm:ss） */
  expireTime?: string
  /** 关联客户编码 */
  clientCode?: string
}

/** 权益分页查询参数 */
export interface EquityQuery {
  /** 权益编码（模糊匹配，可选） */
  equityCode?: string
  /** 权益状态（可选） */
  equityStatus?: EquityStatus
  /** 关联客户编码（模糊匹配，可选） */
  clientCode?: string
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
}
