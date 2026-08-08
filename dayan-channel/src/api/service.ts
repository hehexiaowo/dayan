import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ServiceSession, ServiceSessionQuery } from '@/types/service'

/**
 * 服务域接口（渠道端）。
 * 对应后端 /channel-api/service-sessions。
 */

/** 服务记录分页：GET /channel-api/service-sessions */
export function pageServiceSessions(
  query: ServiceSessionQuery
): Promise<PageResult<ServiceSession>> {
  return request<PageResult<ServiceSession>>({
    url: '/channel-api/service-sessions',
    method: 'get',
    params: query
  })
}

/** 服务记录详情：GET /channel-api/service-sessions/{sessionCode} */
export function getServiceSession(sessionCode: string): Promise<ServiceSession> {
  return request<ServiceSession>({
    url: `/channel-api/service-sessions/${sessionCode}`,
    method: 'get'
  })
}
