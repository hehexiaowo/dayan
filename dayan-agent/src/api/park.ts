import request from '@/utils/request';
import type { CategoryCount, RegionDrillResult, RegionQuery, ParkDetail, ParkFullDetail } from '@/types/park';

/**
 * 三分类机构数量统计
 * GET /agent-api/park/categories
 */
export function getCategories(): Promise<CategoryCount[]> {
  return request<CategoryCount[]>({
    url: '/park/categories',
    method: 'GET',
  });
}

/**
 * 区域下钻查询
 * GET /agent-api/park/regions
 */
export function getRegions(params: RegionQuery): Promise<RegionDrillResult> {
  return request<RegionDrillResult>({
    url: '/park/regions',
    method: 'GET',
    data: params as Record<string, any>,
  });
}

/**
 * 机构详情
 * GET /agent-api/park/{parkCode}
 */
export function getParkDetail(parkCode: string): Promise<ParkDetail> {
  return request<ParkDetail>({
    url: `/park/${parkCode}`,
    method: 'GET',
  });
}

/**
 * 机构完整详情（聚合主表+全部子实体）
 * GET /agent-api/park/{parkCode}/full
 */
export function getParkFullDetail(parkCode: string): Promise<ParkFullDetail> {
  return request<ParkFullDetail>({
    url: `/park/${parkCode}/full`,
    method: 'GET',
  });
}
