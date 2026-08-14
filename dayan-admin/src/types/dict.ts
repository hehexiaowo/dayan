/**
 * 字典相关类型。
 *
 * 字段对齐后端 com.dayan.system.entity.SystemDictCommon。
 * 字典为只读数据（由后端 seed 初始化），无新增/修改/删除。
 */

/** 字典项（后端 SystemDictCommon 实体） */
export interface SystemDictCommon {
  id?: number
  /** 字典类型（如 equity_status / order_status / pay_type） */
  dictType: string
  /** 字典编码（类型内唯一） */
  dictCode: string
  /** 字典名称（展示文案） */
  dictName: string
  /** 字典值（实际存储值） */
  dictValue: string
  /** 父级编码（多级字典用，顶级为 null/空） */
  parentCode: string | null
  /** 层级（从 1 开始） */
  level: number
  /** 排序号 */
  sortOrder: number
  /** 图标（可选） */
  icon: string | null
  /** CSS 类名（可选，用于前端样式区分） */
  cssClass: string | null
  /** 状态：1启用 0禁用 */
  status: number
  /** 是否默认：1是 0否 */
  isDefault: number
  /** 备注 */
  remark: string | null
  /** 创建时间 */
  createdAt?: string
  /** 更新时间 */
  updatedAt?: string
}

/**
 * 字典类型预设（fallback）。
 *
 * 运行时以 listDictTypes() 接口返回为准（动态加载真实存在的类型）；
 * 此处仅作接口不可用时的兜底，避免左侧菜单空白。
 */
export const DICT_TYPE_OPTIONS = [
  { label: '权益状态', value: 'equity_status' },
  { label: '订单状态', value: 'order_status' },
  { label: '支付方式', value: 'pay_type' },
  { label: '性别', value: 'gender' },
  { label: '场景状态', value: 'scene_status' },
  { label: '账户状态', value: 'account_status' },
  { label: '角色类型', value: 'role_type' },
  { label: '菜单类型', value: 'menu_type' }
] as const

/**
 * 业务字典项（后端 SystemDictBusiness 实体，表 system_dict_business）。
 * 按 domain（业务域）组织，区别于通用字典 SystemDictCommon。
 */
export interface SystemDictBusiness {
  id?: number
  dictType: string
  dictCode: string
  dictName: string
  dictValue: string
  parentCode?: string
  /** 所属业务域（如 park/scene/order） */
  domain?: string
  sortOrder?: number
  /** 状态：1启用 0禁用 */
  status?: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/** 业务字典分页查询参数 */
export interface SystemDictBusinessQuery {
  dictType?: string
  domain?: string
  current: number
  size: number
}
