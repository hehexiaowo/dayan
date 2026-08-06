import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SupplierContract, SupplierContractQuery } from '@/types/supplier'

/**
 * 供应商合同接口封装。
 *
 * 对应后端 SupplierContractController（/admin-api/supplier/contract/*）。
 *
 * 注意：
 * - 合同主键是 contractCode（String），由后端 CodeGenerator 生成（HT 前缀），create 表单不含。
 * - update 的 contractCode 走 query string（@RequestParam），不是 path variable。
 * - status 默认由后端在 create 时置 1（待审核）；前端仅通过状态流转接口修改。
 * - 后端不校验状态流转合法性，前端按状态守卫表显示流转按钮。
 */

/** 合同分页：GET /admin-api/supplier/contract/page */
export function pageContracts(query: SupplierContractQuery): Promise<PageResult<SupplierContract>> {
  return request<PageResult<SupplierContract>>({
    url: '/admin-api/supplier/contract/page',
    method: 'get',
    params: query
  })
}

/** 合同详情：GET /admin-api/supplier/contract/{contractCode} */
export function getContract(contractCode: string): Promise<SupplierContract> {
  return request<SupplierContract>({
    url: `/admin-api/supplier/contract/${contractCode}`,
    method: 'get'
  })
}

/** 新增合同：POST /admin-api/supplier/contract（返回 contractCode） */
export function createContract(data: Partial<SupplierContract>): Promise<string> {
  return request<string>({
    url: '/admin-api/supplier/contract',
    method: 'post',
    data
  })
}

/**
 * 修改合同：PUT /admin-api/supplier/contract?contractCode=
 *
 * contractCode 走 query string（@RequestParam），非 path。body 为修改字段。
 * 状态流转亦通过此接口，body 携带 { status, auditRemark? }。
 */
export function updateContract(
  contractCode: string,
  data: Partial<SupplierContract>
): Promise<void> {
  return request<void>({
    url: '/admin-api/supplier/contract',
    method: 'put',
    params: { contractCode },
    data
  })
}

/** 删除合同：DELETE /admin-api/supplier/contract/{contractCode} */
export function deleteContract(contractCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/supplier/contract/${contractCode}`,
    method: 'delete'
  })
}
