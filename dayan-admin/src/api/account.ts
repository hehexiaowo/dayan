import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { Account, AccountQuery, AccountStatus } from '@/types/account'

/**
 * 账号接口封装。
 *
 * 对应后端 OrganAccountAdminController（/admin-api/accounts/*）。
 * 路径无 /v1 版本前缀（见规格 0.2）。
 */

/** 账号分页：GET /admin-api/accounts */
export function pageAccounts(query: AccountQuery): Promise<PageResult<Account>> {
  return request<PageResult<Account>>({
    url: '/admin-api/accounts',
    method: 'get',
    params: query
  })
}

/** 账号详情：GET /admin-api/accounts/{accountCode} */
export function getAccount(accountCode: string): Promise<Account> {
  return request<Account>({
    url: `/admin-api/accounts/${accountCode}`,
    method: 'get'
  })
}

/** 新增账号：POST /admin-api/accounts */
export function createAccount(data: Account): Promise<string> {
  return request<string>({
    url: '/admin-api/accounts',
    method: 'post',
    data
  })
}

/** 修改账号：PUT /admin-api/accounts/{accountCode} */
export function updateAccount(accountCode: string, data: Account): Promise<void> {
  return request<void>({
    url: `/admin-api/accounts/${accountCode}`,
    method: 'put',
    data
  })
}

/** 重置密码：PUT /admin-api/accounts/{accountCode}/reset-password */
export function resetPassword(accountCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/accounts/${accountCode}/reset-password`,
    method: 'put'
  })
}

/** 切换账号状态：PUT /admin-api/accounts/{accountCode}/status/{status} */
export function switchAccountStatus(accountCode: string, status: AccountStatus): Promise<void> {
  return request<void>({
    url: `/admin-api/accounts/${accountCode}/status/${status}`,
    method: 'put'
  })
}

/** 删除账号：DELETE /admin-api/accounts/{accountCode} */
export function deleteAccount(accountCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/accounts/${accountCode}`,
    method: 'delete'
  })
}
