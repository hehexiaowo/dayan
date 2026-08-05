import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { DistributorInfo, DistributorInfoQuery } from '@/types/distributor'

/**
 * 分销商信息接口封装。
 *
 * 对应后端 DistributorInfoAdminController（/admin-api/distributor/info/*）。
 */

/** 分销商分页：GET /admin-api/distributor/info/page */
export function pageDistributors(query: DistributorInfoQuery): Promise<PageResult<DistributorInfo>> {
  return request<PageResult<DistributorInfo>>({
    url: '/admin-api/distributor/info/page',
    method: 'get',
    params: query
  })
}

/** 分销商列表（全量）：GET /admin-api/distributor/info/list */
export function listDistributors(): Promise<DistributorInfo[]> {
  return request<DistributorInfo[]>({
    url: '/admin-api/distributor/info/list',
    method: 'get'
  })
}

/** 分销商详情：GET /admin-api/distributor/info/{distributorCode} */
export function getDistributor(distributorCode: string): Promise<DistributorInfo> {
  return request<DistributorInfo>({
    url: `/admin-api/distributor/info/${distributorCode}`,
    method: 'get'
  })
}

/** 新增分销商：POST /admin-api/distributor/info（返回 distributorCode） */
export function createDistributor(data: Partial<DistributorInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/distributor/info',
    method: 'post',
    data
  })
}

/**
 * 修改分销商：PUT /admin-api/distributor/info?distributorCode=
 *
 * distributorCode 走 query string（@RequestParam），非 path。
 */
export function updateDistributor(
  distributorCode: string,
  data: Partial<DistributorInfo>
): Promise<void> {
  return request<void>({
    url: '/admin-api/distributor/info',
    method: 'put',
    params: { distributorCode },
    data
  })
}

/** 删除分销商：DELETE /admin-api/distributor/info/{distributorCode} */
export function deleteDistributor(distributorCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/distributor/info/${distributorCode}`,
    method: 'delete'
  })
}
