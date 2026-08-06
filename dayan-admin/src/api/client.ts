import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ClientInfo, ClientInfoQuery } from '@/types/client'

/**
 * 客户接口封装。
 *
 * 对应后端 ClientInfoController（/admin-api/clients/*），RESTful 复数风格。
 *
 * 注意：list 接口返回 PageResult，但 url 无 /page 后缀（直接 GET /admin-api/clients）。
 * 主键 clientCode 由服务端生成。
 */

/** 客户分页：GET /admin-api/clients（url 无 /page 后缀） */
export function pageClients(query: ClientInfoQuery): Promise<PageResult<ClientInfo>> {
  return request<PageResult<ClientInfo>>({
    url: '/admin-api/clients',
    method: 'get',
    params: query
  })
}

/** 客户详情：GET /admin-api/clients/{clientCode} */
export function getClient(clientCode: string): Promise<ClientInfo> {
  return request<ClientInfo>({
    url: `/admin-api/clients/${clientCode}`,
    method: 'get'
  })
}

/** 新增客户：POST /admin-api/clients */
export function createClient(data: Partial<ClientInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/clients',
    method: 'post',
    data
  })
}

/** 修改客户：PUT /admin-api/clients/{clientCode} */
export function updateClient(clientCode: string, data: Partial<ClientInfo>): Promise<void> {
  return request<void>({
    url: `/admin-api/clients/${clientCode}`,
    method: 'put',
    data
  })
}

/** 删除客户：DELETE /admin-api/clients/{clientCode} */
export function deleteClient(clientCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/clients/${clientCode}`,
    method: 'delete'
  })
}
