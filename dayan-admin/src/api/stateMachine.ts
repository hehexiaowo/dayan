import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SystemStateMachine, StateMachineQuery } from '@/types/stateMachine'

/**
 * 状态机配置接口封装。
 *
 * 对应后端 SystemStateMachineAdminController（/admin-api/state-machines/*）。
 */

/** 分页查询状态机配置：GET /admin-api/state-machines?machineCode&bizType&current&size */
export function pageStateMachines(query: StateMachineQuery): Promise<PageResult<SystemStateMachine>> {
  return request<PageResult<SystemStateMachine>>({
    url: '/admin-api/state-machines',
    method: 'get',
    params: {
      machineCode: query.machineCode || undefined,
      bizType: query.bizType || undefined,
      current: query.current,
      size: query.size
    }
  })
}

/** 查询状态机规则详情：GET /admin-api/state-machines/{id} */
export function getStateMachine(id: number): Promise<SystemStateMachine> {
  return request<SystemStateMachine>({
    url: `/admin-api/state-machines/${id}`,
    method: 'get'
  })
}

/** 新增状态机配置：POST /admin-api/state-machines */
export function createStateMachine(data: Partial<SystemStateMachine>): Promise<number> {
  return request<number>({
    url: '/admin-api/state-machines',
    method: 'post',
    data
  })
}

/** 修改状态机配置：PUT /admin-api/state-machines/{id} */
export function updateStateMachine(id: number, data: Partial<SystemStateMachine>): Promise<void> {
  return request<void>({
    url: `/admin-api/state-machines/${id}`,
    method: 'put',
    data
  })
}

/** 删除状态机配置：DELETE /admin-api/state-machines/{id} */
export function deleteStateMachine(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/state-machines/${id}`,
    method: 'delete'
  })
}
