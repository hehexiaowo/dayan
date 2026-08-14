import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SystemLog, SystemLogQuery, LogSource } from '@/types/log'

/**
 * 系统日志接口封装（四端分表）。
 *
 * 对应后端 SystemLogController（/admin-api/logs/*）。
 * 四张 system_log_* 表各自自增主键可能重复，查询与详情都必须携带 source。
 */

/** 分页查询系统日志：GET /admin-api/logs?source&module&accountCode&resultStatus&startTime&endTime&current&size */
export function pageSystemLogs(query: SystemLogQuery): Promise<PageResult<SystemLog>> {
  return request<PageResult<SystemLog>>({
    url: '/admin-api/logs',
    method: 'get',
    params: {
      source: query.source,
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

/** 系统日志详情（按来源 + 主键 id）：GET /admin-api/logs/{id}?source= */
export function getSystemLogDetail(id: number, source: LogSource): Promise<SystemLog> {
  return request<SystemLog>({
    url: `/admin-api/logs/${id}`,
    method: 'get',
    params: { source }
  })
}
