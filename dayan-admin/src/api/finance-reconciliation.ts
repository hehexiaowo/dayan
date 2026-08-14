import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { FinanceReconciliation, FinanceReconciliationQuery } from '@/types/finance-reconciliation'

/**
 * 对账记录（FinanceReconciliation）接口封装。
 *
 * 对应后端 FinanceReconciliationAdminController（基路径 /admin-api/finance/reconciliation）。
 *
 * 状态流转（status，状态机驱动）：
 * - create 创建：→ 0 对账中
 * - complete：0 → 1 已完成（无差异，仅 path 传 reconCode）
 * - submit-diff：0 → 2 待确认（body：reconCode + 差异明细）
 * - confirm：2 → 3 已确认（body：reconCode + 差异处理结果，终态）
 *
 * 注意：complete 走 path；submit-diff / confirm 走请求体。
 */

/** 对账分页：GET /admin-api/finance/reconciliation/page */
export function pageReconciliations(
  query: FinanceReconciliationQuery
): Promise<PageResult<FinanceReconciliation>> {
  return request<PageResult<FinanceReconciliation>>({
    url: '/admin-api/finance/reconciliation/page',
    method: 'get',
    params: query
  })
}

/** 对账列表（全量）：GET /admin-api/finance/reconciliation/list */
export function listReconciliations(
  query: FinanceReconciliationQuery
): Promise<FinanceReconciliation[]> {
  return request<FinanceReconciliation[]>({
    url: '/admin-api/finance/reconciliation/list',
    method: 'get',
    params: query
  })
}

/** 对账详情：GET /admin-api/finance/reconciliation/{reconCode} */
export function getReconciliation(reconCode: string): Promise<FinanceReconciliation> {
  return request<FinanceReconciliation>({
    url: `/admin-api/finance/reconciliation/${reconCode}`,
    method: 'get'
  })
}

/**
 * 创建对账记录：POST /admin-api/finance/reconciliation
 *
 * 对应后端 CreateReconciliationDTO（@Valid 校验）。
 * reconCode、reconTime 由服务端生成，status=0（对账中）由服务端初始化。
 * 成功后返回新生成的 reconCode。
 */
export function createReconciliation(data: {
  /** 对账类型：1=渠道对账/2=供应商对账（NotNull） */
  reconType: number
  /** 对账对象编码（NotBlank） */
  targetCode: string
  /** 对账对象名称（NotBlank） */
  targetName: string
  /** 对账周期开始（NotNull） */
  periodStart: string
  /** 对账周期结束（NotNull） */
  periodEnd: string
  /** 我方订单数（NotNull） */
  ourOrderCount: number
  /** 我方总金额（NotNull） */
  ourTotalAmount: number
  /** 对方订单数（可空） */
  theirOrderCount?: number
  /** 对方总金额（可空） */
  theirTotalAmount?: number
  /** 差异订单数（默认 0） */
  diffCount?: number
  /** 差异金额（默认 0） */
  diffAmount?: number
  /** 差异明细（JSON 字符串） */
  diffDetail?: string
  /** 对账结果：0=有差异/1=一致（默认 0） */
  reconResult?: number
  /** 操作人编码（NotBlank） */
  operatorCode: string
  /** 操作人姓名 */
  operatorName?: string
  /** 对账时间（为空取当前时间） */
  reconTime?: string
  remark?: string
}): Promise<string> {
  return request<string>({
    url: '/admin-api/finance/reconciliation',
    method: 'post',
    data
  })
}

/**
 * 完成对账（对账中→已完成，无差异）：POST /admin-api/finance/reconciliation/complete/{reconCode}
 *
 * reconCode 走 path，状态 0 → 1。
 */
export function completeReconciliation(reconCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/finance/reconciliation/complete/${reconCode}`,
    method: 'post'
  })
}

/**
 * 提交差异（对账中→待确认）：POST /admin-api/finance/reconciliation/submit-diff
 *
 * 对应后端 ReconciliationSubmitDiffDTO，状态 0 → 2。
 */
export function submitDiffReconciliation(data: {
  /** 对账编码（NotBlank） */
  reconCode: string
  /** 差异明细（JSON 字符串） */
  diffDetail?: string
  /** 差异处理结果 */
  handleResult?: string
  operatorCode?: string
  operatorName?: string
  remark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/reconciliation/submit-diff',
    method: 'post',
    data
  })
}

/**
 * 确认对账（待确认→已确认）：POST /admin-api/finance/reconciliation/confirm
 *
 * 对应后端 ReconciliationConfirmDTO，状态 2 → 3（终态）。
 */
export function confirmReconciliation(data: {
  /** 对账编码（NotBlank） */
  reconCode: string
  /** 差异处理结果 */
  handleResult?: string
  operatorCode?: string
  operatorName?: string
  remark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/reconciliation/confirm',
    method: 'post',
    data
  })
}
