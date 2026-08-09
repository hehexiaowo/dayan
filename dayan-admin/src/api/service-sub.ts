import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ServiceEvaluation,
  ServiceEvaluationQuery,
  ServiceEquityDemand,
  ServiceEquityDemandQuery,
  ServiceEquitySolution,
  ServiceEquitySolutionQuery,
  ServiceEquityArrange,
  ServiceEquityArrangeQuery,
  ServiceEquityFollowup,
  ServiceEquityFollowupQuery
} from '@/types/service'

/**
 * 服务会话子表接口封装（评价 + 权益需求 + 权益方案 + 全程安排 + 回访品控）。
 *
 * 对应后端 5 个 admin Controller：
 * - /admin-api/service/evaluation  （ServiceEvaluationAdminController，1:1 一会话一评价）
 * - /admin-api/service/demand      （ServiceEquityDemandAdminController）
 * - /admin-api/service/solution    （ServiceEquitySolutionAdminController，含 /accept）
 * - /admin-api/service/arrange     （ServiceEquityArrangeAdminController，含 /confirm）
 * - /admin-api/service/followup    （ServiceEquityFollowupAdminController）
 *
 * 主键约定：
 * - ServiceEvaluation：无业务 code，路径用 Long id（useCrud 传 idKey:'id'）。
 * - ServiceEquityDemand/Solution/Arrange/Followup：有业务 code（DM/SO/AR/FU 前缀），
 *   路径参数用 xxxCode（useCrud 传 idKey:'demandCode' 等），避免雪花 Long 精度溢出。
 *
 * list 端点差异（务必注意）：
 * - evaluation：list 接 QueryDTO（sessionCode/butlerCode/parkCode 等均可过滤）。
 * - demand/solution/arrange/followup：list 只接 @RequestParam String sessionCode。
 *
 * 业务链端点：
 * - solution /accept：POST，body SolutionAcceptDTO { id, isAccepted:0/1/2, clientFeedback? }。
 *   会话 confirm_solution 前须存在 isAccepted=1 的方案。
 * - arrange /confirm：POST，body ArrangeConfirmDTO { id, isConfirmed:0/1 }。
 *   isConfirmed=1 后自动写 confirmTime，方可触发会话 start_service。
 */

// ==================== 服务评价（service/evaluation，1:1）====================

/** 评价分页：GET /admin-api/service/evaluation/page */
export function pageServiceEvaluations(query: ServiceEvaluationQuery): Promise<PageResult<ServiceEvaluation>> {
  return request<PageResult<ServiceEvaluation>>({
    url: '/admin-api/service/evaluation/page',
    method: 'get',
    params: query
  })
}

/** 评价列表：GET /admin-api/service/evaluation/list（接 QueryDTO，一会话至多 1 条） */
export function listServiceEvaluations(query: Partial<ServiceEvaluationQuery>): Promise<ServiceEvaluation[]> {
  return request<ServiceEvaluation[]>({
    url: '/admin-api/service/evaluation/list',
    method: 'get',
    params: query
  })
}

/** 评价详情：GET /admin-api/service/evaluation/{id} */
export function getServiceEvaluation(id: number): Promise<ServiceEvaluation> {
  return request<ServiceEvaluation>({
    url: `/admin-api/service/evaluation/${id}`,
    method: 'get'
  })
}

/** 新增评价：POST /admin-api/service/evaluation（返回 id，重复将抛业务异常） */
export function createServiceEvaluation(data: Partial<ServiceEvaluation>): Promise<number> {
  return request<number>({
    url: '/admin-api/service/evaluation',
    method: 'post',
    data
  })
}

/** 修改评价（含回复）：PUT /admin-api/service/evaluation/{id} */
export function updateServiceEvaluation(id: number, data: Partial<ServiceEvaluation>): Promise<void> {
  return request<void>({
    url: `/admin-api/service/evaluation/${id}`,
    method: 'put',
    data
  })
}

