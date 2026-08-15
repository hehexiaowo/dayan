/**
 * 退款记录（finance_refund）相关类型。
 *
 * 字段对齐后端 com.dayan.finance 包下：
 * - FinanceRefundVO / FinanceRefundQueryDTO。
 *
 * 类型约定：Integer→number，BigDecimal→number，LocalDateTime→string。
 */
import type { PageQuery } from '@/types/common'

// ==================== 订单类型（退款/支付共用语义） ====================

/** 订单类型：1=权益 / 2=场景 / 3=课程 / 4=旅游短居 */
export enum OrderType {
  /** 权益 */
  EQUITY = 1,
  /** 场景 */
  SCENE = 2,
  /** 课程 */
  COURSE = 3,
  /** 旅游短居 */
  TRAVEL = 4
}

export const ORDER_TYPE_OPTIONS = [
  { label: '权益', value: OrderType.EQUITY },
  { label: '场景', value: OrderType.SCENE },
  { label: '课程', value: OrderType.COURSE },
  { label: '旅游短居', value: OrderType.TRAVEL }
] as const

// ==================== 退款类型 ====================

/** 退款类型：1=全额退款 / 2=部分退款 */
export enum RefundType {
  /** 全额退款 */
  FULL = 1,
  /** 部分退款 */
  PARTIAL = 2
}

export const REFUND_TYPE_OPTIONS = [
  { label: '全额退款', value: RefundType.FULL },
  { label: '部分退款', value: RefundType.PARTIAL }
] as const

// ==================== 退款渠道 ====================

/** 退款渠道：1=原路退回 / 2=退到余额 / 3=线下退款 */
export enum RefundChannel {
  /** 原路退回 */
  ORIGINAL = 1,
  /** 退到余额 */
  BALANCE = 2,
  /** 线下退款 */
  OFFLINE = 3
}

export const REFUND_CHANNEL_OPTIONS = [
  { label: '原路退回', value: RefundChannel.ORIGINAL },
  { label: '退到余额', value: RefundChannel.BALANCE },
  { label: '线下退款', value: RefundChannel.OFFLINE }
] as const

// ==================== 退款状态 ====================

/**
 * 退款状态：0=待审核 / 1=审核通过 / 2=退款中 / 3=退款成功 / 4=审核拒绝 / 5=退款失败
 */
export enum RefundStatus {
  /** 待审核 */
  PENDING_AUDIT = 0,
  /** 审核通过 */
  AUDITED = 1,
  /** 退款中 */
  REFUNDING = 2,
  /** 退款成功 */
  SUCCESS = 3,
  /** 审核拒绝 */
  AUDIT_REJECT = 4,
  /** 退款失败 */
  FAILED = 5
}

export const REFUND_STATUS_OPTIONS = [
  { label: '待审核', value: RefundStatus.PENDING_AUDIT },
  { label: '审核通过', value: RefundStatus.AUDITED },
  { label: '退款中', value: RefundStatus.REFUNDING },
  { label: '退款成功', value: RefundStatus.SUCCESS },
  { label: '审核拒绝', value: RefundStatus.AUDIT_REJECT },
  { label: '退款失败', value: RefundStatus.FAILED }
] as const

/**
 * 退款记录实体（后端 FinanceRefundVO）。
 */
export interface FinanceRefund {
  id?: number
  /** 退款编号（系统生成 RF+序号） */
  refundCode?: string
  /** 订单类型：1=权益/2=场景/3=课程/4=旅游短居 */
  orderType?: OrderType
  /** 订单编号 */
  orderCode?: string
  /** 原支付记录编码 */
  paymentCode?: string
  /** 退款金额 */
  refundAmount?: number
  /** 退款原因 */
  refundReason?: string
  /** 退款类型：1=全额退款/2=部分退款 */
  refundType?: RefundType
  /** 退款渠道：1=原路退回/2=退到余额/3=线下退款 */
  refundChannel?: RefundChannel
  /** 退款交易号 */
  refundTradeNo?: string
  /** 申请时间 */
  applyTime?: string
  /** 审核时间 */
  auditTime?: string
  /** 退款完成时间 */
  refundTime?: string
  /** 审核人编码 */
  auditorCode?: string
  /** 审核人姓名 */
  auditorName?: string
  /** 审核备注 */
  auditRemark?: string
  /** 状态 */
  refundStatus?: RefundStatus
  remark?: string
  createdAt?: string
}

/**
 * 退款记录分页查询参数（后端 FinanceRefundQueryDTO）。
 */
export interface FinanceRefundQuery extends PageQuery {
  refundCode?: string
  orderType?: OrderType
  orderCode?: string
  paymentCode?: string
  refundType?: RefundType
  refundChannel?: RefundChannel
  refundStatus?: RefundStatus
  /** 申请时间 ≥ */
  applyTimeFrom?: string
  /** 申请时间 ≤ */
  applyTimeTo?: string
}
