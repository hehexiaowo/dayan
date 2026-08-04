/**
 * 机构相关 API。
 * 后端业务接口未实现，调用方 try/catch 降级。
 */
import request from '@/utils/request';
import type { Park, PageResult, PageQuery } from '@/types';

export interface ParkQuery extends PageQuery {
  /** 区域筛选 */
  region?: string;
  /** 标签筛选 */
  tag?: string;
}

/** 机构列表（分页） */
export function getParks(query: ParkQuery = {}): Promise<PageResult<Park>> {
  return request<PageResult<Park>>({ url: '/parks', method: 'GET', data: query });
}

/** 机构详情 */
export function getParkDetail(parkCode: string): Promise<Park> {
  return request<Park>({ url: `/parks/${parkCode}`, method: 'GET' });
}
