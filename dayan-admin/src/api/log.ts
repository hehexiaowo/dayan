import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SystemOperationLog, OperationLogQuery } from '@/types/log'

/**
 * 操作日志接口封装。
 *
 * 对应后端 SystemOperationLogController（/admin-api/operation-logs/*）。
 */

/** 分页查询操作日志：GET /admin-api/operation-logs?module&accountCode&resultStatus&startTime&endTime&current&size */
export function pageOperationLogs(query: OperationLogQuery): Promise<PageResult<SystemOperationLog>> {
  return request<PageResult<SystemOperationLog>>({
    url: '/admin-api/operation-logs',
    method: 'get',
    params: {
      module: query.module || undefined,
      accountCode: query.accountCode || undefined,
      resultStatus: query.resultStatus ?? undefined,
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
