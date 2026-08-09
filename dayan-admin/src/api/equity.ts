import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  EquityBatch,
  EquityBatchQuery,
  EquityBatchStats,
  EquityDepot,
  EquityDepotQuery
} from '@/types/equity'

/**
 * 权益域接口封装。
 *
 * 对应后端两个 Admin 控制器（均挂在 /admin-api 前缀下）：
 * - EquityBatchAdminController（/admin-api/equity/batch/*）
 * - EquityDepotAdminController（/admin-api/equity/depot/*）
 */

// ============================================================
// 权益批次（/admin-api/equity/batch）
// ============================================================

/** 批次分页：GET /admin-api/equity/batch/page */
export function pageBatches(query: EquityBatchQuery): Promise<PageResult<EquityBatch>> {
  return request<PageResult<EquityBatch>>({
    url: '/admin-api/equity/batch/page',
    method: 'get',
    params: query
  })
}

/** 批次列表（不分页）：GET /admin-api/equity/batch/list */
export function listBatches(): Promise<EquityBatch[]> {
  return request<EquityBatch[]>({
    url: '/admin-api/equity/batch/list',
    method: 'get'
  })
}

/** 批次详情：GET /admin-api/equity/batch/{batchCode} */
export function getBatch(batchCode: string): Promise<EquityBatch> {
  return request<EquityBatch>({
    url: `/admin-api/equity/batch/${batchCode}`,
    method: 'get'
  })
}

/** 批次统计：GET /admin-api/equity/batch/stats/{batchCode}（与 /{batchCode} 等价） */
export function getBatchStats(batchCode: string): Promise<EquityBatchStats> {
  return request<EquityBatchStats>({
    url: `/admin-api/equity/batch/stats/${batchCode}`,
    method: 'get'
  })
}

/** 新增批次：POST /admin-api/equity/batch（返回 batchCode） */
export function createBatch(data: Partial<EquityBatch>): Promise<string> {
  return request<string>({
    url: '/admin-api/equity/batch',
    method: 'post',
    data
  })
}

/** 修改批次：PUT /admin-api/equity/batch/{batchCode} */
export function updateBatch(batchCode: string, data: Partial<EquityBatch>): Promise<void> {
  return request<void>({
    url: `/admin-api/equity/batch/${batchCode}`,
    method: 'put',
    data
  })
}

/** 删除批次：DELETE /admin-api/equity/batch/{batchCode}（仅未生产可删） */
export function deleteBatch(batchCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/equity/batch/${batchCode}`,
    method: 'delete'
  })
}

// ============================================================
// 权益仓库（/admin-api/equity/depot）
// ============================================================
//
// 第一版简化：业务端点（stock-in / outbound / activate / void）统一走 transition 通用方法
// （POST /transition?equityCode=&event=），复杂的 DTO body 端点暂未在前端实现。
// transition 的 event 名约定：stock-in / outbound / activate / void 等。

/** 权益分页：GET /admin-api/equity/depot/page */
export function pageDepots(query: EquityDepotQuery): Promise<PageResult<EquityDepot>> {
  return request<PageResult<EquityDepot>>({
    url: '/admin-api/equity/depot/page',
    method: 'get',
    params: query
  })
}

/** 权益列表（不分页）：GET /admin-api/equity/depot/list */
export function listDepots(): Promise<EquityDepot[]> {
  return request<EquityDepot[]>({
    url: '/admin-api/equity/depot/list',
    method: 'get'
  })
}

/** 权益详情：GET /admin-api/equity/depot/{equityCode} */
export function getDepot(equityCode: string): Promise<EquityDepot> {
  return request<EquityDepot>({
    url: `/admin-api/equity/depot/${equityCode}`,
    method: 'get'
  })
}

/** 修改权益：PUT /admin-api/equity/depot/{equityCode} */
export function updateDepot(equityCode: string, data: Partial<EquityDepot>): Promise<void> {
  return request<void>({
    url: `/admin-api/equity/depot/${equityCode}`,
    method: 'put',
    data
  })
}

/** 删除权益：DELETE /admin-api/equity/depot/{equityCode} */
export function deleteDepot(equityCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/equity/depot/${equityCode}`,
    method: 'delete'
  })
}

/**
 * 权益状态流转：POST /admin-api/equity/depot/transition?equityCode=&event=
 *
 * 通用状态机入口，event 示例：stock-in / outbound / activate / void /
 * start_service / end_service / complete / shelf_expire / expire 等。
 */
export function transitionDepot(equityCode: string, event: string): Promise<number> {
  return request<number>({
    url: '/admin-api/equity/depot/transition',
    method: 'post',
    params: { equityCode, event }
  })
}

// TODO（后续迭代）：以下业务端点使用专用 DTO body，第一版用 transition 替代。
// - stockIn(data: StockInDTO):  POST /admin-api/equity/depot/stock-in
// - outbound(data: OutboundDTO): POST /admin-api/equity/depot/outbound
// - activate(data: ActivateDTO): POST /admin-api/equity/depot/activate
// - voidEquity(data: VoidDTO):   POST /admin-api/equity/depot/void
