/**
 * 渠道相关类型。
 *
 * 字段对齐后端 com.dayan.channel.entity.ChannelInfo（表 channel_info）。
 *
 * 注意：后端渠道控制器为 RESTful 复数风格（/admin-api/channels），且 list 接口
 * 返回 List 非 PageResult（无分页字段）。前端以树形表格展示，树数据由 /tree 接口
 * 直接返回，或由 buildChannelTree 基于平铺列表组树。
 */
import type { PageQuery } from '@/types/common'
import { AccountStatus } from '@/types/account'

/** 渠道类型（企业类型）：1=保险公司 2=银行机构 3=保险中介 4=其他企业 */
export enum ChannelType {
  /** 保险公司 */
  INSURANCE = 1,
  /** 银行机构 */
  BANK = 2,
  /** 保险中介 */
  INTERMEDIARY = 3,
  /** 其他企业 */
  ENTERPRISE = 4
}

/** 渠道类型选项 */
export const CHANNEL_TYPE_OPTIONS = [
  { label: '保险公司', value: ChannelType.INSURANCE },
  { label: '银行机构', value: ChannelType.BANK },
  { label: '保险中介', value: ChannelType.INTERMEDIARY },
  { label: '其他企业', value: ChannelType.ENTERPRISE }
] as const

/** 渠道状态（合作状态）：0=待审核 1=合作中 2=已暂停 3=已终止（DDL 权威） */
export enum ChannelStatus {
  /** 待审核 */
  PENDING = 0,
  /** 合作中 */
  COOPERATING = 1,
  /** 已暂停 */
  PAUSED = 2,
  /** 已终止 */
  TERMINATED = 3
}

/** 渠道状态选项 */
export const CHANNEL_STATUS_OPTIONS = [
  { label: '待审核', value: ChannelStatus.PENDING },
  { label: '合作中', value: ChannelStatus.COOPERATING },
  { label: '已暂停', value: ChannelStatus.PAUSED },
  { label: '已终止', value: ChannelStatus.TERMINATED }
] as const

/** 结算周期：1=月结 2=季结（DDL 权威） */
export const CHANNEL_SETTLEMENT_CYCLE_OPTIONS = [
  { label: '月结', value: 1 },
  { label: '季结', value: 2 }
] as const

/** 管理配置能力：0=业务型（仅业务操作），1=管理型（可建删子渠道+配置app） */
export const CHANNEL_CAN_MANAGE_OPTIONS = [
  { label: '业务型', value: 0 },
  { label: '管理型', value: 1 }
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
 */
export interface ChannelInfo {
  id?: number
  /** 渠道编码（主键，服务端生成） */
  channelCode?: string
  /** 渠道全称 */
  fullName: string
  /** 渠道简称 */
  shortName?: string
  /** 渠道类型（企业类型）：1保险公司 2银行机构 3保险中介 4其他企业 */
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
  /** 结算周期：1=月结 2=季结 */
  settlementCycle?: number
  /** 渠道功能开关配置（JSON 字符串） */
  featureConfig?: string
  /** 管理配置能力：0=业务型，1=管理型 */
  canManage?: number
  /** 排序号 */
  sortOrder?: number
  /** 状态（合作状态）：0待审核 1合作中 2已暂停 3已终止 */
  status?: ChannelStatus
  /** 审核状态：0待审 1通过 2驳回 */
  auditStatus?: ChannelAuditStatus
  /** 备注 */
  remark?: string
  /** 子渠道（前端组树时填充，或 /tree 接口直接返回） */
  children?: ChannelInfo[]
  createdAt?: string
  updatedAt?: string
}

/**
 * 渠道列表查询参数（后端 ChannelInfoQueryDTO）。
 *
 * 后端 list 接口返回 List 非 PageResult，无分页字段。
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
  /** 状态（合作状态）：0待审核 1合作中 2已暂停 3已终止 */
  status?: ChannelStatus
  /** 审核状态：0待审 1通过 2驳回 */
  auditStatus?: ChannelAuditStatus
  /** 按所属分销商筛选 */
  distributorCode?: string
}

/**
 * 将平铺渠道列表构建为树形结构。
 *
 * 后端 /channels 接口返回平铺列表时，前端调用此方法组树展示；
 * 后端 /channels/tree 接口已直接返回树结构，则无需调用。
 */
