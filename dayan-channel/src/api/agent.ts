import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { Agent, AgentQuery, AgentAccount, AgentAccountQuery, AgentClientRel, AgentClientRelQuery, ShareRecord, ShareRecordQuery } from '@/types/agent'

/**
 * 代理人接口封装。
 *
 * 对应后端 Channel 端 AgentController（/channel-api/agents/*）。
 * 注意：本期后端业务端点尚未实现，调用会走 request.ts 响应拦截器报错，
 * 调用方需 try/catch 降级处理。
 */

/** 代理人分页：GET /channel-api/agents */
export function pageAgents(query: AgentQuery): Promise<PageResult<Agent>> {
  return request<PageResult<Agent>>({
    url: '/channel-api/agents',
    method: 'get',
    params: query
  })
}

// ==================== 代理人账号（/channel-api/agent-accounts）====================

/** 代理人账号分页 */
export function pageAgentAccounts(
  query: AgentAccountQuery
): Promise<PageResult<AgentAccount>> {
  return request<PageResult<AgentAccount>>({
    url: '/channel-api/agent-accounts',
    method: 'get',
    params: query
  })
}

/** 代理人账号详情 */
export function getAgentAccount(agentCode: string): Promise<AgentAccount> {
  return request<AgentAccount>({
    url: `/channel-api/agent-accounts/${agentCode}`,
    method: 'get'
  })
}

// ==================== 客户线索（/channel-api/agent-client-rels）====================

/** 客户线索分页 */
export function pageAgentClientRels(
  query: AgentClientRelQuery
): Promise<PageResult<AgentClientRel>> {
  return request<PageResult<AgentClientRel>>({
    url: '/channel-api/agent-client-rels',
    method: 'get',
    params: query
  })
}

// ==================== 分享记录（/channel-api/agent-share-records）====================

/** 分享记录分页 */
export function pageShareRecords(
  query: ShareRecordQuery
): Promise<PageResult<ShareRecord>> {
  return request<PageResult<ShareRecord>>({
    url: '/channel-api/agent-share-records',
    method: 'get',
    params: query
  })
}
