/**
 * 状态机配置相关类型。
 *
 * 字段对齐后端 com.dayan.system.entity.SystemStateMachine。
 * 注意：后端 SystemStateMachineAdminController 暂未提供，前端做降级处理。
 */

/** 业务类型选项（按系统已知业务枚举） */
export const STATE_MACHINE_BIZ_TYPE_OPTIONS = [
  { label: '订单', value: 'order' },
  { label: '权益', value: 'equity' },
  { label: '场景', value: 'scene' },
  { label: '退款', value: 'refund' }
] as const

/**
 * 状态机配置（后端 SystemStateMachine 实体）。
 *
 * 一条记录 = 状态机的一个「迁移规则」（fromState --event--> toState）。
 */
export interface SystemStateMachine {
  id?: number
  /** 状态机编码（标识一组状态机，如 order_state_machine） */
  machineCode: string
  /** 状态机名称 */
  machineName: string
  /** 业务类型：order/equity/scene/refund 等 */
  bizType: string
  /** 起始状态码 */
  fromState: string
  /** 起始状态名称 */
  fromStateName: string
  /** 目标状态码 */
  toState: string
  /** 目标状态名称 */
  toStateName: string
  /** 触发事件编码（如 pay/cancel） */
  eventCode: string
  /** 触发事件名称 */
  eventName: string
  /** 排序号 */
  sortOrder: number
  /** 状态：1启用 0禁用 */
  status: number
  /** 备注 */
  remark: string | null
  /** 创建时间 */
  createdAt?: string
  /** 更新时间 */
  updatedAt?: string
}

/** 状态机分页查询参数 */
export interface StateMachineQuery {
  /** 状态机编码筛选 */
  machineCode?: string
  /** 业务类型筛选 */
  bizType?: string
  current: number
  size: number
}
