import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { FinanceAccount, FinanceAccountQuery } from '@/types/finance-account'

/**
 * 应收应付账目（FinanceAccount）接口封装。
 *
 * 对应后端 FinanceAccountAdminController（基路径 /admin-api/finance/account）。
 *
 * 收/付推进（account_status）：
 * - receive 累加 received_amount、扣减 remain_amount：
 *   remain > 0 → 0(待收付)→1(部分收付)；remain ≤ 0 → 2(已结清)。
 *
 * 注意：后端无 update / delete 接口（账目创建后只通过 receive 推进或人工核销），
 * 故本封装仅提供 page / list / get / create / receive。
 */

/** 账目分页：GET /admin-api/finance/account/page */
export function pageAccounts(query: FinanceAccountQuery): Promise<PageResult<FinanceAccount>> {
  return request<PageResult<FinanceAccount>>({
    url: '/admin-api/finance/account/page',
    method: 'get',
    params: query
  })
}

/** 账目列表（全量）：GET /admin-api/finance/account/list */
export function listAccounts(query: FinanceAccountQuery): Promise<FinanceAccount[]> {
  return request<FinanceAccount[]>({
    url: '/admin-api/finance/account/list',
    method: 'get',
    params: query
  })
}

/** 账目详情：GET /admin-api/finance/account/{accountCode} */
export function getAccount(accountCode: string): Promise<FinanceAccount> {
  return request<FinanceAccount>({
    url: `/admin-api/finance/account/${accountCode}`,
    method: 'get'
  })
}

/**
 * 创建账目：POST /admin-api/finance/account
 *
 * 对应后端 CreateAccountDTO（@Valid 校验）。
 * - accountCode 由服务端生成，不在请求体中；
 * - received_amount=0、remain_amount=total_amount、account_status=0 由服务端初始化。
 * 成功后返回新生成的 accountCode。
 */
export function createAccount(data: {
  /** 账目方向：1=应收/2=应付（NotNull） */
  direction: number
  /** 对象类型：channel/supplier/agent（NotBlank） */
  accountType: string
  /** 对象编码（NotBlank） */
  targetCode: string
  /** 对象名称（NotBlank） */
  targetName: string
  /** 业务类型：equity_purchase/scene_fee/service_fee（NotBlank） */
  bizType: string
  /** 业务编码 */
  bizCode?: string
  /** 应收/应付总额（NotNull） */
  totalAmount: number
  /** 到期日期 */
  dueDate?: string
  remark?: string
}): Promise<string> {
  return request<string>({
    url: '/admin-api/finance/account',
    method: 'post',
    data
  })
}

/**
 * 账目收/付款：POST /admin-api/finance/account/receive
 *
 * 对应后端 AccountReceiveDTO（@Valid 校验）。
 * 累加 received_amount、扣减 remain_amount、推进 account_status：
 * remain > 0 → 0→1（部分收付）；remain ≤ 0 → 2（已结清）。
 * receiveTime 为空时服务端取当前时间。
 */
export function receiveAccount(data: {
  /** 账目编码（NotBlank） */
  accountCode: string
  /** 本笔收/付金额（正数，NotNull） */
  amount: number
  /** 收/付款时间（为空取当前时间） */
  receiveTime?: string
  remark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/finance/account/receive',
    method: 'post',
    data
  })
}
