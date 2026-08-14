/**
 * 账号相关类型。
 *
 * 字段对齐后端 com.dayan.organ.entity.OrganAccount。
 *
 * 注意：敏感字段（password/salt/openId/unionId/idCard）仅在创建/重置场景使用，
 * 列表与详情展示一律不返回这些字段。
 */

/** 账号状态：1启用 0禁用 */
export enum AccountStatus {
  ENABLED = 1,
  DISABLED = 0
}

/** 账号状态选项 */
export const ACCOUNT_STATUS_OPTIONS = [
  { label: '启用', value: AccountStatus.ENABLED },
  { label: '禁用', value: AccountStatus.DISABLED }
] as const

/** 性别（与字典 sex 对齐）：1男 2女 0未知 */
export const GENDER_OPTIONS = [
  { label: '男', value: 1 },
  { label: '女', value: 2 },
  { label: '未知', value: 0 }
] as const

/**
 * 账号实体（后端 OrganAccount）。
 *
 * 列表 / 详情接口不返回 password / salt / idCard / openId / unionId；
 * create 时 password 必填，update 时 password 留空表示不修改。
 */
export interface Account {
  id?: number
  /** 机构编码 */
  organCode: string
  /** 账号编码（主键业务码） */
  accountCode?: string
  /** 登录账号 */
  username: string
  /** 密码（仅 create/reset-password 使用） */
  password?: string
  /** 密码盐（仅后端内部使用，前端不展示） */
  salt?: string
  /** 真实姓名 */
  realName: string
  /** 头像地址 */
  avatar?: string
  /** 性别：1男 2女 0未知 */
  gender?: number
  /** 手机号 */
  phone?: string
  /** 微信 openId（敏感，不展示） */
  openId?: string
  /** 微信 unionId（敏感，不展示） */
  unionId?: string
  /** 邮箱 */
  email?: string
  /** 身份证号（敏感，不展示） */
  idCard?: string
  /** 最近登录时间 */
  lastLoginTime?: string
  /** 最近登录 IP */
  lastLoginIp?: string
  /** 登录次数 */
  loginCount?: number
  /** 密码更新时间 */
  pwdUpdateTime?: string
  /** 账号状态：1启用 0禁用 */
  accountStatus: AccountStatus
  /** 是否超管：1是 0否 */
  isAdmin?: number
  /** 备注 */
  remark?: string
  /** 机构名称（列表 VO 解析回填，提交时不传） */
  organName?: string
  /** 已分配角色编码列表（详情 VO / 表单回显用） */
  roleCodes?: string[]
  /** 已分配角色名称列表（列表 VO 解析回填，仅展示） */
  roleNames?: string[]
}

/** 账号分页查询参数 */
export interface AccountQuery {
  /** 机构编码（必填） */
  organCode: string
  /** 登录账号（模糊匹配，可选） */
  username?: string
  /** 真实姓名（模糊匹配，可选） */
  realName?: string
  /** 账号状态（可选） */
  accountStatus?: AccountStatus
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
}
