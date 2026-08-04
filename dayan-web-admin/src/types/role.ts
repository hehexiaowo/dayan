/**
 * 角色相关类型。
 *
 * 字段对齐后端 com.dayan.organ.entity.OrganRole。
 */

/** 角色状态：1启用 0禁用 */
export enum RoleStatus {
  ENABLED = 1,
  DISABLED = 0
}

/** 角色状态选项 */
export const ROLE_STATUS_OPTIONS = [
  { label: '启用', value: RoleStatus.ENABLED },
  { label: '禁用', value: RoleStatus.DISABLED }
] as const

/** 角色类型 */
export type RoleType = string

/** 角色类型选项（对齐业务常见取值，后端无枚举强约束） */
export const ROLE_TYPE_OPTIONS = [
  { label: '系统角色', value: 'system' },
  { label: '业务角色', value: 'business' },
  { label: '数据角色', value: 'data' }
] as const

/** 数据权限范围 */
export type DataScope = string

/** 数据权限范围选项（对齐常见 RBAC 数据范围取值） */
export const DATA_SCOPE_OPTIONS = [
  { label: '全部数据', value: 'all' },
  { label: '本部门数据', value: 'dept' },
  { label: '本部门及下级', value: 'dept_and_sub' },
  { label: '仅本人数据', value: 'self' },
  { label: '自定义', value: 'custom' }
] as const

/**
 * 角色实体（后端 OrganRole）。
 *
 * permissionCodes 为角色关联的权限编码集合；详情接口可能不回填，
 * 由前端 getPermissions(roleCode) 单独拉取。
 */
export interface Role {
  id?: number
  /** 机构编码 */
  organCode: string
  /** 角色编码（主键业务码） */
  roleCode?: string
  /** 角色名称 */
  roleName: string
  /** 角色类型 */
  roleType?: RoleType
  /** 角色描述 */
  description?: string
  /** 数据权限范围 */
  dataScope?: DataScope
  /** 状态：1启用 0禁用 */
  status: RoleStatus
  /** 排序号 */
  sortOrder?: number
  /** 关联权限编码列表 */
  permissionCodes?: string[]
}

/** 角色分页查询参数 */
export interface RoleQuery {
  /** 机构编码（可选） */
  organCode?: string
  /** 角色名称（模糊匹配，可选） */
  roleName?: string
  /** 角色编码（模糊匹配，可选） */
  roleCode?: string
  /** 状态（可选） */
  status?: RoleStatus
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
}
