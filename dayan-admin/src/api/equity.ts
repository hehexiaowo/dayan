import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  EquityBatch,
  EquityBatchQuery,
  EquityBatchStats,
  EquityDepot,
  EquityDepotQuery,
  EquityActivate,
  EquityActivateQuery,
  EquityChangeHolder,
  EquityChangeHolderQuery,
  EquityUsePerson,
  EquityUsePersonQuery,
  SetDefaultHolderPayload
} from '@/types/equity'

/**
 * 权益域接口封装。
 *
 * 对应后端 Admin 控制器（均挂在 /admin-api 前缀下）：
 * - EquityBatchAdminController（/admin-api/equity/batch/*）
 * - EquityDepotAdminController（/admin-api/equity/depot/*）
 * - EquityActivateAdminController（/admin-api/equity/activate/*，仅查询）
 * - EquityChangeHolderAdminController（/admin-api/equity/change-holder/*，仅查询）
 * - EquityUsePersonAdminController（/admin-api/equity/use-person/*，CRUD + set-default）
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

// ============================================================
// 权益激活记录（/admin-api/equity/activate，仅查询）
// ============================================================
// 记录由 depot.activate 生命周期自动产生，管理端不直接新增/修改/删除。

/** 激活记录分页：GET /admin-api/equity/activate/page */
export function pageEquityActivates(query: EquityActivateQuery): Promise<PageResult<EquityActivate>> {
  return request<PageResult<EquityActivate>>({
    url: '/admin-api/equity/activate/page',
    method: 'get',
    params: query
  })
}

/** 激活记录列表（全量）：GET /admin-api/equity/activate/list */
export function listEquityActivates(query: EquityActivateQuery): Promise<EquityActivate[]> {
  return request<EquityActivate[]>({
    url: '/admin-api/equity/activate/list',
    method: 'get',
    params: query
  })
}

/** 按权益编码查激活记录：GET /admin-api/equity/activate/{equityCode} */
export function getEquityActivate(equityCode: string): Promise<EquityActivate> {
  return request<EquityActivate>({
    url: `/admin-api/equity/activate/${equityCode}`,
    method: 'get'
  })
}

// ============================================================
// 权益更换权益人记录（/admin-api/equity/change-holder，仅查询）
// ============================================================
// 记录由 depot 换持有人生命周期自动产生，发起/完成/回滚在 /equity/depot 下。

/** 更换记录分页：GET /admin-api/equity/change-holder/page */
export function pageEquityChangeHolders(
  query: EquityChangeHolderQuery
): Promise<PageResult<EquityChangeHolder>> {
  return request<PageResult<EquityChangeHolder>>({
    url: '/admin-api/equity/change-holder/page',
    method: 'get',
    params: query
  })
}

/** 按权益编码列出更换历史：GET /admin-api/equity/change-holder/list-by-equity/{equityCode} */
export function listEquityChangeHoldersByEquity(equityCode: string): Promise<EquityChangeHolder[]> {
  return request<EquityChangeHolder[]>({
    url: `/admin-api/equity/change-holder/list-by-equity/${equityCode}`,
    method: 'get'
  })
}

/** 更换记录详情：GET /admin-api/equity/change-holder/{id} */
export function getEquityChangeHolder(id: string): Promise<EquityChangeHolder> {
  return request<EquityChangeHolder>({
    url: `/admin-api/equity/change-holder/${id}`,
    method: 'get'
  })
}

// ============================================================
// 权益使用人（/admin-api/equity/use-person，CRUD + set-default）
// ============================================================
// id 序列化为字符串（雪花ID），故 update/remove 接收 string。

/** 使用人分页：GET /admin-api/equity/use-person/page */
export function pageEquityUsePersons(query: EquityUsePersonQuery): Promise<PageResult<EquityUsePerson>> {
  return request<PageResult<EquityUsePerson>>({
    url: '/admin-api/equity/use-person/page',
    method: 'get',
    params: query
  })
}

/** 按权益编码列出全部使用人：GET /admin-api/equity/use-person/list-by-equity/{equityCode} */
export function listEquityUsePersonsByEquity(equityCode: string): Promise<EquityUsePerson[]> {
  return request<EquityUsePerson[]>({
    url: `/admin-api/equity/use-person/list-by-equity/${equityCode}`,
    method: 'get'
  })
}

/** 使用人详情：GET /admin-api/equity/use-person/{id} */
export function getEquityUsePerson(id: string): Promise<EquityUsePerson> {
  return request<EquityUsePerson>({
    url: `/admin-api/equity/use-person/${id}`,
    method: 'get'
  })
}

/** 登记使用人：POST /admin-api/equity/use-person（返回新 id） */
export function createEquityUsePerson(data: Partial<EquityUsePerson>): Promise<string> {
  return request<string>({
    url: '/admin-api/equity/use-person',
    method: 'post',
    data
  })
}

/** 修改使用人：PUT /admin-api/equity/use-person/{id} */
export function updateEquityUsePerson(id: string, data: Partial<EquityUsePerson>): Promise<void> {
  return request<void>({
    url: `/admin-api/equity/use-person/${id}`,
    method: 'put',
    data
  })
}

/** 删除使用人：DELETE /admin-api/equity/use-person/{id} */
export function deleteEquityUsePerson(id: string): Promise<void> {
  return request<void>({
    url: `/admin-api/equity/use-person/${id}`,
    method: 'delete'
  })
}

/**
 * 设置默认权益人：POST /admin-api/equity/use-person/set-default
 *
 * 将指定使用人置为默认（is_default_holder=1），同 equity_code 下其它使用人置 0。
 */
export function setDefaultEquityUsePerson(payload: SetDefaultHolderPayload): Promise<void> {
  return request<void>({
    url: '/admin-api/equity/use-person/set-default',
    method: 'post',
    data: payload
  })
}
