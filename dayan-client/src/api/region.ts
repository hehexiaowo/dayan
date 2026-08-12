import request from '@/utils/request';

/** 行政区划（对齐后端 SystemDictRegion） */
export interface Region {
  regionCode: string;
  regionName: string;
  parentCode: string | null;
  level: number;
}

/** 省级列表：GET /regions/provinces */
export function listProvinces(): Promise<Region[]> {
  return request<Region[]>({ url: '/regions/provinces', method: 'GET' });
}

/** 下级列表：GET /regions/children?parentCode= */
export function listRegionChildren(parentCode: string): Promise<Region[]> {
  return request<Region[]>({ url: '/regions/children', method: 'GET', data: { parentCode } });
}