export function buildChannelTree(list: ChannelInfo[]): ChannelInfo[] {
  const map = new Map<string, ChannelInfo & { children?: ChannelInfo[] }>()
  list.forEach((item) => {
    if (item.channelCode) {
      map.set(item.channelCode, { ...item, children: [] })
    }
  })
  const roots: ChannelInfo[] = []
  map.forEach((node) => {
    if (node.parentCode && map.has(node.parentCode)) {
      ;(map.get(node.parentCode)!.children as ChannelInfo[]).push(node)
    } else {
      roots.push(node)
    }
  })
  // 移除空 children 数组（避免叶子节点显示展开箭头）
  const clean = (nodes: ChannelInfo[]) =>
    nodes.forEach((n) => {
      const node = n as ChannelInfo & { children?: ChannelInfo[] }
      if (node.children && node.children.length === 0) {
        delete node.children
      } else if (node.children) {
        clean(node.children)
      }
    })
  clean(roots)
  // 按 sortOrder 升序排序
  const sortNodes = (nodes: ChannelInfo[]) => {
    nodes.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    nodes.forEach((n) => {
      const node = n as ChannelInfo & { children?: ChannelInfo[] }
      if (node.children && node.children.length > 0) sortNodes(node.children)
    })
  }
  sortNodes(roots)
  return roots
}

// ==================== 子表：渠道账户（ChannelAccount）====================

/** 账号状态：0锁定/1正常/2禁用（DDL 权威） */
export const CHANNEL_ACCOUNT_STATUS_OPTIONS = [
  { label: '锁定', value: AccountStatus.LOCKED },
  { label: '正常', value: AccountStatus.NORMAL },
  { label: '禁用', value: AccountStatus.DISABLED }
] as const

/** 是否管理员：0否/1是 */
export const CHANNEL_IS_ADMIN_OPTIONS = [
  { label: '否', value: 0 },
  { label: '是', value: 1 }
] as const

/**
 * 渠道账户实体（后端 ChannelAccountVO，不含密码/salt）。
 *
 * 主键 id 自增 Long，业务键 accountCode（CA 前缀，服务端生成）。
 * 路径参数用 accountCode（非 id）。
 */
export interface ChannelAccount {
  id?: number
  channelCode: string
  /** 账号编码（业务键，服务端生成） */
  accountCode?: string
  username: string
  realName?: string
  avatar?: string
  phone?: string
  openId?: string
  unionId?: string
  email?: string
  position?: string
  lastLoginTime?: string
  lastLoginIp?: string
  loginCount?: number
  /** 账号状态：0锁定/1正常/2禁用 */
  accountStatus?: number
  /** 是否管理员：0否/1是 */
  isAdmin?: number
  createdAt?: string
}

export interface ChannelAccountQuery extends PageQuery {
  channelCode?: string
  username?: string
  realName?: string
  accountStatus?: number
}

// ==================== 子表：渠道角色（ChannelRole）====================

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
  channelCode: string
  /** 角色编码（业务键，服务端生成，RL 前缀） */
  roleCode?: string
  roleName: string
  /** 角色类型：1系统预置/2自定义 */
  roleType?: number
  description?: string
  /** 状态：0禁用/1启用 */
  status?: number
  sortOrder?: number
  createdAt?: string
  updatedAt?: string
}

export interface ChannelRoleQuery extends PageQuery {
  channelCode?: string
  roleName?: string
  roleType?: number
  status?: number
}

// ==================== 子表：开放平台对接（ChannelOpenPlatform）====================

/**
 * 对接类型：1H5嵌入/2API对接/3SDK集成（DDL 权威）。
 *
 * 注：CreateDTO 的 Java 注释写「1=API/2=H5/3=小程序」与 DDL 矛盾，前端采信 DDL。
 */
export const CHANNEL_DOCK_TYPE_OPTIONS = [
  { label: 'H5嵌入', value: 1 },
  { label: 'API对接', value: 2 },
  { label: 'SDK集成', value: 3 }
] as const

/** 认证方式：1Token/2签名 */
export const CHANNEL_AUTH_TYPE_OPTIONS = [
  { label: 'Token', value: 1 },
  { label: '签名', value: 2 }
] as const

/** 开放平台状态：0禁用/1启用 */
export const CHANNEL_PLATFORM_STATUS_OPTIONS = [
  { label: '禁用', value: 0 },
  { label: '启用', value: 1 }
] as const

/**
 * 开放平台对接实体（后端 ChannelOpenPlatformVO）。
 *
 * 主键 id 自增 Long。**与其他子表不同：详情/更新/删除走 id（Long），不走编码字符串。**
 * appSecret 出参为脱敏占位 `***`（明文不回传）；编辑时留空=不改，填值=轮换。
 */
