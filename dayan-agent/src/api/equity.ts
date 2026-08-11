import request from '@/utils/request';
import type { EquityCard, EquityQuery, EquityStats, PageResult } from '@/types';

/**
 * 我的权益卡列表（GET /agent-api/equities）。
 */
export function getEquityCards(query?: EquityQuery): Promise<PageResult<EquityCard>> {
  return request<PageResult<EquityCard>>({
    url: '/equities',
    method: 'GET',
    data: (query || {}) as Record<string, any>,
  });
}

/**
 * 我的权益卡状态统计（GET /agent-api/equities/stats）。
 */
export function getEquityStats(): Promise<EquityStats> {
  return request<EquityStats>({
    url: '/equities/stats',
    method: 'GET',
  });
}
