/**
 * 后端统一响应封装（com.dayan.common.core.resp.R）。
 */
export interface ApiResult<T = unknown> {
  /** 业务码：0 成功，非 0 失败 */
  code: number
  /** 业务消息 */
  message: string
  /** 业务数据 */
  data: T
  /** 时间戳（毫秒） */
  timestamp?: number
  /** 链路追踪 ID */
  traceId?: string
}

/** 业务码常量 */
export const CODE_SUCCESS = 0

/** Token 失效相关业务码（10100 未登录 / 10101 Token 失效） */
export const CODE_UNAUTHORIZED = 10100
export const CODE_TOKEN_INVALID = 10101

/** Channel 端 Token 请求头名称 */
export const TOKEN_HEADER = 'Channel-Token'
