import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  SceneItem,
  SceneItemQuery,
  SceneItemPrice,
  SceneItemPriceQuery,
  SceneSchedule,
  SceneScheduleQuery,
  SceneResource,
  SceneResourceQuery
} from '@/types/scene'

/**
 * 场景子表接口封装（项目明细 + 价格档位 + 活动日程 + 所需资源）。
 *
 * 对应后端 4 个子表 Controller：
 * - /admin-api/scene/item        （SceneItemController）
 * - /admin-api/scene/item-price  （SceneItemPriceController）
 * - /admin-api/scene/schedule    （SceneScheduleController）
 * - /admin-api/scene/resource    （SceneResourceController）
 *
 * 约定（与 butler/agent 子表不同）：
 * - 4 子表全部 IdType.AUTO（自增 Long，非雪花）→ 主键 id 用 number，useCrud 传 idKey:'id'。
 * - /list 端点只接 sceneCode 一个参数（与 park-misc.ts 的 listPeripheries 一致），返回数组非分页。
 * - 其余 4 端点（page/get/create/update/delete）路径带 {id} 的以 id 路径参数为准。
 */

// ==================== 项目明细（scene/item）====================

/** 项目明细分页：GET /admin-api/scene/item/page */
export function pageSceneItems(query: SceneItemQuery): Promise<PageResult<SceneItem>> {
  return request<PageResult<SceneItem>>({
    url: '/admin-api/scene/item/page',
    method: 'get',
    params: query
  })
}

/** 项目明细列表（全量，按 sceneCode 过滤）：GET /admin-api/scene/item/list?sceneCode=xxx */
export function listSceneItems(sceneCode: string): Promise<SceneItem[]> {
  return request<SceneItem[]>({
    url: '/admin-api/scene/item/list',
    method: 'get',
    params: { sceneCode }
  })
}

/** 项目明细详情：GET /admin-api/scene/item/{id} */
export function getSceneItem(id: number): Promise<SceneItem> {
  return request<SceneItem>({
    url: `/admin-api/scene/item/${id}`,
    method: 'get'
  })
}

/** 新增项目明细：POST /admin-api/scene/item（返回新 id） */
export function createSceneItem(data: Partial<SceneItem>): Promise<number> {
  return request<number>({
    url: '/admin-api/scene/item',
    method: 'post',
    data
  })
}

/** 修改项目明细：PUT /admin-api/scene/item/{id} */
export function updateSceneItem(id: number, data: Partial<SceneItem>): Promise<void> {
  return request<void>({
    url: `/admin-api/scene/item/${id}`,
    method: 'put',
    data
  })
}

/** 删除项目明细：DELETE /admin-api/scene/item/{id} */
export function deleteSceneItem(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/scene/item/${id}`,
    method: 'delete'
  })
}

// ==================== 价格档位（scene/item-price）====================

/** 价格档位分页：GET /admin-api/scene/item-price/page */
export function pageSceneItemPrices(query: SceneItemPriceQuery): Promise<PageResult<SceneItemPrice>> {
  return request<PageResult<SceneItemPrice>>({
    url: '/admin-api/scene/item-price/page',
    method: 'get',
    params: query
  })
}

/** 价格档位列表（全量，按 sceneCode 过滤）：GET /admin-api/scene/item-price/list?sceneCode=xxx */
export function listSceneItemPrices(sceneCode: string): Promise<SceneItemPrice[]> {
  return request<SceneItemPrice[]>({
    url: '/admin-api/scene/item-price/list',
    method: 'get',
    params: { sceneCode }
  })
}

/** 价格档位详情：GET /admin-api/scene/item-price/{id} */
export function getSceneItemPrice(id: number): Promise<SceneItemPrice> {
  return request<SceneItemPrice>({
    url: `/admin-api/scene/item-price/${id}`,
    method: 'get'
  })
}

/** 新增价格档位：POST /admin-api/scene/item-price（返回新 id） */
export function createSceneItemPrice(data: Partial<SceneItemPrice>): Promise<number> {
  return request<number>({
    url: '/admin-api/scene/item-price',
    method: 'post',
    data
  })
}

/** 修改价格档位：PUT /admin-api/scene/item-price/{id} */
export function updateSceneItemPrice(id: number, data: Partial<SceneItemPrice>): Promise<void> {
  return request<void>({
    url: `/admin-api/scene/item-price/${id}`,
    method: 'put',
    data
  })
}

/** 删除价格档位：DELETE /admin-api/scene/item-price/{id} */
export function deleteSceneItemPrice(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/scene/item-price/${id}`,
    method: 'delete'
  })
}