/** 删除评价：DELETE /admin-api/service/evaluation/{id} */
export function deleteServiceEvaluation(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/service/evaluation/${id}`,
    method: 'delete'
  })
}

// ==================== 权益需求（service/demand）====================

/** 需求分页：GET /admin-api/service/demand/page */
export function pageServiceEquityDemands(query: ServiceEquityDemandQuery): Promise<PageResult<ServiceEquityDemand>> {
  return request<PageResult<ServiceEquityDemand>>({
    url: '/admin-api/service/demand/page',
    method: 'get',
    params: query
  })
}

/** 会话下需求列表：GET /admin-api/service/demand/list?sessionCode=xxx */
export function listServiceEquityDemands(sessionCode: string): Promise<ServiceEquityDemand[]> {
  return request<ServiceEquityDemand[]>({
    url: '/admin-api/service/demand/list',
    method: 'get',
    params: { sessionCode }
  })
}

/** 需求详情：GET /admin-api/service/demand/{demandCode} */
export function getServiceEquityDemand(demandCode: string): Promise<ServiceEquityDemand> {
  return request<ServiceEquityDemand>({
    url: `/admin-api/service/demand/${demandCode}`,
    method: 'get'
  })
}

/** 新增需求：POST /admin-api/service/demand（返回 demandCode） */
export function createServiceEquityDemand(data: Partial<ServiceEquityDemand>): Promise<string> {
  return request<string>({
    url: '/admin-api/service/demand',
    method: 'post',
    data
  })
}

/** 修改需求：PUT /admin-api/service/demand/{demandCode} */
export function updateServiceEquityDemand(demandCode: string, data: Partial<ServiceEquityDemand>): Promise<void> {
  return request<void>({
    url: `/admin-api/service/demand/${demandCode}`,
    method: 'put',
    data
  })
}

/** 删除需求：DELETE /admin-api/service/demand/{demandCode} */
export function deleteServiceEquityDemand(demandCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/service/demand/${demandCode}`,
    method: 'delete'
  })
}

// ==================== 权益方案（service/solution，含 accept）====================

/** 方案分页：GET /admin-api/service/solution/page */
export function pageServiceEquitySolutions(query: ServiceEquitySolutionQuery): Promise<PageResult<ServiceEquitySolution>> {
  return request<PageResult<ServiceEquitySolution>>({
    url: '/admin-api/service/solution/page',
    method: 'get',
    params: query
  })
}

/** 会话下方案列表：GET /admin-api/service/solution/list?sessionCode=xxx */
export function listServiceEquitySolutions(sessionCode: string): Promise<ServiceEquitySolution[]> {
  return request<ServiceEquitySolution[]>({
    url: '/admin-api/service/solution/list',
    method: 'get',
    params: { sessionCode }
  })
}

/** 方案详情：GET /admin-api/service/solution/{solutionCode} */
export function getServiceEquitySolution(solutionCode: string): Promise<ServiceEquitySolution> {
  return request<ServiceEquitySolution>({
    url: `/admin-api/service/solution/${solutionCode}`,
    method: 'get'
  })
}

/** 新增方案：POST /admin-api/service/solution（返回 solutionCode） */
export function createServiceEquitySolution(data: Partial<ServiceEquitySolution>): Promise<string> {
  return request<string>({
    url: '/admin-api/service/solution',
    method: 'post',
    data
  })
}

/** 修改方案：PUT /admin-api/service/solution/{solutionCode} */
export function updateServiceEquitySolution(solutionCode: string, data: Partial<ServiceEquitySolution>): Promise<void> {
  return request<void>({
    url: `/admin-api/service/solution/${solutionCode}`,
    method: 'put',
    data
  })
}

/**
 * 方案接受/拒绝标记：POST /admin-api/service/solution/accept。
 *
 * body SolutionAcceptDTO { solutionCode, isAccepted:0/1/2, clientFeedback? }。
 * 会话 confirm_solution 前须存在 isAccepted=1 的方案。
 */
export function acceptServiceEquitySolution(
  solutionCode: string,
  isAccepted: number,
  clientFeedback?: string
): Promise<void> {
  return request<void>({
    url: '/admin-api/service/solution/accept',
    method: 'post',
    data: { solutionCode, isAccepted, clientFeedback }
  })
}

