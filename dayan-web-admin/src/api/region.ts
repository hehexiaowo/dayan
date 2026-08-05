import { request } from '@/utils/request'

/**
 * 行政区划接口封装。
 *
 * 对应后端 RegionController（/admin-api/regions/*）。
 * 供前端省市区三级联动下拉使用。
 */

/** 行政区划（后端 SystemDictRegion 实体） */
export interface Region {
  /** 区划代码（6 位国标） */
  regionCode: string
  /** 区划名称 */
  regionName: string
  /** 父级区划代码（省级为 null） */
  parentCode: string | null
  /** 层级：1=省 2=市 3=区县 */
  level: number
}

/** 查全部省级行政区：GET /admin-api/regions/provinces */
export function listProvinces(): Promise<Region[]> {
  return request<Region[]>({
    url: '/admin-api/regions/provinces',
    method: 'get'
  })
}

/** 查某父级的下级行政区：GET /admin-api/regions/children?parentCode= */
export function listRegionChildren(parentCode: string): Promise<Region[]> {
  return request<Region[]>({
    url: '/admin-api/regions/children',
    method: 'get',
    params: { parentCode }
  })
}
