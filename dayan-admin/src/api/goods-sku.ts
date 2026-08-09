import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  GoodsSkuEquity,
  GoodsSkuEquityQuery,
  GoodsScene,
  GoodsSceneQuery,
  GoodsCourse,
  GoodsCourseQuery,
  GoodsSojourn,
  GoodsSojournQuery
} from '@/types/goods'

/**
 * 商品域子表接口封装。
 *
 * 权益规格（sku-equity）使用旧路径 /admin-api/goods/sku-equity/*（保留）。
 * 场景/课程/旅居配置已重命名为 /admin-api/goods/scene/course/sojourn（无 sku 前缀）。
 *
 * 公共契约（4 子表一致）：
 * - 主键：物理 id（自增 number，非雪花），update/delete/{id} 都用 id。
 * - 业务键：skuCode（服务端生成 GE/GS/GC/GJ 前缀，前端不传）。
 * - 关联键：goodsCode（弱外键，无 DB 约束）。
 * - list 入参是单参 goodsCode（不是 query DTO），返回 List 非分页。
 * - create 返回 number（id），不是 skuCode（与主表返回 goodsCode 不同）。
 * - salesCount create 时硬编码 0，UpdateDTO 无此字段，前端只读。
 * - sortOrder 默认 0，status 默认 1（在售）。
 */

// ============================================================================
// 1. 权益规格（sku-equity，skuCode 前缀 GE）— 保留旧路径，增量5将删除
// ============================================================================

/** 权益规格分页：GET /admin-api/goods/sku-equity/page */
export function pageSkuEquities(query: GoodsSkuEquityQuery): Promise<PageResult<GoodsSkuEquity>> {
  return request<PageResult<GoodsSkuEquity>>({
    url: '/admin-api/goods/sku-equity/page',
    method: 'get',
    params: query
  })
}

/** 权益规格列表（按 goodsCode 过滤，非分页）：GET /admin-api/goods/sku-equity/list */
export function listSkuEquities(goodsCode: string): Promise<GoodsSkuEquity[]> {
  return request<GoodsSkuEquity[]>({
    url: '/admin-api/goods/sku-equity/list',
    method: 'get',
    params: { goodsCode }
  })
}

/** 权益规格详情：GET /admin-api/goods/sku-equity/{id} */
export function getSkuEquity(id: number): Promise<GoodsSkuEquity> {
  return request<GoodsSkuEquity>({
    url: `/admin-api/goods/sku-equity/${id}`,
    method: 'get'
  })
}

/** 新增权益规格：POST /admin-api/goods/sku-equity（返回 id） */
export function createSkuEquity(data: Partial<GoodsSkuEquity>): Promise<number> {
  return request<number>({
    url: '/admin-api/goods/sku-equity',
    method: 'post',
    data
  })
}

/** 修改权益规格：PUT /admin-api/goods/sku-equity/{id} */
export function updateSkuEquity(id: number, data: Partial<GoodsSkuEquity>): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/sku-equity/${id}`,
    method: 'put',
    data
  })
}

/** 删除权益规格：DELETE /admin-api/goods/sku-equity/{id} */
export function deleteSkuEquity(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/sku-equity/${id}`,
    method: 'delete'
  })
}

// ============================================================================
// 2. 场景配置（scene，skuCode 前缀 GS）
// ============================================================================

/** 场景配置分页：GET /admin-api/goods/scene/page */
export function pageScenes(query: GoodsSceneQuery): Promise<PageResult<GoodsScene>> {
  return request<PageResult<GoodsScene>>({
    url: '/admin-api/goods/scene/page',
    method: 'get',
    params: query
  })
}

/** 场景配置列表（按 goodsCode 过滤，非分页）：GET /admin-api/goods/scene/list */
export function listScenes(goodsCode: string): Promise<GoodsScene[]> {
  return request<GoodsScene[]>({
    url: '/admin-api/goods/scene/list',
    method: 'get',
    params: { goodsCode }
  })
}

/** 场景配置详情：GET /admin-api/goods/scene/{id} */
export function getScene(id: number): Promise<GoodsScene> {
  return request<GoodsScene>({
    url: `/admin-api/goods/scene/${id}`,
    method: 'get'
  })
}

