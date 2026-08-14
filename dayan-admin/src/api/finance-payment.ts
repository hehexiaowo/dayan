import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { FinancePayment, FinancePaymentQuery } from '@/types/finance-payment'

/**
 * 支付记录（FinancePayment）接口封装。
 *
 * 对应后端 FinancePaymentAdminController（基路径 /admin-api/finance/payment）。
 *
 * 状态流转（pay_status）：
 * - create 创建：→ 0 待支付
 * - mark-success：0 → 1 支付成功（body：paymentCode + tradeNo + payTime/notifyTime）
 * - mark-failed：0 → 2 支付失败（body：paymentCode + 失败说明）
 *
 * 注意：mark-success / mark-failed 均走请求体（DTO 校验）。
 */

/** 支付分页：GET /admin-api/finance/payment/page */
export function pagePayments(query: FinancePaymentQuery): Promise<PageResult<FinancePayment>> {
  return request<PageResult<FinancePayment>>({
    url: '/admin-api/finance/payment/page',
    method: 'get',
    params: query
  })
}

/** 支付列表（全量）：GET /admin-api/finance/payment/list */
export function listPayments(query: FinancePaymentQuery): Promise<FinancePayment[]> {
  return request<FinancePayment[]>({
    url: '/admin-api/finance/payment/list',
    method: 'get',
    params: query
  })
}

/** 支付详情：GET /admin-api/finance/payment/{paymentCode} */
export function getPayment(paymentCode: string): Promise<FinancePayment> {
  return request<FinancePayment>({
    url: `/admin-api/finance/payment/${paymentCode}`,
    method: 'get'
  })
}

/**
 * 创建支付记录：POST /admin-api/finance/payment
 *
 * 对应后端 CreatePaymentDTO（@Valid 校验）。
 * paymentCode 由服务端生成（PAY+序号），pay_status=0（待支付）由服务端初始化。
 * 成功后返回新生成的 paymentCode。
 */
export function createPayment(data: {
  /** 订单类型：1=权益/2=场景/3=课程/4=旅居（NotNull） */
  orderType: number
  /** 订单编号（NotBlank） */
  orderCode: string
  /** 支付方式：1微信/2支付宝/3银行转账/4余额/5线下（NotNull） */
  payType: number
  /** 支付金额（可空，由订单权威解析） */
  payAmount?: number
  /** 付款方账号 */
  payerAccount?: string
  /** 收款方账号 */
  payeeAccount?: string
  /** 支付说明 */
  payDescription?: string
  /** 扩展数据（JSON 字符串） */
  extraData?: string
}): Promise<string> {
  return request<string>({
    url: '/admin-api/finance/payment',
    method: 'post',
    data
  })
}

/**
 * 标记支付成功（待支付→支付成功）：POST /admin-api/finance/payment/mark-success
 *
 * 对应后端 PaymentMarkSuccessDTO（@Valid 校验），状态 0 → 1。
 * 写 tradeNo + payTime + notifyTime。payTime/notifyTime 为空时服务端取当前时间。
 */
export function markPaymentSuccess(data: {
  /** 支付流水号（NotBlank） */
  paymentCode: string
  /** 第三方交易号（NotBlank） */
  tradeNo: string
  /** 支付时间（为空取当前时间） */
  payTime?: string
  /** 回调通知时间（为空取当前时间） */
  notifyTime?: string
  payerAccount?: string
  payeeAccount?: string
  /** 备注 */
  payDescription?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/payment/mark-success',
    method: 'post',
    data
  })
}

/**
 * 标记支付失败（待支付→支付失败）：POST /admin-api/finance/payment/mark-failed
 *
 * 对应后端 PaymentMarkFailedDTO，状态 0 → 2。payDescription 为失败原因。
 */
export function markPaymentFailed(data: {
  /** 支付流水号（NotBlank） */
  paymentCode: string
  /** 支付说明（失败原因） */
  payDescription?: string
  /** 回调通知时间 */
  notifyTime?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/payment/mark-failed',
    method: 'post',
    data
  })
}
