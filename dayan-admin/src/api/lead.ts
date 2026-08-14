import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { LeadInfo, LeadInfoQuery, LeadTrace } from '@/types/lead'

/**
 * 线索域接口封装（只读）。
 *
 * 对应后端 LeadInfoAdminController（/admin-api/lead/info/*）。
 * 线索由分享追踪自动建档，后台仅检索/详情/时间线，无新增改删。
 */

/** 线索分页：GET /admin-api/lead/info/page */
export function pageLeads(query: LeadInfoQuery): Promise<PageResult<LeadInfo>> {
  return request<PageResult<LeadInfo>>({
    url: '/admin-api/lead/info/page',
    method: 'get',
    params: query
  })
}

/** 线索详情：GET /admin-api/lead/info/{leadCode} */
export function getLead(leadCode: string): Promise<LeadInfo> {
  return request<LeadInfo>({
    url: `/admin-api/lead/info/${leadCode}`,
    method: 'get'
  })
}

/** 线索互动时间线：GET /admin-api/lead/info/{leadCode}/traces */
export function getLeadTraces(leadCode: string): Promise<LeadTrace[]> {
  return request<LeadTrace[]>({
    url: `/admin-api/lead/info/${leadCode}/traces`,
    method: 'get'
  })
}