/** 删除方案：DELETE /admin-api/service/solution/{solutionCode} */
export function deleteServiceEquitySolution(solutionCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/service/solution/${solutionCode}`,
    method: 'delete'
  })
}

// ==================== 全程安排（service/arrange，含 confirm）====================

/** 安排分页：GET /admin-api/service/arrange/page */
export function pageServiceEquityArranges(query: ServiceEquityArrangeQuery): Promise<PageResult<ServiceEquityArrange>> {
  return request<PageResult<ServiceEquityArrange>>({
    url: '/admin-api/service/arrange/page',
    method: 'get',
    params: query
  })
}

/** 会话下安排列表：GET /admin-api/service/arrange/list?sessionCode=xxx */
export function listServiceEquityArranges(sessionCode: string): Promise<ServiceEquityArrange[]> {
  return request<ServiceEquityArrange[]>({
    url: '/admin-api/service/arrange/list',
    method: 'get',
    params: { sessionCode }
  })
}

/** 安排详情：GET /admin-api/service/arrange/{arrangeCode} */
export function getServiceEquityArrange(arrangeCode: string): Promise<ServiceEquityArrange> {
  return request<ServiceEquityArrange>({
    url: `/admin-api/service/arrange/${arrangeCode}`,
    method: 'get'
  })
}

/** 新增安排：POST /admin-api/service/arrange（返回 arrangeCode） */
export function createServiceEquityArrange(data: Partial<ServiceEquityArrange>): Promise<string> {
  return request<string>({
    url: '/admin-api/service/arrange',
    method: 'post',
    data
  })
}

/** 修改安排：PUT /admin-api/service/arrange/{arrangeCode} */
export function updateServiceEquityArrange(arrangeCode: string, data: Partial<ServiceEquityArrange>): Promise<void> {
  return request<void>({
    url: `/admin-api/service/arrange/${arrangeCode}`,
    method: 'put',
    data
  })
}

/**
 * 确认安排：POST /admin-api/service/arrange/confirm。
 *
 * body ArrangeConfirmDTO { arrangeCode, isConfirmed:0/1 }。
 * isConfirmed=1 后自动写 confirmTime，方可触发会话 start_service。
 */
export function confirmServiceEquityArrange(arrangeCode: string, isConfirmed: number): Promise<void> {
  return request<void>({
    url: '/admin-api/service/arrange/confirm',
    method: 'post',
    data: { arrangeCode, isConfirmed }
  })
}

/** 删除安排：DELETE /admin-api/service/arrange/{arrangeCode} */
export function deleteServiceEquityArrange(arrangeCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/service/arrange/${arrangeCode}`,
    method: 'delete'
  })
}

// ==================== 回访品控（service/followup）====================

/** 回访分页：GET /admin-api/service/followup/page */
export function pageServiceEquityFollowups(query: ServiceEquityFollowupQuery): Promise<PageResult<ServiceEquityFollowup>> {
  return request<PageResult<ServiceEquityFollowup>>({
    url: '/admin-api/service/followup/page',
    method: 'get',
    params: query
  })
}

/** 会话下回访列表：GET /admin-api/service/followup/list?sessionCode=xxx */
export function listServiceEquityFollowups(sessionCode: string): Promise<ServiceEquityFollowup[]> {
  return request<ServiceEquityFollowup[]>({
    url: '/admin-api/service/followup/list',
    method: 'get',
    params: { sessionCode }
  })
}

/** 回访详情：GET /admin-api/service/followup/{followupCode} */
export function getServiceEquityFollowup(followupCode: string): Promise<ServiceEquityFollowup> {
  return request<ServiceEquityFollowup>({
    url: `/admin-api/service/followup/${followupCode}`,
    method: 'get'
  })
}

/** 新增回访：POST /admin-api/service/followup（返回 followupCode，status 固定 2） */
export function createServiceEquityFollowup(data: Partial<ServiceEquityFollowup>): Promise<string> {
  return request<string>({
    url: '/admin-api/service/followup',
    method: 'post',
    data
  })
}

/** 修改回访：PUT /admin-api/service/followup/{followupCode} */
export function updateServiceEquityFollowup(followupCode: string, data: Partial<ServiceEquityFollowup>): Promise<void> {
  return request<void>({
    url: `/admin-api/service/followup/${followupCode}`,
    method: 'put',
    data
  })
}

/** 删除回访：DELETE /admin-api/service/followup/{followupCode} */
export function deleteServiceEquityFollowup(followupCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/service/followup/${followupCode}`,
    method: 'delete'
  })
}
