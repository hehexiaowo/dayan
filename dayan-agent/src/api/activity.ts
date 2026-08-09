import request from '@/utils/request';
import type { Activity, PageQuery, PageResult } from '@/types';

/**
 * 活动/内容素材列表（GET /agent-api/activities）。
 * 后端业务接口未实现时降级（由调用方 try/catch）。
 */
export function getActivities(query?: PageQuery): Promise<PageResult<Activity>> {
  return request<PageResult<Activity>>({
    url: '/activities',
    method: 'GET',
    data: (query || {}) as Record<string, any>,
  });
}
