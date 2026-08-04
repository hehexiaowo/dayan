/**
 * 操作日志相关类型。
 *
 * 字段对齐后端 com.dayan.system.entity.SystemOperationLog。
 * 注意：后端 SystemOperationLogAdminController 暂未提供，前端做降级处理。
 */

/** 日志类型 */
export type LogType = 'login' | 'logout' | 'create' | 'update' | 'delete' | 'export' | 'other'

/** 日志类型选项 */
export const LOG_TYPE_OPTIONS = [
  { label: '登录', value: 'login' },
  { label: '登出', value: 'logout' },
  { label: '新增', value: 'create' },
  { label: '修改', value: 'update' },
  { label: '删除', value: 'delete' },
  { label: '导出', value: 'export' },
  { label: '其他', value: 'other' }
] as const

/** 操作状态 */
export type LogStatus = 'success' | 'fail'

/** 操作状态选项 */
export const LOG_STATUS_OPTIONS = [
  { label: '成功', value: 'success' },
  { label: '失败', value: 'fail' }
] as const

/**
 * 操作日志（后端 SystemOperationLog 实体）。
 */
export interface SystemOperationLog {
  id?: number
  /** 日志类型 */
  logType: LogType
  /** 模块（如 system/account） */
  module: string
  /** 操作动作（如 新增账号） */
  action: string
  /** HTTP 方法（GET/POST/PUT/DELETE） */
  method: string
  /** 请求 URL */
  requestUrl: string
  /** 请求参数（JSON 字符串） */
  requestParams: string
  /** 响应结果（JSON 字符串） */
  responseResult: string
  /** 状态：success/fail */
  status: LogStatus
  /** 耗时（毫秒） */
  costTime: number
  /** 操作人编码 */
  operatorCode: string
  /** 操作人姓名 */
  operatorName: string
  /** 操作人 IP */
  operatorIp: string
  /** 操作时间 */
  operateTime: string
}

/** 操作日志分页查询参数 */
export interface OperationLogQuery {
  /** 日志类型筛选 */
  logType?: LogType
  /** 模块筛选 */
  module?: string
  /** 操作人编码筛选 */
  operatorCode?: string
  /** 操作时间范围起始（ISO 字符串） */
  startTime?: string
  /** 操作时间范围结束（ISO 字符串） */
  endTime?: string
  current: number
  size: number
}
