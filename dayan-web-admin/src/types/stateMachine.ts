/**
 * 状态机配置相关类型。
 *
 * 字段对齐后端 com.dayan.system.entity.SystemStateMachine。
 */

/** 业务类型选项（对齐 seed 实际 6 个业务域） */
export const STATE_MACHINE_BIZ_TYPE_OPTIONS = [
  { label: '权益', value: 'equity' },
  { label: '订单', value: 'order' },
  { label: '服务会话', value: 'service' },
  { label: '园区', value: 'park' },
  { label: '内容', value: 'content' },
  { label: '场景', value: 'scene' }
] as const

/**
 * 状态机配置（后端 SystemStateMachine 实体）。
 *
 * 一条记录 = 状态机的一个「迁移规则」（fromState --event--> toState）。
 * fromState/toState 为 Integer（DDL TINYINT），对应业务状态枚举值。
 * fromSubState/toSubState/conditionExpr/actionBean 为高级字段，
 * 接口保留类型对齐，但 Admin 表单不暴露（DB 列可空）。
 */
export interface SystemStateMachine {
  id?: number
  /** 状态机编码（标识一组状态机，如 ORDER_SM） */
  machineCode: string
  /** 状态机名称 */
  machineName: string
  /** 业务类型：equity/order/service/park/content/scene */
  bizType: string
  /** 起始状态码（Integer，业务状态枚举值） */
  fromState: number
  /** 起始状态名称 */
  fromStateName: string
  /** 起始子状态值（高级字段，表单不暴露） */
  fromSubState?: string | null
  /** 目标状态码（Integer，业务状态枚举值） */
  toState: number
  /** 目标状态名称 */
  toStateName: string
  /** 目标子状态值（高级字段，表单不暴露） */
  toSubState?: string | null
  /** 触发事件编码（如 pay/cancel） */
  eventCode: string
  /** 触发事件名称 */
  eventName: string
  /** 流转条件表达式（高级字段，表单不暴露） */
  conditionExpr?: string | null
  /** 流转执行器 bean 名（高级字段，表单不暴露） */
  actionBean?: string | null
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
