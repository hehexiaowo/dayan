import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SceneInfo, SceneInfoQuery, SceneSchedule, SceneScheduleQuery } from '@/types/scene'

/**
 * 场景域接口（渠道端）。
 * 对应后端 /channel-api/scenes。
 */

/** 场景分页：GET /channel-api/scenes */
export function pageScenes(query: SceneInfoQuery): Promise<PageResult<SceneInfo>> {
  return request<PageResult<SceneInfo>>({
    url: '/channel-api/scenes',
    method: 'get',
    params: query
  })
}

/** 场景详情：GET /channel-api/scenes/{sceneCode} */
export function getScene(sceneCode: string): Promise<SceneInfo> {
  return request<SceneInfo>({
    url: `/channel-api/scenes/${sceneCode}`,
    method: 'get'
  })
}

// ==================== 场景活动日程（/channel-api/scenes/schedules）====================

/** 场景日程分页：GET /channel-api/scenes/schedules */
export function pageSceneSchedules(query: SceneScheduleQuery): Promise<PageResult<SceneSchedule>> {
  return request<PageResult<SceneSchedule>>({
    url: '/channel-api/scenes/schedules',
    method: 'get',
    params: query
  })
}

/** 场景日程详情：GET /channel-api/scenes/schedules/{id} */
export function getSceneSchedule(id: number): Promise<SceneSchedule> {
  return request<SceneSchedule>({
    url: `/channel-api/scenes/schedules/${id}`,
    method: 'get'
  })
}
