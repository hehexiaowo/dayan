import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { FinanceBill, FinanceBillQuery } from '@/types/finance'

/**
 * 结算单（FinanceBill）接口封装。
 *
 * 对应后端 FinanceBillAdminController（基路径 /admin-api/finance/bill）。
 *
 * 状态流转（billStatus）：
 * - 0 待审核 → audit pass → 1 审核通过
 * - 0 待审核 → audit reject → 4 审核拒绝（终态）
 * - 1 审核通过 → start-settle → 2 结算中
 * - 2 结算中 → finish-settle → 3 已结算（终态）
 */

/** 结算单分页：GET /admin-api/finance/bill/page */
export function pageBills(query: FinanceBillQuery): Promise<PageResult<FinanceBill>> {
  return request<PageResult<FinanceBill>>({
    url: '/admin-api/finance/bill/page',
    method: 'get',
    params: query
  })
}

/** 结算单详情：GET /admin-api/finance/bill/{billCode} */
export function getBill(billCode: string): Promise<FinanceBill> {
  return request<FinanceBill>({
    url: `/admin-api/finance/bill/${billCode}`,
    method: 'get'
  })
}

/**
 * 生成结算单：POST /admin-api/finance/bill/generate
 *
 * 对应后端 GenerateBillDTO。billCode 由服务端生成，finalAmount 由服务端计算，
 * 前端表单不收集这两个字段。返回新生成的 billCode。
 */
export function generateBill(data: {
  billType: number
  targetType: string
  targetCode: string
  targetName: string
  periodStart: string
  periodEnd: string
  orderCount: number
  totalAmount: number
  commissionAmount?: number
  refundAmount?: number
  adjustAmount?: number
  /** 关联流水 ID 列表（后端 GenerateBillDTO.flowIds 为 List<Long>，传数字数组） */
  flowIds?: number[]
  settlementMethod?: number
  bankInfo?: string
  remark?: string
}): Promise<string> {
  return request<string>({
    url: '/admin-api/finance/bill/generate',
    method: 'post',
    data
  })
}

/**
 * 审核结算单（待审核→审核通过/审核拒绝）：POST /admin-api/finance/bill/audit
 *
 * 对应后端 BillAuditDTO。pass=true→状态1(通过)，pass=false→状态4(拒绝)。
 */
export function auditBill(data: {
  billCode: string
  pass: boolean
  auditorCode?: string
  auditorName?: string
  auditRemark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/bill/audit',
    method: 'post',
    data
  })
}

/**
 * 开始结算（审核通过→结算中）：POST /admin-api/finance/bill/start-settle/{billCode}
 *
 * billCode 走 path，状态 1 → 2。
 */
export function startSettleBill(billCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/finance/bill/start-settle/${billCode}`,
    method: 'post'
  })
}

/**
 * 完成结算（结算中→已结算）：POST /admin-api/finance/bill/finish-settle
 *
 * 对应后端 BillFinishSettleDTO。settleTime 为空时服务端取当前时间，状态 2 → 3。
 */
export function finishSettleBill(data: {
  billCode: string
  settleTime?: string
  remark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/bill/finish-settle',
    method: 'post',
    data
  })
}
