import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { FinanceFlow, FinanceFlowQuery } from '@/types/finance'

/**
 * 资金流水（FinanceFlow）接口封装。
 *
 * 对应后端 FinanceFlowAdminController（基路径 /admin-api/finance/flow）。
 *
 * 流水主要由系统在业务过程中自动生成（下单 / 退款 / 结算等），手动录入为辅助功能。
 * - page：分页查询（支持多维度筛选）；
 * - get：详情；
 * - record：手动记录一条流水，服务端生成 flowCode / flowTime / balanceBefore / balanceAfter。
 *
 * 流水状态仅 正常 / 已冲正 两种，冲正通常不在此页面操作，故本封装不含状态流转接口。
 */

/** 流水分页：GET /admin-api/finance/flow/page */
export function pageFlows(query: FinanceFlowQuery): Promise<PageResult<FinanceFlow>> {
  return request<PageResult<FinanceFlow>>({
    url: '/admin-api/finance/flow/page',
    method: 'get',
    params: query
  })
}

/** 流水详情：GET /admin-api/finance/flow/{flowCode} */
export function getFlow(flowCode: string): Promise<FinanceFlow> {
  return request<FinanceFlow>({
    url: `/admin-api/finance/flow/${flowCode}`,
    method: 'get'
  })
}

/**
 * 记录流水（手动录入）：POST /admin-api/finance/flow/record
 *
 * 对应后端 RecordFlowDTO（@Valid 校验）。
 * - flowCode 由服务端生成，不在请求体中；
 * - flowTime / balanceBefore / balanceAfter 由服务端生成，不在请求体中。
 * 成功后返回新生成的 flowCode。
 */
export function recordFlow(data: {
  /** 流水类型：1收入/2支出/3退款/4结算（NotNull） */
  flowType: number
  /** 业务类型：equity_order/scene_order/course_order/travel_order/settlement（NotBlank） */
  bizType: string
  /** 业务编码 */
  bizCode?: string
  /** 账号类型：organ/channel/agent/client/supplier（NotBlank） */
  accountType: string
  /** 账号编码（NotBlank） */
  accountCode: string
  /** 流水金额（NotNull） */
  flowAmount: number
  /** 支付方式：1微信/2支付宝/3银行转账/4余额/5线下 */
  payType?: number
  /** 交易流水号 */
  tradeNo?: string
  /** 对方类型 */
  counterpartyType?: string
  /** 对方编码 */
  counterpartyCode?: string
  /** 对方名称 */
  counterpartyName?: string
  /** 流水描述 */
  flowDescription?: string
  /** 备注 */
  remark?: string
}): Promise<string> {
  return request<string>({
    url: '/admin-api/finance/flow/record',
    method: 'post',
    data
  })
}
