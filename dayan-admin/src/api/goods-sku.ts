import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  GoodsSkuEquity,
  GoodsSkuEquityQuery,
  GoodsSkuScene,
  GoodsSkuSceneQuery,
  GoodsSkuCourse,
  GoodsSkuCourseQuery,
  GoodsSkuSojourn,
  GoodsSkuSojournQuery
} from '@/types/goods'

/**
 * 商品域 SKU 子表接口封装（4 个子表）。
 *
 * 对应后端 GoodsSkuEquityAdminController / GoodsSkuSceneAdminController /
 * GoodsSkuCourseAdminController / GoodsSkuSojournAdminController，
 * 全部挂在 /admin-api 前缀下，每个子表标准 CRUD 5 端点（无特殊端点）。
 *
 * 公共契约（4 子表一致）：
 * - 主键：物理 id（自增 number，非雪花），update/delete/{id} 都用 id。
 * - 业务键：skuCode（服务端生成 GE/GS/GC/GJ 前缀，前端不传）。
 * - 关联键：goodsCode（弱外键，无 DB 约束）。
 * - list 入参是单参 goodsCode（不是 query DTO），返回 List 非分页。
 * - create 返回 number（id），不是 skuCode（与主表返回 goodsCode 不同）。
 * - salesCount create 时硬编码 0，UpdateDTO 无此字段，前端只读。
 * - sortOrder 默认 0，status 默认 1（在售）。
 *
 * 已知遗留（RBAC seed 缺口）：4 个 SKU Controller 的 20 个权限码
 * （goods:sku-equity:list / goods:sku-scene:list / goods:sku-course:list / goods:sku-sojourn:list 等）
 * 在 db/migration/seed 未定义，非超管角色调子表端点会 403。超管可旁路。
 * 这是后端/DB 任务，前端不在本任务范围修。
 */

// ============================================================================
// 1. 权益规格（sku-equity，skuCode 前缀 GE）
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
// 2. 场景规格（sku-scene，skuCode 前缀 GS）
// ============================================================================

/** 场景规格分页：GET /admin-api/goods/sku-scene/page */
export function pageSkuScenes(query: GoodsSkuSceneQuery): Promise<PageResult<GoodsSkuScene>> {
  return request<PageResult<GoodsSkuScene>>({
    url: '/admin-api/goods/sku-scene/page',
    method: 'get',
    params: query
  })
}

/** 场景规格列表（按 goodsCode 过滤，非分页）：GET /admin-api/goods/sku-scene/list */
export function listSkuScenes(goodsCode: string): Promise<GoodsSkuScene[]> {
  return request<GoodsSkuScene[]>({
    url: '/admin-api/goods/sku-scene/list',
    method: 'get',
    params: { goodsCode }
  })
}

/** 场景规格详情：GET /admin-api/goods/sku-scene/{id} */
export function getSkuScene(id: number): Promise<GoodsSkuScene> {
  return request<GoodsSkuScene>({
    url: `/admin-api/goods/sku-scene/${id}`,
    method: 'get'
  })
}

/** 新增场景规格：POST /admin-api/goods/sku-scene（返回 id） */
export function createSkuScene(data: Partial<GoodsSkuScene>): Promise<number> {
  return request<number>({
    url: '/admin-api/goods/sku-scene',
    method: 'post',
    data
  })
}

/** 修改场景规格：PUT /admin-api/goods/sku-scene/{id} */
export function updateSkuScene(id: number, data: Partial<GoodsSkuScene>): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/sku-scene/${id}`,
    method: 'put',
    data
  })
}

/** 删除场景规格：DELETE /admin-api/goods/sku-scene/{id} */
export function deleteSkuScene(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/sku-scene/${id}`,
    method: 'delete'
  })
}

// ============================================================================
// 3. 课程规格（sku-course，skuCode 前缀 GC）
// ============================================================================

/** 课程规格分页：GET /admin-api/goods/sku-course/page */
export function pageSkuCourses(query: GoodsSkuCourseQuery): Promise<PageResult<GoodsSkuCourse>> {
  return request<PageResult<GoodsSkuCourse>>({
    url: '/admin-api/goods/sku-course/page',
    method: 'get',
    params: query
  })
}

/** 课程规格列表（按 goodsCode 过滤，非分页）：GET /admin-api/goods/sku-course/list */
export function listSkuCourses(goodsCode: string): Promise<GoodsSkuCourse[]> {
  return request<GoodsSkuCourse[]>({
    url: '/admin-api/goods/sku-course/list',
    method: 'get',
    params: { goodsCode }
  })
}

/** 课程规格详情：GET /admin-api/goods/sku-course/{id} */
export function getSkuCourse(id: number): Promise<GoodsSkuCourse> {
  return request<GoodsSkuCourse>({
    url: `/admin-api/goods/sku-course/${id}`,
    method: 'get'
  })
}

/** 新增课程规格：POST /admin-api/goods/sku-course（返回 id） */
export function createSkuCourse(data: Partial<GoodsSkuCourse>): Promise<number> {
  return request<number>({
    url: '/admin-api/goods/sku-course',
    method: 'post',
    data
  })
}

/** 修改课程规格：PUT /admin-api/goods/sku-course/{id} */
export function updateSkuCourse(id: number, data: Partial<GoodsSkuCourse>): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/sku-course/${id}`,
    method: 'put',
    data
  })
}

/** 删除课程规格：DELETE /admin-api/goods/sku-course/{id} */
export function deleteSkuCourse(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/sku-course/${id}`,
    method: 'delete'
  })
}

// ============================================================================
// 4. 旅居规格（sku-sojourn，skuCode 前缀 GJ）
// ============================================================================

/** 旅居规格分页：GET /admin-api/goods/sku-sojourn/page */
export function pageSkuSojourns(query: GoodsSkuSojournQuery): Promise<PageResult<GoodsSkuSojourn>> {
  return request<PageResult<GoodsSkuSojourn>>({
    url: '/admin-api/goods/sku-sojourn/page',
    method: 'get',
    params: query
  })
}

/** 旅居规格列表（按 goodsCode 过滤，非分页）：GET /admin-api/goods/sku-sojourn/list */
export function listSkuSojourns(goodsCode: string): Promise<GoodsSkuSojourn[]> {
  return request<GoodsSkuSojourn[]>({
    url: '/admin-api/goods/sku-sojourn/list',
    method: 'get',
    params: { goodsCode }
  })
}

/** 旅居规格详情：GET /admin-api/goods/sku-sojourn/{id} */
export function getSkuSojourn(id: number): Promise<GoodsSkuSojourn> {
  return request<GoodsSkuSojourn>({
    url: `/admin-api/goods/sku-sojourn/${id}`,
    method: 'get'
  })
}

/** 新增旅居规格：POST /admin-api/goods/sku-sojourn（返回 id） */
export function createSkuSojourn(data: Partial<GoodsSkuSojourn>): Promise<number> {
  return request<number>({
    url: '/admin-api/goods/sku-sojourn',
    method: 'post',
    data
  })
}

/** 修改旅居规格：PUT /admin-api/goods/sku-sojourn/{id} */
export function updateSkuSojourn(id: number, data: Partial<GoodsSkuSojourn>): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/sku-sojourn/${id}`,
    method: 'put',
    data
  })
}

/** 删除旅居规格：DELETE /admin-api/goods/sku-sojourn/{id} */
export function deleteSkuSojourn(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/goods/sku-sojourn/${id}`,
    method: 'delete'
  })
}
