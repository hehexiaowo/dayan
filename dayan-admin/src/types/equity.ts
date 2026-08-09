/**
 * 权益域相关类型。
 *
 * 字段对齐后端 com.dayan.equity 包下实体：
 * - EquityTemplate（权益模板）/ EquityBatch（批次）/ EquityDepot（权益仓库）。
 *
 * 类型约定：Integer→number，BigDecimal→number，LocalDate/LocalDateTime→string。
 */
import type { PageQuery } from '@/types/common'

// ---------------- 权益类型（模板/仓库共用） ----------------

/**
 * 权益类型：对齐后端 DDL 10_equity.sql:17。
 * 1=机构入住权益 / 2=机构参观权益 / 3=场景活动权益 / 4=居家护理权益 /
 * 5=健康检测权益 / 6=课程学习权益 / 7=旅居体验权益。
 */
export enum EquityType {
  /** 机构入住权益 */
  INSTITUTION_STAY = 1,
  /** 机构参观权益 */
  INSTITUTION_VISIT = 2,
  /** 场景活动权益 */
  SCENE_ACTIVITY = 3,
  /** 居家护理权益 */
  HOME_CARE = 4,
  /** 健康检测权益 */
  HEALTH_CHECK = 5,
  /** 课程学习权益 */
  COURSE_LEARNING = 6,
  /** 旅居体验权益 */
  SOJOURN_EXPERIENCE = 7
}

/** 权益类型选项 */
export const EQUITY_TYPE_OPTIONS = [
  { label: '机构入住权益', value: EquityType.INSTITUTION_STAY },
  { label: '机构参观权益', value: EquityType.INSTITUTION_VISIT },
  { label: '场景活动权益', value: EquityType.SCENE_ACTIVITY },
  { label: '居家护理权益', value: EquityType.HOME_CARE },
  { label: '健康检测权益', value: EquityType.HEALTH_CHECK },
  { label: '课程学习权益', value: EquityType.COURSE_LEARNING },
  { label: '旅居体验权益', value: EquityType.SOJOURN_EXPERIENCE }
] as const

/**
 * 权益等级：对齐后端 DDL 10_equity.sql:18。
 * 1=基础 / 2=标准 / 3=高级 / 4=尊享 / 5=定制。
 */
export enum EquityLevel {
  /** 基础 */
  BASIC = 1,
  /** 标准 */
  STANDARD = 2,
  /** 高级 */
  PREMIUM = 3,
  /** 尊享 */
  LUXURY = 4,
  /** 定制 */
  CUSTOM = 5
}

/** 权益等级选项 */
export const EQUITY_LEVEL_OPTIONS = [
  { label: '基础', value: EquityLevel.BASIC },
  { label: '标准', value: EquityLevel.STANDARD },
  { label: '高级', value: EquityLevel.PREMIUM },
  { label: '尊享', value: EquityLevel.LUXURY },
  { label: '定制', value: EquityLevel.CUSTOM }
] as const

// ---------------- 批次状态 ----------------

/** 批次状态：对齐后端 DDL 10_equity.sql batch_status。0=待生产 / 1=生产中 / 2=已完成 / 3=已出库 / 4=已关闭 */
export enum BatchStatus {
  /** 待生产 */
  PENDING_PRODUCE = 0,
  /** 生产中 */
  PRODUCING = 1,
  /** 已完成 */
  COMPLETED = 2,
  /** 已出库 */
  OUT_BOUND = 3,
  /** 已关闭 */
  CLOSED = 4
}

/** 批次状态选项 */
export const BATCH_STATUS_OPTIONS = [
  { label: '待生产', value: BatchStatus.PENDING_PRODUCE },
  { label: '生产中', value: BatchStatus.PRODUCING },
  { label: '已完成', value: BatchStatus.COMPLETED },
  { label: '已出库', value: BatchStatus.OUT_BOUND },
  { label: '已关闭', value: BatchStatus.CLOSED }
] as const

