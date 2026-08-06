import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  AgentInfo,
  AgentInfoQuery,
  AgentAccount,
  AgentAccountQuery,
  AgentClientRel,
  AgentPerformance,
  AgentPerformanceSummary,
  AgentShareRecord,
  AgentFavorite
} from '@/types/agent'

/**
 * 代理人接口封装。
 *
 * 对应后端 AgentInfoController（/admin-api/agents/*），RESTful 复数风格。
 *
 * 注意：list 接口返回 PageResult，但 url 无 /page 后缀（直接 GET /admin-api/agents）。
 * 主键 agentCode 由服务端生成。
 */

/** 代理人分页：GET /admin-api/agents（url 无 /page 后缀） */
export function pageAgents(query: AgentInfoQuery): Promise<PageResult<AgentInfo>> {
  return request<PageResult<AgentInfo>>({
    url: '/admin-api/agents',
    method: 'get',
    params: query
  })
}

/** 代理人详情：GET /admin-api/agents/{agentCode} */
export function getAgent(agentCode: string): Promise<AgentInfo> {
  return request<AgentInfo>({
    url: `/admin-api/agents/${agentCode}`,
    method: 'get'
  })
}

/** 新增代理人：POST /admin-api/agents */
export function createAgent(data: Partial<AgentInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/agents',
    method: 'post',
    data
  })
}

/** 修改代理人：PUT /admin-api/agents/{agentCode} */
export function updateAgent(agentCode: string, data: Partial<AgentInfo>): Promise<void> {
  return request<void>({
    url: `/admin-api/agents/${agentCode}`,
    method: 'put',
    data
  })
}

/** 删除代理人：DELETE /admin-api/agents/{agentCode} */
export function deleteAgent(agentCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/agents/${agentCode}`,
    method: 'delete'
  })
}

// ============================================================================
// 子表接口（代理人域 5 张子表，对齐后端 dayan-module-agent）
//
// 主键混合模式（重要）：
// - Account：业务主键 agentCode（非 id）。get/update/delete/reset-password 都用 agentCode。
//   一代理人一账号（1:1）。
// - ClientRel：雪花 id string。无 update；bind(POST) / unbind(PUT /{id}/unbind)。
// - Performance：雪花 id string。只增不改不删；create 返回 Void。
// - ShareRecord：雪花 id string。只增不改不删；create 返回 shareCode string。
// - Favorite：雪花 id string。幂等 add；remove 路径用 id。无 update。
//
// 返回类型规则：
// - by-agent 端点：Promise<VO[]>（非分页）。
// - get 单条：Promise<VO>（Account 一代理人一账号）。
// ============================================================================

// ---------------- 1. 代理人账号（agent-accounts）----------------

/** 代理人账号分页：GET /admin-api/agent-accounts */
export function pageAgentAccounts(query: AgentAccountQuery): Promise<PageResult<AgentAccount>> {
  return request<PageResult<AgentAccount>>({
    url: '/admin-api/agent-accounts',
    method: 'get',
    params: query
  })
}

/**
 * 代理人账号详情：GET /admin-api/agent-accounts/{agentCode}。
 * 一代理人一账号（1:1），不存在时后端返回 null 或 404。
 */
export function getAgentAccount(agentCode: string): Promise<AgentAccount | null> {
  return request<AgentAccount | null>({
    url: `/admin-api/agent-accounts/${agentCode}`,
    method: 'get'
  })
}

/** 新增代理人账号：POST /admin-api/agent-accounts（同 agentCode 仅一个，重复 create 报错） */
export function createAgentAccount(data: Partial<AgentAccount> & { password?: string }): Promise<void> {
  return request<void>({
    url: '/admin-api/agent-accounts',
    method: 'post',
    data
  })
}

/** 修改代理人账号：PUT /admin-api/agent-accounts/{agentCode}（不含 username/password） */
export function updateAgentAccount(agentCode: string, data: Partial<AgentAccount>): Promise<void> {
  return request<void>({
    url: `/admin-api/agent-accounts/${agentCode}`,
    method: 'put',
    data
  })
}

/**
 * 重置账号密码：PUT /admin-api/agent-accounts/{agentCode}/reset-password。
 * 无 body，服务端重置为 dayan@123。
 */
export function resetAgentAccountPassword(agentCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/agent-accounts/${agentCode}/reset-password`,
    method: 'put'
  })
}

