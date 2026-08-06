import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { Agent, AgentQuery } from '@/types/agent'

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
