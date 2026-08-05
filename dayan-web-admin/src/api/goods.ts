import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { GoodsInfo, GoodsInfoQuery, GoodsInfoShelfDTO } from '@/types/goods'

/**
 * 商品信息接口封装。
 *
 * 对应后端 GoodsInfoAdminController（/admin-api/goods/info/*）。
 * 含上下架流：shelf（上架/下架切换）。
 */

/** 商品分页：GET /admin-api/goods/info/page */
export function pageGoods(query: GoodsInfoQuery): Promise<PageResult<GoodsInfo>> {
  return request<PageResult<GoodsInfo>>({
    url: '/admin-api/goods/info/page',
    method: 'get',
    params: query
  })
}

/** 商品列表（全量）：GET /admin-api/goods/info/list */
export function listGoods(): Promise<GoodsInfo[]> {
  return request<GoodsInfo[]>({
    url: '/admin-api/goods/info/list',
    method: 'get'
  })
}

/** 商品详情：GET /admin-api/goods/info/{goodsCode} */
export function getGoods(goodsCode: string): Promise<GoodsInfo> {
  return request<GoodsInfo>({
    url: `/admin-api/goods/info/${goodsCode}`,
    method: 'get'
  })
}

/** 新增商品：POST /admin-api/goods/info（返回 goodsCode） */
export function createGoods(data: Partial<GoodsInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/goods/info',
    method: 'post',
    data
  })
}

/**
 * 修改商品：PUT /admin-api/goods/info/{goodsCode}
 *
 * goodsCode 走 path（与 distributor/supplier 的 query string 不同）。
 */
export function updateGoods(goodsCode: string, data: Partial<GoodsInfo>): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/info/${goodsCode}`,
    method: 'put',
    data
  })
}

/**
 * 商品上下架：POST /admin-api/goods/info/shelf
 *
 * shelfStatus: 1=上架 / 0=下架（对齐后端 GoodsInfoShelfDTO）。
 */
export function shelfGoods(data: GoodsInfoShelfDTO): Promise<void> {
  return request<void>({
    url: '/admin-api/goods/info/shelf',
    method: 'post',
    data
  })
}

/** 删除商品：DELETE /admin-api/goods/info/{goodsCode} */
export function deleteGoods(goodsCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/info/${goodsCode}`,
    method: 'delete'
  })
}
