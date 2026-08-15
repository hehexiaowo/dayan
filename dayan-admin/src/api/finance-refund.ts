import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { FinanceRefund, FinanceRefundQuery } from '@/types/finance-refund'

/**
 * 退款记录（FinanceRefund）接口封装。
 *
 * 对应后端 FinanceRefundAdminController（基路径 /admin-api/finance/refund）。
 *
 * 状态流转（refund_status，状态机驱动）：
 * - apply 创建：→ 0 待审核
 * - audit：0 → 1 审核通过 / 0 → 4 审核拒绝
 * - mark-refunding：1 → 2 退款中（仅 path 传 refundCode）
 * - mark-success：2 → 3 退款成功（body：refundCode + refundTradeNo，终态）
 * - mark-failed：2 → 5 退款失败（body：refundCode + 备注，终态）
 *
 * 注意：mark-success / mark-failed 走请求体（DTO 校验），mark-refunding 走 path。
 */

/** 退款分页：GET /admin-api/finance/refund/page */
export function pageRefunds(query: FinanceRefundQuery): Promise<PageResult<FinanceRefund>> {
  return request<PageResult<FinanceRefund>>({
    url: '/admin-api/finance/refund/page',
    method: 'get',
    params: query
  })
}

/** 退款列表（全量）：GET /admin-api/finance/refund/list */
export function listRefunds(query: FinanceRefundQuery): Promise<FinanceRefund[]> {
  return request<FinanceRefund[]>({
    url: '/admin-api/finance/refund/list',
    method: 'get',
    params: query
  })
}

/** 退款详情：GET /admin-api/finance/refund/{refundCode} */
export function getRefund(refundCode: string): Promise<FinanceRefund> {
  return request<FinanceRefund>({
    url: `/admin-api/finance/refund/${refundCode}`,
    method: 'get'
  })
}

/**
 * 申请退款：POST /admin-api/finance/refund/apply
 *
 * 对应后端 ApplyRefundDTO（@Valid 校验）。
 * refundCode 由服务端生成（RF+序号），refund_status=0、apply_time=now 由服务端写入。
 * 成功后返回新生成的 refundCode。
 */
export function applyRefund(data: {
  /** 订单类型：1=权益/2=场景/3=课程/4=旅游短居（NotNull） */
  orderType: number
  /** 订单编号（NotBlank） */
  orderCode: string
  /** 原支付记录编码（可空，线下退款可无） */
  paymentCode?: string
  /** 退款金额（NotNull） */
  refundAmount: number
  /** 退款原因（NotBlank） */
  refundReason: string
  /** 退款类型：1=全额退款/2=部分退款（默认 1） */
  refundType?: number
  /** 退款渠道：1=原路退回/2=退到余额/3=线下退款（默认 1） */
  refundChannel?: number
  remark?: string
}): Promise<string> {
  return request<string>({
    url: '/admin-api/finance/refund/apply',
    method: 'post',
    data
  })
}

/**
 * 审核退款（待审核→审核通过/审核拒绝）：POST /admin-api/finance/refund/audit
 *
 * 对应后端 RefundAuditDTO，pass=true→状态1(通过)，pass=false→状态4(拒绝)。
 */
export function auditRefund(data: {
  /** 退款编码（NotBlank） */
  refundCode: string
  /** 是否通过：true=通过(→1)/false=拒绝(→4)（NotNull） */
  pass: boolean
  auditorCode?: string
  auditorName?: string
  auditRemark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/refund/audit',
    method: 'post',
    data
  })
}

/**
 * 进入退款中（审核通过→退款中）：POST /admin-api/finance/refund/mark-refunding/{refundCode}
 *
 * refundCode 走 path，状态 1 → 2。
 */
export function markRefunding(refundCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/finance/refund/mark-refunding/${refundCode}`,
    method: 'post'
  })
}

/**
 * 标记退款成功（退款中→退款成功）：POST /admin-api/finance/refund/mark-success
 *
 * 对应后端 RefundMarkSuccessDTO（@Valid 校验），状态 2 → 3（终态）。
 * 写 refundTradeNo + refundTime。refundTime 为空时服务端取当前时间。
 */
export function markRefundSuccess(data: {
  /** 退款编码（NotBlank） */
  refundCode: string
  /** 退款交易号（NotBlank） */
  refundTradeNo: string
  /** 退款完成时间（为空取当前时间） */
  refundTime?: string
  remark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/refund/mark-success',
    method: 'post',
    data
  })
}

/**
 * 标记退款失败（退款中→退款失败）：POST /admin-api/finance/refund/mark-failed
 *
 * 对应后端 RefundMarkFailedDTO，状态 2 → 5（终态）。备注为失败原因。
 */
export function markRefundFailed(data: {
  /** 退款编码（NotBlank） */
  refundCode: string
  /** 备注（失败原因） */
  remark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/refund/mark-failed',
    method: 'post',
    data
  })
}
