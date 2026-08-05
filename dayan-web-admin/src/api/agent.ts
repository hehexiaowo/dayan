import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { AgentInfo, AgentInfoQuery } from '@/types/agent'

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
