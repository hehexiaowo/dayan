/**
 * 菜单相关类型。
 *
 * 字段对齐后端 com.dayan.system.entity.SystemMenu。
 * 从 dayan-admin 复制（P8 已建立），channel 端共用同一菜单实体。
 */

/** 菜单类型 */
export enum MenuType {
  /** 目录 */
  DIRECTORY = 1,
  /** 菜单 */
  MENU = 2,
  /** 按钮 */
  BUTTON = 3
}

/** 所属端 */
export type DomainType = 'admin' | 'channel' | 'agent' | 'client'

/**
 * 菜单（后端 SystemMenu 实体）。
 *
 * 树形结构：children 由后端 tree 接口组装（list 接口为平铺，需前端自行 buildTree）。
 */
export interface Menu {
  id?: number
  /** 菜单编码（主键业务码） */
  menuCode: string
  /** 菜单名称 */
  menuName: string
  /** 父菜单编码（顶级为 null/空） */
  parentCode: string | null
  /** 菜单类型：1目录 2菜单 3按钮 */
  menuType: MenuType
  /** 路由路径（如 /dashboard） */
  path: string | null
  /** 前端组件路径（如 dashboard/index，相对 src/views） */
  component: string | null
  /** 权限标识（按钮类用，如 channel:agent:view） */
  permissionCode: string | null
  /** 菜单图标（Element Plus 图标组件名，如 User） */
  icon: string | null
  /** 排序号 */
  sortOrder: number
  /** 是否可见：1可见 0隐藏 */
  isVisible: number
  /** 是否外链：1是 0否 */
  isExternal: number
  /** 是否缓存：1是 0否 */
  isCache: number
  /** 所属域：admin/channel/agent/client */
  domainType: DomainType
  /** 状态：1启用 0禁用 */
  status: number
  /** 备注 */
  remark: string | null
  /** 子菜单（树形接口返回时填充） */
  children?: Menu[]
  /** 创建时间 */
  createdAt?: string
  /** 更新时间 */
  updatedAt?: string
}