export interface ChannelOpenPlatform {
  id?: number
  channelCode: string
  platformName: string
  /** 对接类型：1H5嵌入/2API对接/3SDK集成 */
  dockType?: number
  apiBaseUrl?: string
  appKey?: string
  /** 脱敏后的密钥占位（明文不回传）；编辑留空不改，填值轮换 */
  appSecret?: string
  callbackUrl?: string
  h5Domain?: string
  h5Theme?: string
  /** 认证方式：1Token/2签名 */
  authType?: number
  /** IP白名单（JSON 数组字符串） */
  ipWhitelist?: string
  /** 调用频率限制（次/分钟） */
  rateLimit?: number
  /** 超时时间（秒） */
  timeout?: number
  /** 扩展配置（JSON 字符串） */
  extraConfig?: string
  /** 状态：0禁用/1启用 */
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface ChannelOpenPlatformQuery extends PageQuery {
  channelCode?: string
  platformName?: string
  dockType?: number
  status?: number
}

// ==================== 子表：渠道配置（Config content/scene/goods，list+save 全量覆盖模式）====================

/**
 * 渠道配置内容（ChannelConfigContent）。
 *
 * 非 CRUD，而是「先删后增全量覆盖」：save 发整个 List，后端 setId(null) 忽略 id。
 * 前端 UI 范式：可编辑表格 + 整体「保存配置」按钮。
 */
export interface ChannelConfigContent {
  id?: number
  channelCode?: string
  contentCode: string
  /** 内容类型：1文章/2视频/3图片/4专题 */
  contentType: number
  /** 展示端类型：agent=代理人端/client=客户端（字符串枚举，非数字） */
  appType: string
  /** 展示位置：banner/recommend/hot/new 等 */
  position?: string
  sortOrder?: number
  /** 是否置顶：0否/1是 */
  isTop?: number
  effectiveTime?: string
  expireTime?: string
  /** 状态：0禁用/1启用 */
  status?: number
}

/** 内容类型：1文章/2视频/3图片/4专题 */
export const CHANNEL_CONTENT_TYPE_OPTIONS = [
  { label: '文章', value: 1 },
  { label: '视频', value: 2 },
  { label: '图片', value: 3 },
  { label: '专题', value: 4 }
] as const

/** 展示端类型：agent/client（字符串） */
export const CHANNEL_APP_TYPE_OPTIONS = [
  { label: '代理人端', value: 'agent' },
  { label: '客户端', value: 'client' }
] as const

/** 内容展示位置选项（非枚举，按 DDL 注释的示例值列出） */
export const CHANNEL_CONTENT_POSITION_OPTIONS = [
  { label: 'Banner', value: 'banner' },
  { label: '推荐', value: 'recommend' },
  { label: '热门', value: 'hot' },
  { label: '最新', value: 'new' }
] as const

/**
 * 渠道配置场景（ChannelConfigScene）。
 *
 * 关联到 scene 主档案的 sceneCode，可自定义名称/价格（渠道专属价）。
 */
export interface ChannelConfigScene {
  id?: number
  channelCode?: string
  sceneCode: string
  /** 是否渠道专属：0否/1是 */
  isExclusive?: number
  /** 自定义场景名称（渠道定制） */
  customName?: string
  /** 自定义价格 */
  customPrice?: number
  sortOrder?: number
  effectiveTime?: string
  expireTime?: string
  /** 状态：0禁用/1启用 */
  status?: number
}

/**
 * 渠道配置商品（ChannelConfigGoods）。
 *
 * 关联到 goods 主档案的 goodsCode，可自定义名称/价格/描述/采购限制。
 */
export interface ChannelConfigGoods {
  id?: number
  channelCode?: string
  goodsCode: string
  /** 商品类型：1养老权益/2场景营销/3培训课程/4旅游短居（与 goods 域 GoodsType 对齐） */
  goodsType?: number
  customName?: string
  /** 自定义价格 */
  customPrice?: number
  customDescription?: string
  /** 是否渠道专属：0否/1是 */
  isExclusive?: number
  /** 采购限制数量（null=不限） */
  purchaseLimit?: number
  sortOrder?: number
  effectiveTime?: string
  expireTime?: string
  /** 状态：0禁用/1启用 */
  status?: number
}

/** 通用启用/禁用（Config 三类共用） */
export const CHANNEL_CONFIG_STATUS_OPTIONS = [
  { label: '禁用', value: 0 },
  { label: '启用', value: 1 }
] as const

/** 是否（渠道专属/置顶等 0/1 字段共用） */
export const CHANNEL_YES_NO_OPTIONS = [
  { label: '否', value: 0 },
  { label: '是', value: 1 }
] as const

// ==================== 子表：渠道配置（Config course/tool，list+save 全量覆盖模式）====================

/**
 * 渠道配置课程（ChannelConfigCourse）。
 *
 * 关联到 course 主档案的 courseCode，config_type=0 为基础可见性配置。
 */
export interface ChannelConfigCourse {
  id?: number
  channelCode?: string
  courseCode: string
  /** 配置类型（0=基础可见性，预留扩展） */
  configType?: number
  /** 配置内容 JSON（格式随 config_type 不同） */
  configJson?: string
  /** 状态：0禁用/1启用 */
  status?: number
}

/**
 * 渠道配置工具（ChannelConfigTool）。
 *
 * 关联到 tool 主档案的 toolCode，config_type=0 为基础可见性配置，
 * config_type=1 为问答人物知识库补充。
 */
export interface ChannelConfigTool {
  id?: number
  channelCode?: string
  toolCode: string
  /** 配置类型（0=基础可见性，1=问答人物知识库补充） */
  configType?: number
  /** 配置内容 JSON（格式随 config_type 不同） */
  configJson?: string
  /** 状态：0禁用/1启用 */
  status?: number
}
