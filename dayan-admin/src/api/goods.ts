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
 * 后端 GoodsInfoShelfDTO 字段为 goodsStatus（非 shelfStatus），值为：
 * - 1 = 上架
 * - 0 = 下架
 *
 * 语义偏差（已知遗留，不在前端任务范围修）：
 * DDL 的 goods_status 是 5 态（0草稿/1待上架/2已上架/3已下架/4已售罄），shelf 接口的 1
 * 在 DDL 字面是"待上架"，但 shelf 接口语义是"上架"。后端无完整状态机（statemachine 目录为空），
 * 故前端按 shelf 契约传 0/1；列表按 DDL 5 态展示（2 才是真正的"已上架"）。
 * 调用方判断当前是否上架用 `goodsStatus === GoodsStatus.ON_SHELF`（即 2），点击时传 1 上架 / 0 下架。
 *
 * TODO: 后端状态机补齐后，此处的语义偏差需消除。
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
