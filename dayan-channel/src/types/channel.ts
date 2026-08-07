/**
 * 渠道端系统管理类型（账号 / 角色 / 权限 / 渠道架构）。
 *
 * 从 admin 端 src/types/channel.ts 精简而来，渠道端系统管理专用。
 * 已剔除 admin 版的 ChannelConfig 系列 / ChannelOpenPlatform / ChannelDistributor 等增量2不用的接口，
 * 避免类型污染。
 *
 * 字段对齐后端：
 * - ChannelInfo     ← com.dayan.channel.entity.ChannelInfo（表 channel_info）
 * - ChannelAccount  ← ChannelAccountVO
 * - ChannelRole     ← ChannelRole Entity + BaseEntity 审计字段
 * - ChannelPermission ← ChannelPermission Entity（增量2后端新增）
 *
 * 主键约定：
 * - ChannelInfo    业务键 channelCode（路径参数用 channelCode）
 * - ChannelAccount 业务键 accountCode（CA 前缀，路径参数用 accountCode）
 * - ChannelRole    业务键 roleCode（RL 前缀，路径参数用 roleCode）
 * - ChannelPermission 主键 id（Long，权限树由 tree 接口直接组装）
 */
import type { PageQuery } from '@/types/common'

// ==================== 渠道架构（ChannelInfo）====================

/** 渠道类型：1=总代理 2=区域代理 3=城市代理 4=门店 */
export enum ChannelType {
  /** 总代理 */
  GENERAL = 1,
  /** 区域代理 */
  REGION = 2,
  /** 城市代理 */
  CITY = 3,
  /** 门店 */
  STORE = 4
}

/** 渠道类型选项 */
export const CHANNEL_TYPE_OPTIONS = [
  { label: '总代理', value: ChannelType.GENERAL },
  { label: '区域代理', value: ChannelType.REGION },
  { label: '城市代理', value: ChannelType.CITY },
  { label: '门店', value: ChannelType.STORE }
] as const

/** 渠道状态：1启用 0禁用 */
export enum ChannelStatus {
  ENABLED = 1,
  DISABLED = 0
}

/** 渠道状态选项 */
export const CHANNEL_STATUS_OPTIONS = [
  { label: '启用', value: ChannelStatus.ENABLED },
  { label: '禁用', value: ChannelStatus.DISABLED }
] as const

/** 渠道审核状态：0待审 1通过 2驳回 */
export enum ChannelAuditStatus {
  PENDING = 0,
  PASS = 1,
  REJECT = 2
}

/** 渠道审核状态选项 */
export const CHANNEL_AUDIT_STATUS_OPTIONS = [
  { label: '待审核', value: ChannelAuditStatus.PENDING },
  { label: '审核通过', value: ChannelAuditStatus.PASS },
  { label: '审核驳回', value: ChannelAuditStatus.REJECT }
] as const

/**
 * 渠道信息实体（后端 ChannelInfo / ChannelInfoVO）。
 *
 * 渠道架构页面以树形表格展示，树数据由 /tree 接口直接返回。
 */
export interface ChannelInfo {
  id?: number
  /** 渠道编码（主键业务码，服务端生成） */
  channelCode?: string
  /** 渠道全称 */
  fullName: string
  /** 渠道简称 */
  shortName?: string
  /** 渠道类型：1总代理 2区域代理 3城市代理 4门店 */
  channelType?: ChannelType
  /** 上级渠道编码（树形关键字段，顶级为 null/空） */
  parentCode?: string | null
  /** 祖级列表（逗号分隔编码链，后端维护） */
  ancestors?: string
  /** 层级 */
  level?: number
  /** 统一社会信用代码 */
  unifiedCreditCode?: string
  /** 法人代表 */
  legalPerson?: string
  /** 省级编码 */
  provinceCode?: string
  /** 市级编码 */
  cityCode?: string
  /** 区县编码 */
  districtCode?: string
  /** 详细地址 */
  address?: string
  /** 联系人 */
  contactPerson?: string
  /** 联系电话 */
  contactPhone?: string
  /** 联系邮箱 */
  contactEmail?: string
  /** Logo 地址 */
  logoUrl?: string
  /** 渠道描述 */
  description?: string
  /** 旗下代理人数量（统计字段） */
  agentCount?: number
  /** 累计订单金额（统计字段） */
  totalOrderAmount?: number
  /** 合作开始日期 */
  cooperationStartDate?: string
  /** 分销商编码 */
  distributorCode?: string
  /** 结算周期 */
  settlementCycle?: number
  /** 功能配置（JSON 字符串，渠道专属功能开关） */
  featureConfig?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 0禁用 */
  status?: ChannelStatus
  /** 审核状态：0待审 1通过 2驳回 */
  auditStatus?: ChannelAuditStatus
  /** 备注 */
  remark?: string
  /**
   * 当前登录账号是否有权管理该渠道（0否 1是，增量2后端新增，admin 版无此字段）。
   *
   * 渠道端只能管理自身及下级渠道，后端按当前账号所属渠道下钻，前端据此控制
   * 「编辑/删除」按钮显隐。
   */
  canManage?: number
  /** 子渠道（/tree 接口直接返回） */
  children?: ChannelInfo[]
  createdAt?: string
  updatedAt?: string
}

/**
 * 渠道架构查询参数（后端 ChannelInfoQueryDTO）。
 */
