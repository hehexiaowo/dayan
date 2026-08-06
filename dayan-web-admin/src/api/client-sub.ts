import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ClientAccount,
  ClientAccountQuery,
  ClientFamilyMember,
  ClientAddress,
  ClientHealthProfile,
  ClientCareNeed,
  ClientCareNeedQuery,
  ClientFavorite,
  ClientFavoriteQuery
} from '@/types/client'

/**
 * 客户域子表接口封装（6 张子表）。
 *
 * 对应后端 dayan-module-client 的 6 个 admin 控制器，全部挂在 /admin-api 前缀下。
 *
 * 主键规则（重要，与 park/supplier 子表不同）：
 * - ClientAccount：业务主键 clientCode（非 id）。update/delete/reset-password 都用 clientCode。
 * - ClientFamilyMember / ClientAddress / ClientCareNeed / ClientFavorite：自增 id。
 * - ClientHealthProfile：一客户一档案，业务主键 clientCode；无 update 端点，
 *   编辑走 POST（saveOrUpdate 语义）。
 *
 * 返回类型规则：
 * - page 方法：Promise<PageResult<VO>>
 * - by-client 方法：Promise<VO[]>（非分页）
 * - get 单条：Promise<VO>
 */

// ---------------- 1. 客户账号（client-accounts）----------------

/** 客户账号分页：GET /admin-api/client-accounts */
export function pageClientAccounts(query: ClientAccountQuery): Promise<PageResult<ClientAccount>> {
  return request<PageResult<ClientAccount>>({
    url: '/admin-api/client-accounts',
    method: 'get',
    params: query
  })
}

/** 新增客户账号：POST /admin-api/client-accounts（password 可填，留空服务端用默认值） */
export function createClientAccount(data: Partial<ClientAccount> & { password?: string }): Promise<void> {
  return request<void>({
    url: '/admin-api/client-accounts',
    method: 'post',
    data
  })
}

/** 修改客户账号：PUT /admin-api/client-accounts/{clientCode}（不含密码，密码走 reset） */
export function updateClientAccount(clientCode: string, data: Partial<ClientAccount>): Promise<void> {
  return request<void>({
    url: `/admin-api/client-accounts/${clientCode}`,
    method: 'put',
    data
  })
}

/** 重置账号密码：PUT /admin-api/client-accounts/{clientCode}/reset-password（无 body，服务端重置为 dayan@123） */
export function resetClientAccountPassword(clientCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/client-accounts/${clientCode}/reset-password`,
    method: 'put'
  })
}

/** 删除客户账号：DELETE /admin-api/client-accounts/{clientCode} */
export function deleteClientAccount(clientCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/client-accounts/${clientCode}`,
    method: 'delete'
  })
}

// ---------------- 2. 客户家庭成员（family-members）----------------

/** 客户家庭成员列表（全量，按 clientCode 过滤）：GET /admin-api/family-members/by-client/{clientCode} */
export function listFamilyMembersByClient(clientCode: string): Promise<ClientFamilyMember[]> {
  return request<ClientFamilyMember[]>({
    url: `/admin-api/family-members/by-client/${clientCode}`,
    method: 'get'
  })
}

/** 新增家庭成员：POST /admin-api/family-members */
export function createFamilyMember(data: Partial<ClientFamilyMember>): Promise<void> {
  return request<void>({
    url: '/admin-api/family-members',
    method: 'post',
    data
  })
}

/** 修改家庭成员：PUT /admin-api/family-members/{id} */
export function updateFamilyMember(id: number, data: Partial<ClientFamilyMember>): Promise<void> {
  return request<void>({
    url: `/admin-api/family-members/${id}`,
    method: 'put',
    data
  })
}

/** 删除家庭成员：DELETE /admin-api/family-members/{id} */
export function deleteFamilyMember(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/family-members/${id}`,
    method: 'delete'
  })
}

// ---------------- 3. 客户收货地址（addresses）----------------

/** 客户收货地址列表（全量，按 clientCode 过滤）：GET /admin-api/addresses/by-client/{clientCode} */
export function listAddressesByClient(clientCode: string): Promise<ClientAddress[]> {
  return request<ClientAddress[]>({
    url: `/admin-api/addresses/by-client/${clientCode}`,
    method: 'get'
  })
}

/** 新增收货地址：POST /admin-api/addresses */
export function createAddress(data: Partial<ClientAddress>): Promise<void> {
  return request<void>({
    url: '/admin-api/addresses',
    method: 'post',
    data
  })
}

/** 修改收货地址：PUT /admin-api/addresses/{id} */
export function updateAddress(id: number, data: Partial<ClientAddress>): Promise<void> {
  return request<void>({
    url: `/admin-api/addresses/${id}`,
    method: 'put',
    data
  })
}

/** 设为默认地址：PUT /admin-api/addresses/{id}/default（后端自动互斥，把同 clientCode 其他地址置 0） */
export function setDefaultAddress(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/addresses/${id}/default`,
    method: 'put'
  })
}

