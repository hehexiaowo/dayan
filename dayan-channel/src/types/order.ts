/**
 * 订单相关类型。
 *
 * 字段对齐后端 Order 域 Entity（com.dayan.order.entity.Order），
 * 渠道后台视角取本渠道订单子集。
 */

/**
 * 订单状态：0 待支付 / 1 已支付 / 2 待发货 / 3 已发货 / 4 已完成 /
 *          5 已取消 / 6 已退款 / 7 已关闭
 */
export enum OrderStatus {
  /** 待支付 */
  PENDING_PAY = 0,
  /** 已支付 */
  PAID = 1,
  /** 待发货 */
  PENDING_SHIP = 2,
  /** 已发货 */
  SHIPPED = 3,
  /** 已完成 */
  COMPLETED = 4,
  /** 已取消 */
  CANCELLED = 5,
  /** 已退款 */
  REFUNDED = 6,
  /** 已关闭 */
  CLOSED = 7
}

/** 订单状态选项 */
export const ORDER_STATUS_OPTIONS = [
  { label: '待支付', value: OrderStatus.PENDING_PAY },
  { label: '已支付', value: OrderStatus.PAID },
  { label: '待发货', value: OrderStatus.PENDING_SHIP },
  { label: '已发货', value: OrderStatus.SHIPPED },
  { label: '已完成', value: OrderStatus.COMPLETED },
  { label: '已取消', value: OrderStatus.CANCELLED },
  { label: '已退款', value: OrderStatus.REFUNDED },
  { label: '已关闭', value: OrderStatus.CLOSED }
] as const

/**
 * 订单实体（渠道视角子集）。
 */
export interface Order {
  id?: number
  /** 订单编码（主键业务码） */
  orderCode?: string
  /** 订单状态（0-7） */
  orderStatus?: OrderStatus
  /** 实付金额（分） */
  payAmount?: number
  /** 订单总金额（分） */
  totalAmount?: number
  /** 订单类型 */
  orderType?: string
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createdAt?: string
  /** 关联客户编码 */
  clientCode?: string
}

/** 订单分页查询参数 */
export interface OrderQuery {
  /** 订单编码（模糊匹配，可选） */
  orderCode?: string
  /** 订单状态（可选） */
  orderStatus?: OrderStatus
  /** 关联客户编码（模糊匹配，可选） */
  clientCode?: string
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
}
