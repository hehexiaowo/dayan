import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SupplierInfo, SupplierInfoQuery, SupplierAuditDTO } from '@/types/supplier'

/**
 * 供应商信息接口封装。
 *
 * 对应后端 SupplierInfoAdminController（/admin-api/supplier/info/*）。
 * 含审核流：audit（待审→通过/驳回）。
 */

/** 供应商分页：GET /admin-api/supplier/info/page */
export function pageSuppliers(query: SupplierInfoQuery): Promise<PageResult<SupplierInfo>> {
  return request<PageResult<SupplierInfo>>({
    url: '/admin-api/supplier/info/page',
    method: 'get',
    params: query
  })
}

/** 供应商列表（全量，下拉/关联用）：GET /admin-api/supplier/info/list */
export function listSuppliers(query?: Partial<SupplierInfoQuery>): Promise<SupplierInfo[]> {
  return request<SupplierInfo[]>({
    url: '/admin-api/supplier/info/list',
    method: 'get',
    params: query
  })
}

/** 供应商详情：GET /admin-api/supplier/info/{supplierCode} */
export function getSupplier(supplierCode: string): Promise<SupplierInfo> {
  return request<SupplierInfo>({
    url: `/admin-api/supplier/info/${supplierCode}`,
    method: 'get'
  })
}

/** 新增供应商：POST /admin-api/supplier/info（返回 supplierCode） */
export function createSupplier(data: Partial<SupplierInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/supplier/info',
    method: 'post',
    data
  })
}

/**
 * 修改供应商：PUT /admin-api/supplier/info?supplierCode=
 *
 * supplierCode 走 query string（@RequestParam），非 path。
 */
export function updateSupplier(
  supplierCode: string,
  data: Partial<SupplierInfo>
): Promise<void> {
  return request<void>({
    url: '/admin-api/supplier/info',
    method: 'put',
    params: { supplierCode },
    data
  })
}

/**
 * 审核供应商（待审→通过/驳回）：POST /admin-api/supplier/info/audit
 *
 * auditStatus: 1=通过 / 2=驳回（对齐后端 SupplierAuditDTO）。
 */
export function auditSupplier(data: SupplierAuditDTO): Promise<void> {
  return request<void>({
    url: '/admin-api/supplier/info/audit',
    method: 'post',
    data
  })
}

/** 删除供应商：DELETE /admin-api/supplier/info/{supplierCode} */
export function deleteSupplier(supplierCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/supplier/info/${supplierCode}`,
    method: 'delete'
  })
}
