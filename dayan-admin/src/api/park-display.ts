import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ParkDisplayBlock, ParkDisplayBlockPayload, ParkDisplayBlockQuery } from '@/types/park'

/**
 * 机构展示板块（ParkDisplayBlock）接口封装。
 *
 * 对应后端 /admin-api/park/display-block/*。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - blockType 必填（决定 C 端渲染板块）；blockType 不可修改。
 * - images / imageDescriptions 是 JSON 数组字符串（后端 TEXT 列存）。
 */

/** 展示板块分页：GET /admin-api/park/display-block/page */
export function pageDisplayBlocks(query: ParkDisplayBlockQuery): Promise<PageResult<ParkDisplayBlock>> {
  return request<PageResult<ParkDisplayBlock>>({
    url: '/admin-api/park/display-block/page',
    method: 'get',
    params: query
  })
}

/** 展示板块列表（按机构，仅状态=1）：GET /admin-api/park/display-block/list?parkCode=xxx */
export function listDisplayBlocks(parkCode: string): Promise<ParkDisplayBlock[]> {
  return request<ParkDisplayBlock[]>({
    url: '/admin-api/park/display-block/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 展示板块详情：GET /admin-api/park/display-block/{id} */
export function getDisplayBlock(id: number): Promise<ParkDisplayBlock> {
  return request<ParkDisplayBlock>({
    url: `/admin-api/park/display-block/${id}`,
    method: 'get'
  })
}

/** 新增展示板块：POST /admin-api/park/display-block（返回新 id） */
export function createDisplayBlock(data: ParkDisplayBlockPayload): Promise<number> {
  return request<number>({
    url: '/admin-api/park/display-block',
    method: 'post',
    data
  })
}

/** 修改展示板块：PUT /admin-api/park/display-block/{id} */
export function updateDisplayBlock(id: number, data: ParkDisplayBlockPayload): Promise<void> {
  return request<void>({
    url: `/admin-api/park/display-block/${id}`,
    method: 'put',
    data
  })
}

/** 删除展示板块：DELETE /admin-api/park/display-block/{id} */
export function deleteDisplayBlock(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/display-block/${id}`,
    method: 'delete'
  })
}
