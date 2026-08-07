import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  FinancePayment,
  FinancePaymentQuery,
  FinanceInvoice,
  FinanceInvoiceQuery,
  CreatePaymentData,
  PaymentMarkSuccessData,
  ApplyInvoiceData
} from '@/types/finance'

/**
 * 渠道端结算域接口封装。
 *
 * 对应后端：
 * - 支付单：`ChannelFinanceController`（/channel-api/finance-payments/*）
 * - 发票：`ChannelInvoiceController`（/channel-api/finance-invoices/*）
 *
 * 防越权：
 * - 支付单读接口：finance_payment 无 channel_code，后端靠反查本渠道 4 类订单
 *   的 orderCode 集合做归属过滤；详情按 orderType 路由校验。
 * - 发票读/申请接口：applicantCode/applicantType 由后端强制注入（= channelCode / "channel"）。
 * - 权益订单（orderType=1）创建支付单时，payAmount 由后端从订单表权威解析覆盖（防篡改）。
 */

// ==================== 支付单（finance-payments）====================

/** 支付单分页：GET /channel-api/finance-payments */
export function pageFinancePayments(
  query: FinancePaymentQuery
): Promise<PageResult<FinancePayment>> {
  return request<PageResult<FinancePayment>>({
    url: '/channel-api/finance-payments',
    method: 'get',
    params: query
  })
}

/** 支付单详情：GET /channel-api/finance-payments/{paymentCode} */
export function getFinancePayment(paymentCode: string): Promise<FinancePayment> {
  return request<FinancePayment>({
    url: `/channel-api/finance-payments/${paymentCode}`,
    method: 'get'
  })
}

/**
 * 创建支付单：POST /channel-api/finance-payments
 *
 * 对齐后端 `CreatePaymentDTO`。返回新生成的 paymentCode（PAY+序号）。
 * 权益订单（orderType=1）的 payAmount 由后端从订单表权威解析覆盖。
 */
export function createFinancePayment(data: CreatePaymentData): Promise<string> {
  return request<string>({
    url: '/channel-api/finance-payments',
    method: 'post',
    data
  })
}

/**
 * 标记支付成功：POST /channel-api/finance-payments/{paymentCode}/mark-success
 *
 * 对齐后端 `PaymentMarkSuccessDTO`。状态 0（待支付）→ 1（支付成功），
 * 写 tradeNo + payTime + notifyTime。paymentCode 走 path，tradeNo 必填。
 */
export function markFinancePaymentSuccess(
  paymentCode: string,
  data: PaymentMarkSuccessData
): Promise<void> {
  return request<void>({
    url: `/channel-api/finance-payments/${paymentCode}/mark-success`,
    method: 'post',
    data
  })
}

// ==================== 发票（finance-invoices）====================

/** 发票分页：GET /channel-api/finance-invoices */
export function pageFinanceInvoices(
  query: FinanceInvoiceQuery
): Promise<PageResult<FinanceInvoice>> {
  return request<PageResult<FinanceInvoice>>({
    url: '/channel-api/finance-invoices',
    method: 'get',
    params: query
  })
}

/** 发票详情：GET /channel-api/finance-invoices/{invoiceCode} */
export function getFinanceInvoice(invoiceCode: string): Promise<FinanceInvoice> {
  return request<FinanceInvoice>({
    url: `/channel-api/finance-invoices/${invoiceCode}`,
    method: 'get'
  })
}

/**
 * 申请发票：POST /channel-api/finance-invoices/apply
 *
 * 对齐后端 `ApplyInvoiceDTO`（@Valid 校验）。
 * - applicantCode / applicantType 由后端强制注入（= 当前 channelCode / "channel"），前端不传；
 * - invoiceCode / applyTime / issueTime / sendTime / invoiceStatus 由服务端管理；
 * - titleType 留空时后端默认 1（企业）；
 * - 电子发票（invoiceType=3）时 receiverEmail 必填；
 * - 企业（titleType=1）时 taxNo 必填。
 * 成功后返回新生成的 invoiceCode。
 */
export function applyFinanceInvoice(data: ApplyInvoiceData): Promise<string> {
  return request<string>({
    url: '/channel-api/finance-invoices/apply',
    method: 'post',
    data
  })
}