export interface ChannelInfoQuery {
  /** 上级渠道编码 */
  parentCode?: string
  /** 渠道编码 */
  channelCode?: string
  /** 渠道全称（模糊匹配） */
  fullName?: string
  /** 渠道类型 */
  channelType?: ChannelType
  /** 状态：1启用 0禁用 */
  status?: ChannelStatus
  /** 审核状态：0待审 1通过 2驳回 */
  auditStatus?: ChannelAuditStatus
}

// ==================== 渠道账号（ChannelAccount）====================

/** 账号状态：0锁定/1正常/2禁用（DDL 权威） */
export const CHANNEL_ACCOUNT_STATUS_OPTIONS = [
  { label: '锁定', value: 0 },
  { label: '正常', value: 1 },
  { label: '禁用', value: 2 }
] as const

/** 是否管理员：0否/1是 */
export const CHANNEL_IS_ADMIN_OPTIONS = [
  { label: '否', value: 0 },
  { label: '是', value: 1 }
] as const

/**
 * 渠道账号实体（后端 ChannelAccountVO，不含密码/salt）。
 *
 * 主键 id 自增 Long，业务键 accountCode（CA 前缀，服务端生成）。
 * 路径参数用 accountCode（非 id）。
 */
export interface ChannelAccount {
  id?: number
  /** 所属渠道编码 */
  channelCode: string
  /** 账号编码（业务键，服务端生成） */
  accountCode?: string
  /** 登录账号 */
  username: string
  /** 真实姓名 */
  realName?: string
  /** 头像 */
  avatar?: string
  /** 手机号 */
  phone?: string
  /** 微信 openId */
  openId?: string
  /** 微信 unionId */
  unionId?: string
  /** 邮箱 */
  email?: string
  /** 职位 */
  position?: string
  /** 最近登录时间 */
  lastLoginTime?: string
  /** 最近登录 IP */
  lastLoginIp?: string
  /** 登录次数 */
  loginCount?: number
  /** 账号状态：0锁定/1正常/2禁用 */
  accountStatus?: number
  /** 是否管理员：0否/1是 */
  isAdmin?: number
  createdAt?: string
}

/** 渠道账号分页查询参数（后端 ChannelAccountQueryDTO） */
export interface ChannelAccountQuery extends PageQuery {
  /** 所属渠道编码 */
  channelCode?: string
  /** 登录账号（模糊匹配） */
  username?: string
  /** 真实姓名（模糊匹配） */
  realName?: string
  /** 账号状态 */
  accountStatus?: number
}

// ==================== 渠道角色（ChannelRole）====================

/** 角色类型：1系统预置/2自定义（DDL 权威） */
export const CHANNEL_ROLE_TYPE_OPTIONS = [
  { label: '系统预置', value: 1 },
  { label: '自定义', value: 2 }
] as const

/** 角色状态：0禁用/1启用 */
export const CHANNEL_ROLE_STATUS_OPTIONS = [
  { label: '禁用', value: 0 },
  { label: '启用', value: 1 }
] as const

/**
 * 渠道角色实体（后端无独立 VO，直接返回 Entity + BaseEntity 审计字段）。
 *
 * 主键 id 自增 Long，业务键 roleCode（RL 前缀，服务端生成，渠道内唯一）。
 * 路径参数用 roleCode（非 id）。
 */
export interface ChannelRole {
  id?: number
  /** 所属渠道编码 */
  channelCode: string
  /** 角色编码（业务键，服务端生成，RL 前缀） */
  roleCode?: string
  /** 角色名称 */
  roleName: string
  /** 角色类型：1系统预置/2自定义 */
  roleType?: number
  /** 角色描述 */
  description?: string
  /** 状态：0禁用/1启用 */
  status?: number
  /** 排序号 */
  sortOrder?: number
  createdAt?: string
  updatedAt?: string
}

/** 渠道角色分页查询参数（后端 ChannelRoleQueryDTO） */
export interface ChannelRoleQuery extends PageQuery {
  /** 所属渠道编码 */
  channelCode?: string
  /** 角色名称（模糊匹配） */
  roleName?: string
  /** 角色类型 */
  roleType?: number
  /** 状态 */
  status?: number
}

// ==================== 渠道权限（ChannelPermission，增量2新增）====================

/** 权限类型：1菜单 2按钮 3接口 */
export const CHANNEL_PERMISSION_TYPE_OPTIONS = [
  { label: '菜单', value: 1 },
  { label: '按钮', value: 2 },
  { label: '接口', value: 3 }
] as const

/**
 * 渠道权限实体（后端 ChannelPermission Entity）。
 *
 * 与 admin 端 OrganPermission 同构，但渠道权限是渠道域独立的权限模型，
 * 由 /channel-api/channel-permissions 暴露。权限授权树由 /tree 接口直接组装。
 */
export interface ChannelPermission {
  id?: number
  /** 权限编码（主键业务码） */
  permissionCode: string
  /** 权限名称 */
  permissionName: string
  /** 父权限编码（顶级为 null/空） */
  parentCode?: string | null
  /** 权限类型：1菜单 2按钮 3接口 */
  permissionType?: number
  /** 资源路径（接口/页面路径） */
  path?: string
  /** 请求方法（接口类权限用：GET/POST/PUT/DELETE） */
  method?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 0禁用 */
  status?: number
  /** 子权限（/tree 接口返回时填充，供 el-tree 渲染） */
  children?: ChannelPermission[]
  createdAt?: string
  updatedAt?: string
}
