/**
 * 养老网络机构查询 API（client 端）。
 * 对接后端 ParkClientController（/client-api/park/*），复用 ParkAgentQueryService 只读服务。
 */
import request from '@/utils/request';
import type { CategoryCount, RegionDrillResult, RegionQuery, ParkCategory, ParkDetail, ParkFullDetail } from '@/types/park';

/**
 * 三分类机构数量统计
 * GET /client-api/park/categories
 */
export function getCategories(): Promise<CategoryCount[]> {
  return request<CategoryCount[]>({
    url: '/park/categories',
    method: 'GET',
  });
}

/**
 * 区域下钻查询
 * GET /client-api/park/regions
 */
export function getRegions(params: RegionQuery): Promise<RegionDrillResult> {
  return request<RegionDrillResult>({
    url: '/park/regions',
    method: 'GET',
    data: params as Record<string, unknown>,
  });
}

/**
 * 机构详情
 * GET /client-api/park/{parkCode}
 */
export function getParkDetail(parkCode: string): Promise<ParkDetail> {
  return request<ParkDetail>({
    url: `/park/${parkCode}`,
    method: 'GET',
  });
}

/**
 * 机构完整详情（聚合主表+全部子实体）
 * GET /client-api/park/{parkCode}/full?network=vital
 * network 有值时服务端过滤展示板块（板块空 tags=全部业态）
 */
export function getParkFullDetail(parkCode: string, network?: ParkCategory): Promise<ParkFullDetail> {
  return request<ParkFullDetail>({
    url: `/park/${parkCode}/full`,
    method: 'GET',
    data: network ? { network } : undefined,
  });
}