/** 删除收货地址：DELETE /admin-api/addresses/{id} */
export function deleteAddress(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/addresses/${id}`,
    method: 'delete'
  })
}

// ---------------- 4. 客户健康档案（health-profiles）----------------

/**
 * 客户健康档案详情：GET /admin-api/health-profiles/{clientCode}（单条，一客户一档案）。
 * 不存在时后端返回 null（无档案）。
 */
export function getHealthProfile(clientCode: string): Promise<ClientHealthProfile | null> {
  return request<ClientHealthProfile | null>({
    url: `/admin-api/health-profiles/${clientCode}`,
    method: 'get'
  })
}

/**
 * 保存客户健康档案：POST /admin-api/health-profiles（upsert：存在则更新，不存在则新增）。
 * 无独立 update 端点，编辑与新增都走此方法。
 */
export function saveHealthProfile(data: Partial<ClientHealthProfile>): Promise<void> {
  return request<void>({
    url: '/admin-api/health-profiles',
    method: 'post',
    data
  })
}

/** 删除客户健康档案：DELETE /admin-api/health-profiles/{clientCode} */
export function deleteHealthProfile(clientCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/health-profiles/${clientCode}`,
    method: 'delete'
  })
}

// ---------------- 5. 客户照护需求（care-needs）----------------

/** 客户照护需求分页：GET /admin-api/care-needs */
export function pageCareNeeds(query: ClientCareNeedQuery): Promise<PageResult<ClientCareNeed>> {
  return request<PageResult<ClientCareNeed>>({
    url: '/admin-api/care-needs',
    method: 'get',
    params: query
  })
}

/** 客户照护需求列表（全量，按 clientCode 过滤）：GET /admin-api/care-needs/by-client/{clientCode} */
export function listCareNeedsByClient(clientCode: string): Promise<ClientCareNeed[]> {
  return request<ClientCareNeed[]>({
    url: `/admin-api/care-needs/by-client/${clientCode}`,
    method: 'get'
  })
}

/** 新增照护需求：POST /admin-api/care-needs */
export function createCareNeed(data: Partial<ClientCareNeed>): Promise<void> {
  return request<void>({
    url: '/admin-api/care-needs',
    method: 'post',
    data
  })
}

/** 修改照护需求：PUT /admin-api/care-needs/{id} */
export function updateCareNeed(id: number, data: Partial<ClientCareNeed>): Promise<void> {
  return request<void>({
    url: `/admin-api/care-needs/${id}`,
    method: 'put',
    data
  })
}

/** 删除照护需求：DELETE /admin-api/care-needs/{id} */
export function deleteCareNeed(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/care-needs/${id}`,
    method: 'delete'
  })
}

// ---------------- 6. 客户收藏（client-favorites）----------------

/** 客户收藏分页：GET /admin-api/client-favorites */
export function pageClientFavorites(query: ClientFavoriteQuery): Promise<PageResult<ClientFavorite>> {
  return request<PageResult<ClientFavorite>>({
    url: '/admin-api/client-favorites',
    method: 'get',
    params: query
  })
}

/** 客户收藏列表（全量，按 clientCode 过滤）：GET /admin-api/client-favorites/by-client/{clientCode} */
export function listClientFavoritesByClient(clientCode: string): Promise<ClientFavorite[]> {
  return request<ClientFavorite[]>({
    url: `/admin-api/client-favorites/by-client/${clientCode}`,
    method: 'get'
  })
}

/** 新增收藏：POST /admin-api/client-favorites */
export function createClientFavorite(data: Partial<ClientFavorite>): Promise<void> {
  return request<void>({
    url: '/admin-api/client-favorites',
    method: 'post',
    data
  })
}

/** 删除收藏：DELETE /admin-api/client-favorites/{id}（无 update，要改先删再加） */
export function deleteClientFavorite(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/client-favorites/${id}`,
    method: 'delete'
  })
}
