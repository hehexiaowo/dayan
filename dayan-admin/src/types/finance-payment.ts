/**
 * 支付记录（finance_payment）相关类型。
 *
 * 字段对齐后端 com.dayan.finance 包下：
 * - FinancePaymentVO / FinancePaymentQueryDTO。
 *
 * 类型约定：Integer→number，BigDecimal→number，LocalDateTime→string。
 *
 * 说明：支付方式 PayType 与 types/finance.ts（资金流水）语义一致
 * （1微信/2支付宝/3银行转账/4余额/5线下），这里直接复用。
 */
import type { PageQuery } from '@/types/common'
import type { PayType } from '@/types/finance'
// 复用流水的支付方式枚举与选项，避免重复定义
export { PayType, PAY_TYPE_OPTIONS } from '@/types/finance'

// ==================== 订单类型（退款/支付共用语义） ====================

/** 订单类型：1=权益 / 2=场景 / 3=课程 / 4=旅居 */
export enum OrderType {
  /** 权益 */
  EQUITY = 1,
  /** 场景 */
  SCENE = 2,
  /** 课程 */
  COURSE = 3,
  /** 旅居 */
  TRAVEL = 4
}

export const ORDER_TYPE_OPTIONS = [
  { label: '权益', value: OrderType.EQUITY },
  { label: '场景', value: OrderType.SCENE },
  { label: '课程', value: OrderType.COURSE },
  { label: '旅居', value: OrderType.TRAVEL }
] as const

// ==================== 支付状态 ====================

/**
 * 支付状态：0=待支付 / 1=支付成功 / 2=支付失败 / 3=已退款 / 4=部分退款
 */
export enum PayStatus {
  /** 待支付 */
  PENDING = 0,
  /** 支付成功 */
  SUCCESS = 1,
  /** 支付失败 */
  FAILED = 2,
  /** 已退款 */
  REFUNDED = 3,
  /** 部分退款 */
  PARTIAL_REFUND = 4
}

export const PAY_STATUS_OPTIONS = [
  { label: '待支付', value: PayStatus.PENDING },
  { label: '支付成功', value: PayStatus.SUCCESS },
  { label: '支付失败', value: PayStatus.FAILED },
  { label: '已退款', value: PayStatus.REFUNDED },
  { label: '部分退款', value: PayStatus.PARTIAL_REFUND }
] as const

/**
 * 支付记录实体（后端 FinancePaymentVO）。
 */
export interface FinancePayment {
  id?: number
  /** 支付流水号（系统生成 PAY+序号） */
  paymentCode?: string
  /** 订单类型：1=权益/2=场景/3=课程/4=旅居 */
  orderType?: OrderType
  /** 订单编号 */
  orderCode?: string
  /** 支付方式：1微信/2支付宝/3银行转账/4余额/5线下 */
  payType?: PayType
  /** 支付金额 */
  payAmount?: number
  /** 第三方交易号 */
  tradeNo?: string
  /** 付款方账号 */
  payerAccount?: string
  /** 收款方账号 */
  payeeAccount?: string
  /** 支付时间 */
  payTime?: string
  /** 回调通知时间 */
  notifyTime?: string
  /** 支付状态 */
  payStatus?: PayStatus
  /** 支付说明 */
  payDescription?: string
  /** 扩展数据（JSON 字符串） */
  extraData?: string
  createdAt?: string
}

/**
 * 支付记录分页查询参数（后端 FinancePaymentQueryDTO）。
 */
export interface FinancePaymentQuery extends PageQuery {
  paymentCode?: string
  orderType?: OrderType
  orderCode?: string
  /** 订单编码集合（多值 IN 过滤） */
  orderCodes?: string[]
  tradeNo?: string
  payType?: PayType
  payStatus?: PayStatus
  /** 支付时间 ≥ */
  payTimeFrom?: string
  /** 支付时间 ≤ */
  payTimeTo?: string
}
