import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { Client, ClientQuery, ClientAccount, ClientAccountQuery } from '@/types/client'

/**
 * 客户接口封装。
 *
 * 对应后端 Channel 端 ClientController（/channel-api/clients/*）。
 * 注意：本期后端业务端点尚未实现，调用会走 request.ts 响应拦截器报错，
 * 调用方需 try/catch 降级处理。
 */

/** 客户分页：GET /channel-api/clients */
export function pageClients(query: ClientQuery): Promise<PageResult<Client>> {
  return request<PageResult<Client>>({
    url: '/channel-api/clients',
    method: 'get',
    params: query
  })
}

// ==================== 客户账号（/channel-api/client-accounts）====================

/** 客户账号分页 */
export function pageClientAccounts(
  query: ClientAccountQuery
): Promise<PageResult<ClientAccount>> {
  return request<PageResult<ClientAccount>>({
    url: '/channel-api/client-accounts',
    method: 'get',
    params: query
  })
}

/** 客户账号详情 */
export function getClientAccount(clientCode: string): Promise<ClientAccount> {
  return request<ClientAccount>({
    url: `/channel-api/client-accounts/${clientCode}`,
    method: 'get'
  })
}
