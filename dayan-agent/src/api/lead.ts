import request from '@/utils/request';
import type { Lead, PageQuery, PageResult } from '@/types';

/**
 * 客户线索列表（GET /agent-api/leads）。
 * 后端业务接口未实现时降级（由调用方 try/catch）。
 */
export function getLeads(query?: PageQuery): Promise<PageResult<Lead>> {
  return request<PageResult<Lead>>({
    url: '/leads',
    method: 'GET',
    data: (query || {}) as Record<string, any>,
  });
}
