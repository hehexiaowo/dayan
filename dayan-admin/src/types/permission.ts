/**
 * 权限相关类型。
 *
 * 字段对齐后端 com.dayan.organ.entity.OrganPermission。
 */

/** 权限类型：1=菜单 2=按钮 3=接口 4=数据（对齐 DDL organ_permission.permission_type） */
export enum PermissionType {
  /** 菜单 */
  MENU = 1,
  /** 按钮 */
  BUTTON = 2,
  /** 接口 */
  API = 3,
  /** 数据 */
  DATA = 4
}

/** 权限类型选项 */
export const PERMISSION_TYPE_OPTIONS = [
  { label: '菜单', value: PermissionType.MENU },
  { label: '按钮', value: PermissionType.BUTTON },
  { label: '接口', value: PermissionType.API },
  { label: '数据', value: PermissionType.DATA }
] as const

/** 权限状态：1启用 0禁用 */
export enum PermissionStatus {
  ENABLED = 1,
  DISABLED = 0
}

/** 权限状态选项 */
export const PERMISSION_STATUS_OPTIONS = [
  { label: '启用', value: PermissionStatus.ENABLED },
  { label: '禁用', value: PermissionStatus.DISABLED }
] as const

/**
 * 权限实体（后端 OrganPermission）。
 *
 * 树形结构：children 由后端 tree 接口组装；list/all 接口为平铺，
 * 前端可用 buildPermissionTree 自行组树。
 */
export interface Permission {
  id?: number
  /** 权限编码（主键业务码） */
  permissionCode: string
  /** 权限名称 */
  permissionName: string
  /** 父权限编码（顶级为 null/空） */
  parentCode: string | null
  /** 权限类型：1菜单 2按钮 3接口 4数据 */
  permissionType: PermissionType
  /** 资源路径（接口/页面路径） */
  path?: string
  /** 请求方法（接口类权限用：GET/POST/PUT/DELETE） */
  method?: string
  /** 图标 */
  icon?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 0禁用 */
  status: PermissionStatus
  /** 备注 */
  remark?: string
  /** 子权限（树形接口返回时填充） */
  children?: Permission[]
}

/**
 * 将平铺权限列表构建为树形结构。
 *
 * 用于 list/all 接口（平铺）转树；tree 接口已组装则无需调用。
 */
export function buildPermissionTree(list: Permission[], parentCode: string | null = null): Permission[] {
  return list
    .filter((p) => (p.parentCode ?? null) === parentCode)
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    .map((p) => {
      const children = buildPermissionTree(list, p.permissionCode)
      return children.length > 0 ? { ...p, children } : { ...p, children: undefined }
    })
}

/**
 * 角色授权树节点（后端 com.dayan.system.vo.MenuGrantTreeVO）。
 *
 * nodeKey 带类型前缀：'menu:'+menuCode（目录/菜单）、'perm:'+permissionCode（操作权限）、
 * 'group:other'（其他权限虚拟组，保存时丢弃）。
 */
export interface GrantTreeNode {
  nodeKey: string
  name: string
  /** DIR 目录 / MENU 菜单 / PERM 操作权限 / GROUP 虚拟组 */
  nodeType: 'DIR' | 'MENU' | 'PERM' | 'GROUP'
  children?: GrantTreeNode[]
}
