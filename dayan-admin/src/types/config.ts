/**
 * 系统配置相关类型。
 *
 * 字段对齐后端 com.dayan.system.entity.SystemConfig。
 */

/** 配置值类型 */
export type ConfigValueType = 'string' | 'number' | 'boolean' | 'json'

/** 配置值类型选项 */
export const CONFIG_VALUE_TYPE_OPTIONS = [
  { label: '字符串', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' },
  { label: 'JSON', value: 'json' }
] as const

/** 配置所属环境 */
export type ConfigEnv = 'dev' | 'test' | 'prod' | 'all'

/** 配置环境选项 */
export const CONFIG_ENV_OPTIONS = [
  { label: '开发', value: 'dev' },
  { label: '测试', value: 'test' },
  { label: '生产', value: 'prod' },
  { label: '全部', value: 'all' }
] as const

/** 配置作用域 */
export type ConfigScope = 'system' | 'organ' | 'user'

/** 配置作用域选项 */
export const CONFIG_SCOPE_OPTIONS = [
  { label: '系统级', value: 'system' },
  { label: '机构级', value: 'organ' },
  { label: '用户级', value: 'user' }
] as const

/** 常见配置分组（搜索栏下拉用） */
export const CONFIG_GROUP_OPTIONS = [
  { label: '系统基础', value: 'system' },
  { label: '安全策略', value: 'security' },
  { label: '通知', value: 'notify' },
  { label: '存储', value: 'storage' },
  { label: '第三方', value: 'third_party' }
] as const

/**
 * 系统配置（后端 SystemConfig 实体）。
 *
 * 主键业务码：configKey。
 * isSecret=1 时 configValue 为敏感值，列表脱敏显示（展示 `***`）。
 */
export interface SystemConfig {
  id?: number
  /** 配置分组 */
  configGroup: string
  /** 配置 Key（主键） */
  configKey: string
  /** 配置值 */
  configValue: string
  /** 值类型：string/number/boolean/json */
  valueType: ConfigValueType
  /** 环境：dev/test/prod/all */
  env: ConfigEnv
  /** 作用域：system/organ/user */
  scope: ConfigScope
  /** 机构编码（scope=organ 时生效） */
  organCode: string | null
  /** 用户编码（scope=user 时生效） */
  userCode: string | null
  /** 配置名称（展示用） */
  configName: string
  /** 描述 */
  description: string | null
  /** 是否敏感：1是 0否（敏感值列表脱敏） */
  isSecret: number
  /** 是否运行时可修改：1是 0否 */
  isRuntime: number
  /** 排序号 */
  sortOrder: number
  /** 创建时间 */
  createdAt?: string
  /** 更新时间 */
  updatedAt?: string
}

/** 系统配置分页查询参数 */
export interface ConfigQuery {
  /** 配置分组筛选 */
  configGroup?: string
  /** 配置 Key 模糊筛选 */
  configKey?: string
  current: number
  size: number
}
