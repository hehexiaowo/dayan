import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { FinanceInvoice, FinanceInvoiceQuery } from '@/types/finance'

/**
 * 发票（FinanceInvoice）接口封装。
 *
 * 对应后端 FinanceInvoiceAdminController（基路径 /admin-api/finance/invoice）。
 *
 * 状态流转（invoiceStatus，7 态状态机）：
 * - 0 待审核 → audit → 1 已审核
 * - 1 已审核 → issue（填发票号码）→ 2 已开票
 * - 2 已开票 → send → 3 已寄出
 * - 3 已寄出 → finish → 4 已完成（终态）
 * - 任意状态 → void → 5 已作废（终态）
 * - 已开票/之后 → red-flush → 6 已红冲（终态）
 *
 * 注意：apply 表单不含 invoiceCode（服务端生成），
 * 也不含 applyTime/issueTime/sendTime/invoiceStatus（服务端管理）。
 */

/** 发票分页：GET /admin-api/finance/invoice/page */
export function pageInvoices(query: FinanceInvoiceQuery): Promise<PageResult<FinanceInvoice>> {
  return request<PageResult<FinanceInvoice>>({
    url: '/admin-api/finance/invoice/page',
    method: 'get',
    params: query
  })
}

/** 发票详情：GET /admin-api/finance/invoice/{invoiceCode} */
export function getInvoice(invoiceCode: string): Promise<FinanceInvoice> {
  return request<FinanceInvoice>({
    url: `/admin-api/finance/invoice/${invoiceCode}`,
    method: 'get'
  })
}

/**
 * 申请开票：POST /admin-api/finance/invoice/apply
 *
 * 对应后端 ApplyInvoiceDTO（@Valid 校验）。
 * - invoiceCode 由服务端生成；
 * - applyTime/issueTime/sendTime/invoiceStatus 由服务端管理；
 * - titleType 留空时后端默认 1（企业）；
 * - 电子发票（invoiceType=3）时 receiverEmail 必填；
 * - 企业（titleType=1）时 taxNo 必填。
 * 成功后返回新生成的 invoiceCode。
 */
export function applyInvoice(data: {
  /** 发票类型：1普票/2专票/3电子（NotNull） */
  invoiceType: number
  billCode?: string
  orderCode?: string
  /** 申请方类型：channel/agent/client（NotBlank） */
  applicantType: string
  /** 申请方编码（NotBlank） */
  applicantCode: string
  /** 申请方名称（NotBlank） */
  applicantName: string
  /** 抬头类型：1企业/2个人，默认 1 */
  titleType?: number
  /** 发票抬头（NotBlank） */
  invoiceTitle: string
  /** 纳税人识别号（企业必填） */
  taxNo?: string
  bankName?: string
  bankAccount?: string
  registerAddress?: string
  registerPhone?: string
  /** 开票金额（NotNull） */
  invoiceAmount: number
  /** 发票内容（NotBlank） */
  invoiceContent: string
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  /** 收件邮箱（电子发票时必填） */
  receiverEmail?: string
  remark?: string
}): Promise<string> {
  return request<string>({
    url: '/admin-api/finance/invoice/apply',
    method: 'post',
    data
  })
}

/**
 * 审核发票（待审核 → 已审核）：POST /admin-api/finance/invoice/audit
 *
 * 对应后端 InvoiceAuditDTO，状态 0 → 1。
 */
export function auditInvoice(data: { invoiceCode: string; remark?: string }): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/invoice/audit',
    method: 'post',
    data
  })
}

/**
 * 开票（已审核 → 已开票）：POST /admin-api/finance/invoice/issue
 *
 * 对应后端 InvoiceIssueDTO，状态 1 → 2。
 * invoiceNo 为税务发票号码（非系统编码），必填。
 */
export function issueInvoice(data: {
  invoiceCode: string
  /** 发票号码（税务号码，NotBlank） */
  invoiceNo: string
  /** 发票文件 URL */
  invoiceUrl?: string
  remark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/invoice/issue',
    method: 'post',
    data
  })
}

/**
 * 寄出（已开票 → 已寄出）：POST /admin-api/finance/invoice/send
 *
 * 对应后端 InvoiceSendDTO，状态 2 → 3。
 */
export function sendInvoice(data: { invoiceCode: string; remark?: string }): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/invoice/send',
    method: 'post',
    data
  })
}

/**
 * 完成（已寄出 → 已完成）：POST /admin-api/finance/invoice/finish
 *
 * 对应后端 InvoiceOperateDTO，状态 3 → 4（终态）。
 */
export function finishInvoice(data: { invoiceCode: string; remark?: string }): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/invoice/finish',
    method: 'post',
    data
  })
}

/**
 * 作废（任意状态 → 已作废）：POST /admin-api/finance/invoice/void
 *
 * 对应后端 InvoiceOperateDTO，状态 → 5（终态）。
 */
export function voidInvoice(data: { invoiceCode: string; remark?: string }): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/invoice/void',
    method: 'post',
    data
  })
}

/**
 * 红冲（已开票/之后 → 已红冲）：POST /admin-api/finance/invoice/red-flush
 *
 * 对应后端 InvoiceOperateDTO，状态 → 6（终态）。
 */
export function redFlushInvoice(data: { invoiceCode: string; remark?: string }): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/invoice/red-flush',
    method: 'post',
    data
  })
}
