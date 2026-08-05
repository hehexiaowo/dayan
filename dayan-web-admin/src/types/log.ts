/**
 * 操作日志相关类型。
 *
 * 字段逐字对齐后端 com.dayan.system.entity.SystemOperationLog（含 BaseEntity 的 createdAt 等）。
 */

/**
 * 操作日志（后端 SystemOperationLog 实体）。
 *
 * 字段语义：
 * - accountType / accountCode / accountName：操作账号类型/编码/姓名
 * - module / action：操作模块 / 操作动作（如 "管家账号" / "新增"）
 * - requestMethod / requestUrl / requestParams：请求方法 / URL / 参数（JSON 字符串，已脱敏）
 * - resultStatus：结果状态，1=成功 / 0=失败
 * - duration：执行耗时（毫秒）
 * - ipAddress / userAgent / deviceType / os / browser：终端审计信息
 */
export interface SystemOperationLog {
  id?: number
  /** 链路追踪 ID */
  traceId?: string
  /** 账号类型（admin/channel/agent/client/supplier/distributor） */
  accountType?: string
  /** 操作账号编码 */
  accountCode?: string
  /** 操作人姓名 */
  accountName?: string
  /** 操作模块 */
  module?: string
  /** 操作动作 */
  action?: string
  /** 操作描述 */
  actionDescription?: string
  /** 操作对象类型 */
  targetType?: string
  /** 操作对象编码 */
  targetCode?: string
  /** 操作对象描述 */
  targetDescription?: string
  /** 请求 URL */
  requestUrl?: string
  /** 请求方法（GET/POST/PUT/DELETE） */
  requestMethod?: string
  /** 请求参数（JSON 字符串，已脱敏） */
  requestParams?: string
  /** 响应状态码（0=成功，1=失败，与 resultStatus 同义但为 Int） */
  responseCode?: number
  /** 操作 IP 地址 */
  ipAddress?: string
  /** IP 归属地 */
  ipLocation?: string
  /** 浏览器 User-Agent */
  userAgent?: string
  /** 设备类型（pc/mobile/tablet） */
  deviceType?: string
  /** 操作系统 */
  os?: string
  /** 浏览器 */
  browser?: string
  /** 结果状态：1=成功 / 0=失败 */
  resultStatus?: number
  /** 错误信息（失败时） */
  errorMsg?: string
  /** 执行耗时（毫秒） */
  duration?: number
  /** 创建时间（即操作时间） */
  createdAt?: string
}

/** 操作日志分页查询参数 */
export interface OperationLogQuery {
  /** 模块筛选（模糊匹配） */
  module?: string
  /** 操作账号编码筛选（精确匹配） */
  accountCode?: string
  /** 结果状态筛选：1=成功 / 0=失败 */
  resultStatus?: number
  /** 操作时间范围起始（ISO 字符串 yyyy-MM-ddTHH:mm:ss） */
  startTime?: string
  /** 操作时间范围结束（ISO 字符串 yyyy-MM-ddTHH:mm:ss） */
  endTime?: string
  current: number
  size: number
}

/** 结果状态选项 */
export const RESULT_STATUS_OPTIONS = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 }
] as const

/** 结果状态标签文案 */
export function resultStatusLabel(status?: number): string {
  if (status === 1) return '成功'
  if (status === 0) return '失败'
  return String(status ?? '')
}
