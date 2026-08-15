/**
 * 字典类型（统一单表 system_dict，原 system_dict_common + system_dict_business 于 54 迁移合并）。
 * 字典管理（系统管理 → 字典管理）统一维护全部类型；
 * 业务语义类型以 domain 标注所属域，extra(JSON) 承载扩展属性。
 */

/** 字典项（后端 SystemDict 实体，表 system_dict） */
export interface SystemDict {
  id?: number
  /** 字典类型（如 gender / content_category / asset_ref_type2） */
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
  /** 业务域（通用字典为空；业务语义字典标注所属域，如 park/content） */
  domain?: string | null
  /** 排序号 */
  sortOrder: number
  /** 图标（可选） */
  icon: string | null
  /** CSS 类名（可选，用于前端样式区分） */
  cssClass: string | null
  /** 扩展属性（JSON 字符串，如内容分类的 coverImage/isVisible） */
  extra?: string | null
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
 * 运行时以 listDictTypes() 接口返回为准（动态加载真实存在的类型）；
 * 此处仅作接口不可用时的兜底，避免左侧菜单空白。
 */
export const DICT_TYPE_OPTIONS = [
  { label: '性别', value: 'gender' },
  { label: '是否', value: 'yes_no' },
  { label: '通用状态', value: 'common_status' },
  { label: '账户状态', value: 'account_status' },
  { label: '业务状态', value: 'biz_status' },
  { label: '内容分类', value: 'content_category' },
  { label: '素材细分分类', value: 'asset_ref_type2' }
] as const