/** 删除代理人账号：DELETE /admin-api/agent-accounts/{agentCode} */
export function deleteAgentAccount(agentCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/agent-accounts/${agentCode}`,
    method: 'delete'
  })
}

// ---------------- 2. 代理人-客户绑定（agent-client-rels）----------------

/** 代理人客户绑定列表（全量，按 agentCode 过滤）：GET /admin-api/agent-client-rels/by-agent/{agentCode} */
export function listAgentClientRelsByAgent(agentCode: string): Promise<AgentClientRel[]> {
  return request<AgentClientRel[]>({
    url: `/admin-api/agent-client-rels/by-agent/${agentCode}`,
    method: 'get'
  })
}

/**
 * 绑定客户：POST /admin-api/agent-client-rels/bind。
 * body：agentCode + clientCode + bindType。
 * 同 agentCode+clientCode 仅允许一条 status=1（后端校验）。bindType 默认 1。
 */
export function bindAgentClient(data: {
  agentCode: string
  clientCode: string
  bindType?: number
}): Promise<void> {
  return request<void>({
    url: '/admin-api/agent-client-rels/bind',
    method: 'post',
    data
  })
}

/**
 * 解绑客户：PUT /admin-api/agent-client-rels/{id}/unbind。
 * 路径用雪花 id（string）。解绑后 status 变为 0（软删）。
 */
export function unbindAgentClient(id: string): Promise<void> {
  return request<void>({
    url: `/admin-api/agent-client-rels/${id}/unbind`,
    method: 'put'
  })
}

// ---------------- 3. 代理人业绩（agent-performances）----------------

/** 代理人业绩列表（全量，按 agentCode 过滤）：GET /admin-api/agent-performances/by-agent/{agentCode} */
export function listAgentPerformancesByAgent(agentCode: string): Promise<AgentPerformance[]> {
  return request<AgentPerformance[]>({
    url: `/admin-api/agent-performances/by-agent/${agentCode}`,
    method: 'get'
  })
}

/**
 * 代理人业绩汇总：GET /admin-api/agent-performances/summary/{agentCode}。
 * 返回汇总 SummaryVO。
 */
export function getAgentPerformanceSummary(agentCode: string): Promise<AgentPerformanceSummary> {
  return request<AgentPerformanceSummary>({
    url: `/admin-api/agent-performances/summary/${agentCode}`,
    method: 'get'
  })
}

/**
 * 新增业绩：POST /admin-api/agent-performances（返回 Void，非 id）。
 * 周期唯一（agentCode+periodType+periodValue），重复抛"该周期业绩已存在"。
 */
export function createAgentPerformance(data: Partial<AgentPerformance>): Promise<void> {
  return request<void>({
    url: '/admin-api/agent-performances',
    method: 'post',
    data
  })
}

// ---------------- 4. 代理人分享记录（agent-share-records）----------------

/** 代理人分享记录列表（全量，按 agentCode 过滤）：GET /admin-api/agent-share-records/by-agent/{agentCode} */
export function listAgentShareRecordsByAgent(agentCode: string): Promise<AgentShareRecord[]> {
  return request<AgentShareRecord[]>({
    url: `/admin-api/agent-share-records/by-agent/${agentCode}`,
    method: 'get'
  })
}

/**
 * 新增分享记录：POST /admin-api/agent-share-records（返回 shareCode string）。
 * shareCode 服务端 UUID 生成；viewCount 服务端写0；shareTime 服务端 now()。
 */
export function createAgentShareRecord(data: Partial<AgentShareRecord>): Promise<string> {
  return request<string>({
    url: '/admin-api/agent-share-records',
    method: 'post',
    data
  })
}

// ---------------- 5. 代理人收藏（agent-favorites）----------------

/** 代理人收藏列表（全量，按 agentCode 过滤）：GET /admin-api/agent-favorites/by-agent/{agentCode} */
export function listAgentFavoritesByAgent(agentCode: string): Promise<AgentFavorite[]> {
  return request<AgentFavorite[]>({
    url: `/admin-api/agent-favorites/by-agent/${agentCode}`,
    method: 'get'
  })
}

/**
 * 新增收藏：POST /admin-api/agent-favorites（幂等 add，重复收藏返回既有 id 不报错）。
 */
export function createAgentFavorite(data: Partial<AgentFavorite>): Promise<string> {
  return request<string>({
    url: '/admin-api/agent-favorites',
    method: 'post',
    data
  })
}

/** 删除收藏：DELETE /admin-api/agent-favorites/{id}（路径用雪花 id string，无 update） */
export function deleteAgentFavorite(id: string): Promise<void> {
  return request<void>({
    url: `/admin-api/agent-favorites/${id}`,
    method: 'delete'
  })
}
