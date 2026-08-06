import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SupplierContact, SupplierContactQuery } from '@/types/supplier'

/**
 * 供应商联系人接口封装。
 *
 * 对应后端 SupplierContactController（/admin-api/supplier/contact/*）。
 *
 * 注意：
 * - 主键 Long id（自增）。
 * - update 的 id 走 query string（@RequestParam），不是 path variable。
 * - isPrimary（0/1）同供应商唯一，后端自动互斥；前端提交前弹 confirm 提醒。
 */

/** 联系人分页：GET /admin-api/supplier/contact/page */
export function pageContacts(query: SupplierContactQuery): Promise<PageResult<SupplierContact>> {
  return request<PageResult<SupplierContact>>({
    url: '/admin-api/supplier/contact/page',
    method: 'get',
    params: query
  })
}

/** 联系人详情：GET /admin-api/supplier/contact/{id} */
export function getContact(id: number): Promise<SupplierContact> {
  return request<SupplierContact>({
    url: `/admin-api/supplier/contact/${id}`,
    method: 'get'
  })
}

/** 新增联系人：POST /admin-api/supplier/contact（返回 id） */
export function createContact(data: Partial<SupplierContact>): Promise<number> {
  return request<number>({
    url: '/admin-api/supplier/contact',
    method: 'post',
    data
  })
}

/**
 * 修改联系人：PUT /admin-api/supplier/contact?id=
 *
 * id 走 query string（@RequestParam），非 path。body 为修改字段。
 */
export function updateContact(id: number, data: Partial<SupplierContact>): Promise<void> {
  return request<void>({
    url: '/admin-api/supplier/contact',
    method: 'put',
    params: { id },
    data
  })
}

/** 删除联系人：DELETE /admin-api/supplier/contact/{id} */
export function deleteContact(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/supplier/contact/${id}`,
    method: 'delete'
  })
}
