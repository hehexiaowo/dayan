import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SceneInfo, SceneInfoQuery } from '@/types/scene'

/**
 * 场景活动信息接口封装。
 *
 * 对应后端 SceneInfoAdminController（/admin-api/scene/info/*）。
 *
 * 审核流（P12-A 提供后端实现）：
 * - submit（草稿→提交审核，auditStatus=0 待审）
 * - audit（待审→通过/驳回，auditStatus 1通过/2驳回）
 * - shelves（审核通过→上架，sceneStatus=1）
 * - offshelves（已上架→下架，sceneStatus=2）
 * - reshelves（已下架→重新上架，sceneStatus=1）
 * - full（已上架→满期，sceneStatus=3，活动到期或名额约满）
 */

/** 场景分页：GET /admin-api/scene/info/page */
export function pageScenes(query: SceneInfoQuery): Promise<PageResult<SceneInfo>> {
  return request<PageResult<SceneInfo>>({
    url: '/admin-api/scene/info/page',
    method: 'get',
    params: query
  })
}

/** 场景列表（不分页）：GET /admin-api/scene/info/list */
export function listScenes(): Promise<SceneInfo[]> {
  return request<SceneInfo[]>({
    url: '/admin-api/scene/info/list',
    method: 'get'
  })
}

/** 场景详情：GET /admin-api/scene/info/{sceneCode} */
export function getScene(sceneCode: string): Promise<SceneInfo> {
  return request<SceneInfo>({
    url: `/admin-api/scene/info/${sceneCode}`,
    method: 'get'
  })
}

/** 新增场景：POST /admin-api/scene/info（返回 sceneCode） */
export function createScene(data: Partial<SceneInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/scene/info',
    method: 'post',
    data
  })
}

/**
 * 修改场景：PUT /admin-api/scene/info/{sceneCode}
 *
 * sceneCode 走 path（与 content/info 不同，content 走 query string）。
 */
export function updateScene(sceneCode: string, data: Partial<SceneInfo>): Promise<void> {
  return request<void>({
    url: `/admin-api/scene/info/${sceneCode}`,
    method: 'put',
    data
  })
}

/** 删除场景：DELETE /admin-api/scene/info/{sceneCode} */
export function deleteScene(sceneCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/scene/info/${sceneCode}`,
    method: 'delete'
  })
}

/**
 * 提交审核（草稿→待审核）：POST /admin-api/scene/info/submit?sceneCode=
 */
export function submitScene(sceneCode: string): Promise<void> {
  return request<void>({
    url: '/admin-api/scene/info/submit',
    method: 'post',
    params: { sceneCode }
  })
}

/**
 * 审核场景（待审→通过/驳回）：POST /admin-api/scene/info/audit
 *
 * auditStatus: 1=通过 / 2=驳回（对齐后端 SceneInfoAuditDTO 约定，与 content 的 2/3 不同）。
 */
export function auditScene(data: {
  sceneCode: string
  auditStatus: number
  auditRemark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/scene/info/audit',
    method: 'post',
    data
  })
}

/** 上架（审核通过→已上架）：POST /admin-api/scene/info/shelves?sceneCode= */
export function shelvesScene(sceneCode: string): Promise<void> {
  return request<void>({
    url: '/admin-api/scene/info/shelves',
    method: 'post',
    params: { sceneCode }
  })
}

/** 下架（已上架→已下架）：POST /admin-api/scene/info/offshelves?sceneCode= */
export function offshelvesScene(sceneCode: string): Promise<void> {
  return request<void>({
    url: '/admin-api/scene/info/offshelves',
    method: 'post',
    params: { sceneCode }
  })
}

/** 重新上架（已下架→已上架）：POST /admin-api/scene/info/reshelves?sceneCode= */
export function reshelvesScene(sceneCode: string): Promise<void> {
  return request<void>({
    url: '/admin-api/scene/info/reshelves',
    method: 'post',
    params: { sceneCode }
  })
}

/** 满期（已上架→已满期，活动到期或名额约满）：POST /admin-api/scene/info/full?sceneCode= */
export function fullScene(sceneCode: string): Promise<void> {
  return request<void>({
    url: '/admin-api/scene/info/full',
    method: 'post',
    params: { sceneCode }
  })
}