// ---------------- 权益状态 ----------------

/** 权益状态：对齐后端 DDL 10_equity.sql equity_status。0=库存中 / 1=已出库 / 2=已激活 / 3=使用中 / 4=已完成 / 5=已过期 / 6=已作废 / 7=更换权益人中 */
export enum EquityStatus {
  /** 库存中 */
  STOCK = 0,
  /** 已出库 */
  OUTBOUND = 1,
  /** 已激活 */
  ACTIVATED = 2,
  /** 使用中 */
  IN_USE = 3,
  /** 已完成 */
  COMPLETED = 4,
  /** 已过期 */
  EXPIRED = 5,
  /** 已作废 */
  VOID = 6,
  /** 更换权益人中 */
  CHANGING_HOLDER = 7
}

/** 权益状态选项 */
export const EQUITY_STATUS_OPTIONS = [
  { label: '库存中', value: EquityStatus.STOCK },
  { label: '已出库', value: EquityStatus.OUTBOUND },
  { label: '已激活', value: EquityStatus.ACTIVATED },
  { label: '使用中', value: EquityStatus.IN_USE },
  { label: '已完成', value: EquityStatus.COMPLETED },
  { label: '已过期', value: EquityStatus.EXPIRED },
  { label: '已作废', value: EquityStatus.VOID },
  { label: '更换权益人中', value: EquityStatus.CHANGING_HOLDER }
] as const

// ---------------- 载体类型 ----------------

/**
 * 载体类型：对齐后端运行时校验（EquityDepotServiceImpl 只允许 1/2）。
 * 1=权益卡 / 2=权益函。
 */
export enum CarrierType {
  /** 权益卡 */
  EQUITY_CARD = 1,
  /** 权益函 */
  EQUITY_LETTER = 2
}

/** 载体类型选项 */
export const CARRIER_TYPE_OPTIONS = [
  { label: '权益卡', value: CarrierType.EQUITY_CARD },
  { label: '权益函', value: CarrierType.EQUITY_LETTER }
] as const

// ---------------- 权益模板 ----------------

/**
 * 权益模板实体（后端 EquityTemplate）。
 *
 * 主键 templateCode 服务端生成，新增表单不含该字段。
 */
export interface EquityTemplate {
  id?: number
  /** 模板编码（服务端生成） */
  templateCode?: string
  /** 模板名称 */
  templateName: string
  /** 权益类型 */
  equityType?: EquityType
  /** 权益等级 */
  equityLevel?: EquityLevel
  /** 权益面值 */
  equityValue?: number
  /** 成本价 */
  costPrice?: number
  /** 权益内容描述 */
  contentDescription?: string
  /** 包含服务项目 */
  serviceItems?: string
  /** 适用机构范围 */
  applicableParks?: string
  /** 适用城市范围 */
  applicableCities?: string
  /** 激活后有效天数 */
  validDays?: number
  /** 库存有效期天数 */
  shelfLifeDays?: number
  /** 是否可转让：0否 1是 */
  isTransferable?: number
  /** 是否可叠加：0否 1是 */
  isStackable?: number
  /** 最大使用次数 */
  maxUseCount?: number
  /** 封面图 URL */
  coverImage?: string
  /** 卡面设计图 URL */
  cardDesignUrl?: string
  /** 使用说明/条款 */
  terms?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 0禁用 */
  status?: number
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createdAt?: string
  /** 更新时间 */
  updatedAt?: string
}

/**
 * 权益模板分页查询参数（后端 EquityTemplateQueryDTO）。
 */
export interface EquityTemplateQuery extends PageQuery {
  templateCode?: string
  templateName?: string
  equityType?: EquityType
  equityLevel?: EquityLevel
  status?: number
}

// ---------------- 权益批次 ----------------

/**
 * 权益批次实体（后端 EquityBatch，雪花ID）。
 *
 * 主键 batchCode 服务端生成，新增表单不含该字段。
 */
