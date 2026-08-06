/**
 * 后端通用类型（com.dayan.common.core.resp.PageResult 等）的前端映射。
 */

/**
 * 分页查询基础参数。
 */
export interface PageQuery {
  /** 当前页码（从 1 开始） */
  current: number
  /** 每页条数 */
  size: number
}

/**
 * 分页结果（后端 com.dayan.common.core.resp.PageResult<T>）。
 *
 * 字段与后端逐字对齐：current / size / total / pages / records。
 */
export interface PageResult<T> {
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
  /** 总记录数 */
  total: number
  /** 总页数 */
  pages: number
  /** 当前页数据列表 */
  records: T[]
}

/**
 * 通用启用/禁用状态。
 */
export enum CommonStatus {
  /** 启用 */
  ENABLED = 1,
  /** 禁用 */
  DISABLED = 0
}

/**
 * 通用启用/禁用选项（供 el-select / el-switch 使用）。
 */
export const COMMON_STATUS_OPTIONS = [
  { label: '启用', value: CommonStatus.ENABLED },
  { label: '禁用', value: CommonStatus.DISABLED }
] as const