/** 新增场景配置：POST /admin-api/goods/scene（返回 id） */
export function createScene(data: Partial<GoodsScene>): Promise<number> {
  return request<number>({
    url: '/admin-api/goods/scene',
    method: 'post',
    data
  })
}

/** 修改场景配置：PUT /admin-api/goods/scene/{id} */
export function updateScene(id: number, data: Partial<GoodsScene>): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/scene/${id}`,
    method: 'put',
    data
  })
}

/** 删除场景配置：DELETE /admin-api/goods/scene/{id} */
export function deleteScene(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/scene/${id}`,
    method: 'delete'
  })
}

// ============================================================================
// 3. 课程配置（course，skuCode 前缀 GC）
// ============================================================================

/** 课程配置分页：GET /admin-api/goods/course/page */
export function pageCourses(query: GoodsCourseQuery): Promise<PageResult<GoodsCourse>> {
  return request<PageResult<GoodsCourse>>({
    url: '/admin-api/goods/course/page',
    method: 'get',
    params: query
  })
}

/** 课程配置列表（按 goodsCode 过滤，非分页）：GET /admin-api/goods/course/list */
export function listCourses(goodsCode: string): Promise<GoodsCourse[]> {
  return request<GoodsCourse[]>({
    url: '/admin-api/goods/course/list',
    method: 'get',
    params: { goodsCode }
  })
}

/** 课程配置详情：GET /admin-api/goods/course/{id} */
export function getCourse(id: number): Promise<GoodsCourse> {
  return request<GoodsCourse>({
    url: `/admin-api/goods/course/${id}`,
    method: 'get'
  })
}

/** 新增课程配置：POST /admin-api/goods/course（返回 id） */
export function createCourse(data: Partial<GoodsCourse>): Promise<number> {
  return request<number>({
    url: '/admin-api/goods/course',
    method: 'post',
    data
  })
}

/** 修改课程配置：PUT /admin-api/goods/course/{id} */
export function updateCourse(id: number, data: Partial<GoodsCourse>): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/course/${id}`,
    method: 'put',
    data
  })
}

/** 删除课程配置：DELETE /admin-api/goods/course/{id} */
export function deleteCourse(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/course/${id}`,
    method: 'delete'
  })
}

// ============================================================================
// 4. 旅居配置（sojourn，skuCode 前缀 GJ）
// ============================================================================

/** 旅居配置分页：GET /admin-api/goods/sojourn/page */
export function pageSojourns(query: GoodsSojournQuery): Promise<PageResult<GoodsSojourn>> {
  return request<PageResult<GoodsSojourn>>({
    url: '/admin-api/goods/sojourn/page',
    method: 'get',
    params: query
  })
}

/** 旅居配置列表（按 goodsCode 过滤，非分页）：GET /admin-api/goods/sojourn/list */
export function listSojourns(goodsCode: string): Promise<GoodsSojourn[]> {
  return request<GoodsSojourn[]>({
    url: '/admin-api/goods/sojourn/list',
    method: 'get',
    params: { goodsCode }
  })
}

/** 旅居配置详情：GET /admin-api/goods/sojourn/{id} */
export function getSojourn(id: number): Promise<GoodsSojourn> {
  return request<GoodsSojourn>({
    url: `/admin-api/goods/sojourn/${id}`,
    method: 'get'
  })
}

/** 新增旅居配置：POST /admin-api/goods/sojourn（返回 id） */
export function createSojourn(data: Partial<GoodsSojourn>): Promise<number> {
  return request<number>({
    url: '/admin-api/goods/sojourn',
    method: 'post',
    data
  })
}

/** 修改旅居配置：PUT /admin-api/goods/sojourn/{id} */
export function updateSojourn(id: number, data: Partial<GoodsSojourn>): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/sojourn/${id}`,
    method: 'put',
    data
  })
}

/** 删除旅居配置：DELETE /admin-api/goods/sojourn/{id} */
export function deleteSojourn(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/sojourn/${id}`,
    method: 'delete'
  })
}