// ==================== 活动日程（scene/schedule）====================

/** 活动日程分页：GET /admin-api/scene/schedule/page */
export function pageSceneSchedules(query: SceneScheduleQuery): Promise<PageResult<SceneSchedule>> {
  return request<PageResult<SceneSchedule>>({
    url: '/admin-api/scene/schedule/page',
    method: 'get',
    params: query
  })
}

/** 活动日程列表（全量，按 sceneCode 过滤）：GET /admin-api/scene/schedule/list?sceneCode=xxx */
export function listSceneSchedules(sceneCode: string): Promise<SceneSchedule[]> {
  return request<SceneSchedule[]>({
    url: '/admin-api/scene/schedule/list',
    method: 'get',
    params: { sceneCode }
  })
}

/** 活动日程详情：GET /admin-api/scene/schedule/{id} */
export function getSceneSchedule(id: number): Promise<SceneSchedule> {
  return request<SceneSchedule>({
    url: `/admin-api/scene/schedule/${id}`,
    method: 'get'
  })
}

/** 新增活动日程：POST /admin-api/scene/schedule（返回新 id） */
export function createSceneSchedule(data: Partial<SceneSchedule>): Promise<number> {
  return request<number>({
    url: '/admin-api/scene/schedule',
    method: 'post',
    data
  })
}

/** 修改活动日程：PUT /admin-api/scene/schedule/{id} */
export function updateSceneSchedule(id: number, data: Partial<SceneSchedule>): Promise<void> {
  return request<void>({
    url: `/admin-api/scene/schedule/${id}`,
    method: 'put',
    data
  })
}

/** 删除活动日程：DELETE /admin-api/scene/schedule/{id} */
export function deleteSceneSchedule(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/scene/schedule/${id}`,
    method: 'delete'
  })
}

// ==================== 所需资源（scene/resource）====================

/** 所需资源分页：GET /admin-api/scene/resource/page */
export function pageSceneResources(query: SceneResourceQuery): Promise<PageResult<SceneResource>> {
  return request<PageResult<SceneResource>>({
    url: '/admin-api/scene/resource/page',
    method: 'get',
    params: query
  })
}

/** 所需资源列表（全量，按 sceneCode 过滤）：GET /admin-api/scene/resource/list?sceneCode=xxx */
export function listSceneResources(sceneCode: string): Promise<SceneResource[]> {
  return request<SceneResource[]>({
    url: '/admin-api/scene/resource/list',
    method: 'get',
    params: { sceneCode }
  })
}

/** 所需资源详情：GET /admin-api/scene/resource/{id} */
export function getSceneResource(id: number): Promise<SceneResource> {
  return request<SceneResource>({
    url: `/admin-api/scene/resource/${id}`,
    method: 'get'
  })
}

/** 新增所需资源：POST /admin-api/scene/resource（返回新 id） */
export function createSceneResource(data: Partial<SceneResource>): Promise<number> {
  return request<number>({
    url: '/admin-api/scene/resource',
    method: 'post',
    data
  })
}

/** 修改所需资源：PUT /admin-api/scene/resource/{id} */
export function updateSceneResource(id: number, data: Partial<SceneResource>): Promise<void> {
  return request<void>({
    url: `/admin-api/scene/resource/${id}`,
    method: 'put',
    data
  })
}

/** 删除所需资源：DELETE /admin-api/scene/resource/{id} */
export function deleteSceneResource(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/scene/resource/${id}`,
    method: 'delete'
  })
}
