/**
 * 渠道相关类型。
 *
 * 字段对齐后端 com.dayan.channel.entity.ChannelInfo（表 channel_info）。
 *
 * 注意：后端渠道控制器为 RESTful 复数风格（/admin-api/channels），且 list 接口
 * 返回 List 非 PageResult（无分页字段）。前端以树形表格展示，树数据由 /tree 接口
 * 直接返回，或由 buildChannelTree 基于平铺列表组树。
 */

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
 */
export interface ChannelInfo {
  id?: number
  /** 渠道编码（主键，服务端生成） */
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
  /** 分销商编码 */
  distributorCode?: string
  /** 结算周期 */
  settlementCycle?: number
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 0禁用 */
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
  /** 状态：1启用 0禁用 */
  status?: ChannelStatus
  /** 审核状态：0待审 1通过 2驳回 */
  auditStatus?: ChannelAuditStatus
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
