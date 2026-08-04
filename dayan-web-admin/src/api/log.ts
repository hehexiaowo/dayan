import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SystemOperationLog, OperationLogQuery } from '@/types/log'

/**
 * 操作日志接口封装。
 *
 * 预期契约对应后端 SystemOperationLogAdminController（/admin-api/operation-logs/*）。
 * 注意：后端 controller 暂未提供，页面调用会 404，前端在页面层做 try/catch 降级。
 */

/** 分页查询操作日志：GET /admin-api/operation-logs?logType&module&operatorCode&startTime&endTime&current&size */
export function pageOperationLogs(query: OperationLogQuery): Promise<PageResult<SystemOperationLog>> {
  return request<PageResult<SystemOperationLog>>({
    url: '/admin-api/operation-logs',
    method: 'get',
    params: {
      logType: query.logType || undefined,
      module: query.module || undefined,
      operatorCode: query.operatorCode || undefined,
      startTime: query.startTime || undefined,
      endTime: query.endTime || undefined,
      current: query.current,
      size: query.size
    }
  })
}

/** 操作日志详情（按主键 id）：GET /admin-api/operation-logs/{id} */
export function getOperationLogDetail(id: number): Promise<SystemOperationLog> {
  return request<SystemOperationLog>({
    url: `/admin-api/operation-logs/${id}`,
    method: 'get'
  })
}
