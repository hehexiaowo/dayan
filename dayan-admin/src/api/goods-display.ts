import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { GoodsDisplayBlock, GoodsDisplayBlockPayload, GoodsDisplayBlockQuery } from '@/types/goods-display'

/**
 * 商品展示板块（GoodsDisplayBlock）接口封装。
 *
 * 对应后端 /admin-api/goods/display-block/*。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - blockType 必填（决定 C 端渲染 tab）；images / imageDescriptions 是 JSON 数组字符串（后端 TEXT 列存）。
 */

/** 展示板块分页：GET /admin-api/goods/display-block/page */
export function pageGoodsDisplayBlocks(query: GoodsDisplayBlockQuery): Promise<PageResult<GoodsDisplayBlock>> {
  return request<PageResult<GoodsDisplayBlock>>({
    url: '/admin-api/goods/display-block/page',
    method: 'get',
    params: query
  })
}

/** 展示板块列表（按商品，仅状态=1）：GET /admin-api/goods/display-block/list?goodsCode=xxx */
export function listGoodsDisplayBlocks(goodsCode: string): Promise<GoodsDisplayBlock[]> {
  return request<GoodsDisplayBlock[]>({
    url: '/admin-api/goods/display-block/list',
    method: 'get',
    params: { goodsCode }
  })
}

/** 展示板块详情：GET /admin-api/goods/display-block/{id} */
export function getGoodsDisplayBlock(id: number): Promise<GoodsDisplayBlock> {
  return request<GoodsDisplayBlock>({
    url: `/admin-api/goods/display-block/${id}`,
    method: 'get'
  })
}

/** 新增展示板块：POST /admin-api/goods/display-block（返回新 id） */
export function createGoodsDisplayBlock(data: GoodsDisplayBlockPayload): Promise<number> {
  return request<number>({
    url: '/admin-api/goods/display-block',
    method: 'post',
    data
  })
}

/** 修改展示板块：PUT /admin-api/goods/display-block/{id} */
export function updateGoodsDisplayBlock(id: number, data: GoodsDisplayBlockPayload): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/display-block/${id}`,
    method: 'put',
    data
  })
}

/** 删除展示板块：DELETE /admin-api/goods/display-block/{id} */
export function deleteGoodsDisplayBlock(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/display-block/${id}`,
    method: 'delete'
  })
}
