import request from '@/utils/request';
import type { SceneActivity, SceneScheduleItem, PageQuery, PageResult } from '@/types';

/**
 * 场景列表查询参数。
 */
export interface SceneQuery extends PageQuery {
  sceneType?: number;
}

/**
 * 场景日程查询参数。
 */
export interface ScheduleQuery extends PageQuery {
  sceneCode?: string;
  status?: number;
}

/**
 * 本渠道已配置场景列表（GET /agent-api/scenes）。
 */
export function getSceneList(query?: SceneQuery): Promise<PageResult<SceneActivity>> {
  return request<PageResult<SceneActivity>>({
    url: '/scenes',
    method: 'GET',
    data: (query || {}) as Record<string, any>,
  });
}

/**
 * 场景详情（GET /agent-api/scenes/{sceneCode}）。
 */
export function getSceneDetail(sceneCode: string): Promise<SceneActivity> {
  return request<SceneActivity>({
    url: `/scenes/${sceneCode}`,
    method: 'GET',
  });
}

/**
 * 场景日程列表（GET /agent-api/scenes/schedules）。
 */
export function getSceneSchedules(query?: ScheduleQuery): Promise<PageResult<SceneScheduleItem>> {
  return request<PageResult<SceneScheduleItem>>({
    url: '/scenes/schedules',
    method: 'GET',
    data: (query || {}) as Record<string, any>,
  });
}