export interface EquityBatch {
  id?: number
  /** 批次编码（服务端生成） */
  batchCode?: string
  /** 批次名称 */
  batchName: string
  /** 权益模板编码 */
  templateCode?: string
  /** 分配渠道编码 */
  channelCode?: string
  /** 总数量 */
  totalQuantity?: number
  /** 已生成数量 */
  producedCount?: number
  /** 已分配数量 */
  allocatedCount?: number
  /** 已出库数量 */
  outboundCount?: number
  /** 已激活数量 */
  activatedCount?: number
  /** 已使用数量 */
  usedCount?: number
  /** 已过期数量 */
  expiredCount?: number
  /** 已作废数量 */
  voidedCount?: number
  /** 剩余可用数量 */
  remainCount?: number
  /** 单位成本 */
  unitCost?: number
  /** 批次总成本 */
  totalCost?: number
  /** 生产日期（yyyy-MM-dd） */
  produceDate?: string
  /** 批次有效期（yyyy-MM-dd） */
  expireDate?: string
  /** 批次状态：0草稿/1生产中/2已完成/3已作废 */
  batchStatus?: BatchStatus
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createdAt?: string
  /** 更新时间 */
  updatedAt?: string
}

/**
 * 权益批次分页查询参数（后端 EquityBatchQueryDTO）。
 */
export interface EquityBatchQuery extends PageQuery {
  batchCode?: string
  batchName?: string
  templateCode?: string
  channelCode?: string
  batchStatus?: BatchStatus
}

/** 批次统计（GET /equity/batch/stats/{batchCode}） */
export interface EquityBatchStats {
  batchCode?: string
  totalQuantity?: number
  producedCount?: number
  allocatedCount?: number
  outboundCount?: number
  activatedCount?: number
  usedCount?: number
  expiredCount?: number
  voidedCount?: number
  remainCount?: number
}

// ---------------- 权益仓库 ----------------

/**
 * 权益仓库实体（后端 EquityDepot，雪花ID）。
 *
 * 主键 equityCode 服务端生成。
 * 表单只覆盖 15 个核心字段（详见 depot/index.vue）。
 */
export interface EquityDepot {
  id?: number
  /** 权益编码（服务端生成） */
  equityCode?: string
  /** 权益卡号 */
  equityNo?: string
  /** 权益模板编码 */
  templateCode?: string
  /** 批次编码 */
  batchCode?: string
  /** 权益类型 */
  equityType?: EquityType
  /** 权益面值 */
  equityValue?: number
  /** 分配渠道编码 */
  channelCode?: string
  /** 分配代理人编码 */
  agentCode?: string
  /** 领取客户编码 */
  clientCode?: string
  /** 载体类型 */
  carrierType?: CarrierType
  /** 权益状态 */
  equityStatus?: EquityStatus
  /** 激活码 */
  activateCode?: string
  /** 绑定码 */
  bindCode?: string
  /** 二维码 URL */
  qrCodeUrl?: string
  /** 物流单号 */
  logisticsNo?: string
  /** 生产时间（yyyy-MM-dd HH:mm:ss） */
  produceTime?: string
  /** 分配时间 */
  allocateTime?: string
  /** 出库时间 */
  outboundTime?: string
  /** 激活时间 */
  activateTime?: string
  /** 过期时间 */
  expireTime?: string
  /** 备注 */
  remark?: string
  /** 作废原因 */
  voidReason?: string
  /** 创建时间 */
  createdAt?: string
  /** 更新时间 */
  updatedAt?: string
}

/**
 * 权益仓库分页查询参数（后端 EquityDepotQueryDTO）。
 */
export interface EquityDepotQuery extends PageQuery {
  equityCode?: string
  equityNo?: string
  templateCode?: string
  batchCode?: string
  channelCode?: string
  agentCode?: string
  clientCode?: string
  carrierType?: CarrierType
  equityStatus?: EquityStatus
}
