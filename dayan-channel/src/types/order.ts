/**
 * 订单相关类型。
 *
 * 字段对齐后端 Order 域 Entity（com.dayan.order.entity.Order），
 * 渠道后台视角取本渠道订单子集。
 */

/**
 * 订单状态：对齐后端 OrderEvent 状态机（ORDER_SM）。
 * 0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款。
 */
export enum OrderStatus {
  /** 待支付 */
  PENDING_PAY = 0,
  /** 已支付 */
  PAID = 1,
  /** 部分发放 */
  PARTIAL_DELIVERED = 2,
  /** 已发放 */
  DELIVERED = 3,
  /** 已完成 */
  COMPLETED = 4,
  /** 已取消 */
  CANCELLED = 5,
  /** 退款中 */
  REFUNDING = 6,
  /** 已退款 */
  REFUNDED = 7
}

/** 订单状态选项 */
export const ORDER_STATUS_OPTIONS = [
  { label: '待支付', value: OrderStatus.PENDING_PAY },
  { label: '已支付', value: OrderStatus.PAID },
  { label: '部分发放', value: OrderStatus.PARTIAL_DELIVERED },
  { label: '已发放', value: OrderStatus.DELIVERED },
  { label: '已完成', value: OrderStatus.COMPLETED },
  { label: '已取消', value: OrderStatus.CANCELLED },
  { label: '退款中', value: OrderStatus.REFUNDING },
  { label: '已退款', value: OrderStatus.REFUNDED }
] as const

/**
 * 订单实体（渠道视角子集，对齐 OrderEquityVO）。
 *
 * 注意：金额单位是「元」（后端 DECIMAL），前端直接显示，不要除以 100。
 * OrderEquity 无 clientCode/orderType 字段，已移除。
 */
export interface Order {
  id?: number
  /** 订单编码（主键业务码） */
  orderCode?: string
  /** 订单状态（0-7，见 OrderStatus） */
  orderStatus?: OrderStatus
  /** 实付金额（单位：元，不要除以100） */
  payAmount?: number
  /** 订单总金额（单位：元，不要除以100） */
  totalAmount?: number
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createdAt?: string
}

/** 订单分页查询参数（对齐 OrderEquityQueryDTO） */
export interface OrderQuery {
  /** 订单编码（模糊匹配，可选） */
  orderCode?: string
  /** 订单状态（可选） */
  orderStatus?: OrderStatus
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
}
