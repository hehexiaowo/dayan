import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ButlerInfo, ButlerInfoQuery, ServiceSession, ServiceSessionQuery } from '@/types/service'

/**
 * 服务域接口封装（管家信息 + 服务会话）。
 *
 * 对应后端：
 * - ButlerInfoAdminController（/admin-api/butler/info/*）：标准 CRUD
 * - ServiceSessionAdminController（/admin-api/service/session/*）：CRUD + 状态机动作
 */

// ==================== 管家信息 ====================

/** 管家分页：GET /admin-api/butler/info/page */
export function pageButlers(query: ButlerInfoQuery): Promise<PageResult<ButlerInfo>> {
  return request<PageResult<ButlerInfo>>({
    url: '/admin-api/butler/info/page',
    method: 'get',
    params: query
  })
}

/** 管家列表（全量）：GET /admin-api/butler/info/list */
export function listButlers(query?: Partial<ButlerInfoQuery>): Promise<ButlerInfo[]> {
  return request<ButlerInfo[]>({
    url: '/admin-api/butler/info/list',
    method: 'get',
    params: query
  })
}

/** 管家详情：GET /admin-api/butler/info/{butlerCode} */
export function getButler(butlerCode: string): Promise<ButlerInfo> {
  return request<ButlerInfo>({
    url: `/admin-api/butler/info/${butlerCode}`,
    method: 'get'
  })
}

/** 新增管家：POST /admin-api/butler/info（返回 butlerCode） */
export function createButler(data: Partial<ButlerInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/butler/info',
    method: 'post',
    data
  })
}

/** 修改管家：PUT /admin-api/butler/info/{butlerCode} */
export function updateButler(butlerCode: string, data: Partial<ButlerInfo>): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/info/${butlerCode}`,
    method: 'put',
    data
  })
}

/** 删除管家：DELETE /admin-api/butler/info/{butlerCode} */
export function deleteButler(butlerCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/info/${butlerCode}`,
    method: 'delete'
  })
}

// ==================== 服务会话 ====================

/** 会话分页：GET /admin-api/service/session/page */
export function pageSessions(query: ServiceSessionQuery): Promise<PageResult<ServiceSession>> {
  return request<PageResult<ServiceSession>>({
    url: '/admin-api/service/session/page',
    method: 'get',
    params: query
  })
}

/** 会话列表（按条件）：GET /admin-api/service/session/list */
export function listSessions(query?: Partial<ServiceSessionQuery>): Promise<ServiceSession[]> {
  return request<ServiceSession[]>({
    url: '/admin-api/service/session/list',
    method: 'get',
    params: query
  })
}

/** 会话详情：GET /admin-api/service/session/{sessionCode} */
export function getSession(sessionCode: string): Promise<ServiceSession> {
  return request<ServiceSession>({
    url: `/admin-api/service/session/${sessionCode}`,
    method: 'get'
  })
}

/**
 * 修改会话（普通字段）：PUT /admin-api/service/session/{sessionCode}
 *
 * 用于编辑非状态机字段（serviceTitle / serviceDescription / priority / remark 等）。
 */
export function updateSession(sessionCode: string, data: Partial<ServiceSession>): Promise<void> {
  return request<void>({
    url: `/admin-api/service/session/${sessionCode}`,
    method: 'put',
    data
  })
}

/** 删除会话：DELETE /admin-api/service/session/{sessionCode} */
export function deleteSession(sessionCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/service/session/${sessionCode}`,
    method: 'delete'
  })
}

/**
 * 通用状态机流转：POST /admin-api/service/session/transition
 *
 * 入参对齐后端 TransitionDTO（@RequestBody）。
 * event 取值见 com.dayan.service.enums.ServiceSessionEvent：
 * assign_butler / submit_demand / confirm_solution / reject_solution /
 * start_service / finish / cancel。
 * 业务专用接口（/assign-butler 等）已封装对应事件，本接口供前端统一调用。
 * 返回流转后的新 sessionStatus 状态码。
 */
export function transitionSession(sessionCode: string, event: string): Promise<number> {
  return request<number>({
    url: '/admin-api/service/session/transition',
    method: 'post',
    data: { sessionCode, event }
  })
}
