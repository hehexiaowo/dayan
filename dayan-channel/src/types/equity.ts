/**
 * 权益相关类型。
 *
 * 字段对齐后端 Equity 域 Entity（com.dayan.equity.entity.Equity），
 * 渠道后台视角取本渠道权益子集。
 */

/**
 * 权益状态：对齐后端 EquityEvent 状态机（EQUITY_SM）。
 * 0=库存中 / 1=已出库 / 2=已激活 / 3=使用中 / 4=已完成 / 5=已过期 / 6=已作废 / 7=更换权益人中。
 */
export enum EquityStatus {
  /** 库存中 */
  STOCK = 0,
  /** 已出库 */
  OUTBOUND = 1,
  /** 已激活 */
  ACTIVATED = 2,
  /** 使用中 */
  IN_USE = 3,
  /** 已完成 */
  COMPLETED = 4,
  /** 已过期 */
  EXPIRED = 5,
  /** 已作废 */
  VOID = 6,
  /** 更换权益人中 */
  CHANGING_HOLDER = 7
}

/** 权益状态选项 */
export const EQUITY_STATUS_OPTIONS = [
  { label: '库存中', value: EquityStatus.STOCK },
  { label: '已出库', value: EquityStatus.OUTBOUND },
  { label: '已激活', value: EquityStatus.ACTIVATED },
  { label: '使用中', value: EquityStatus.IN_USE },
  { label: '已完成', value: EquityStatus.COMPLETED },
  { label: '已过期', value: EquityStatus.EXPIRED },
  { label: '已作废', value: EquityStatus.VOID },
  { label: '更换权益人中', value: EquityStatus.CHANGING_HOLDER }
] as const

/**
 * 权益实体（渠道视角子集，对齐 EquityDepotVO）。
 *
 * 注意：equityValue 单位是「元」（后端 DECIMAL(12,2)），前端直接显示，不要除以 100。
 */
export interface Equity {
  id?: number
  /** 权益编码（主键业务码） */
  equityCode?: string
  /** 权益状态（0-7，见 EquityStatus） */
  equityStatus?: EquityStatus
  /** 权益类型（int，对齐后端 equity_type） */
  equityType?: number
  /** 权益价值（单位：元，不要除以100） */
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
