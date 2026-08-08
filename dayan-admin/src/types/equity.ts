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

/** 权益等级：1普通 / 2银卡 / 3金卡 / 4钻石 */
export enum EquityLevel {
  /** 普通 */
  NORMAL = 1,
  /** 银卡 */
  SILVER = 2,
  /** 金卡 */
  GOLD = 3,
  /** 钻石 */
  DIAMOND = 4
}

/** 权益等级选项 */
export const EQUITY_LEVEL_OPTIONS = [
  { label: '普通', value: EquityLevel.NORMAL },
  { label: '银卡', value: EquityLevel.SILVER },
  { label: '金卡', value: EquityLevel.GOLD },
  { label: '钻石', value: EquityLevel.DIAMOND }
] as const

// ---------------- 批次状态 ----------------

/** 批次状态：0草稿 / 1生产中 / 2已完成 / 3已作废 */
export enum BatchStatus {
  /** 草稿 */
  DRAFT = 0,
  /** 生产中 */
  PRODUCING = 1,
  /** 已完成 */
  COMPLETED = 2,
  /** 已作废 */
  VOIDED = 3
}

/** 批次状态选项 */
export const BATCH_STATUS_OPTIONS = [
  { label: '草稿', value: BatchStatus.DRAFT },
  { label: '生产中', value: BatchStatus.PRODUCING },
  { label: '已完成', value: BatchStatus.COMPLETED },
  { label: '已作废', value: BatchStatus.VOIDED }
] as const

// ---------------- 权益状态 ----------------

/** 权益状态：0待入库 / 1在库 / 2已出库 / 3已激活 / 4已使用 / 5已过期 / 6已作废 / 7变更中 */
export enum EquityStatus {
  /** 待入库 */
  PENDING_STOCK_IN = 0,
  /** 在库 */
  IN_STOCK = 1,
  /** 已出库 */
  OUT_BOUND = 2,
  /** 已激活 */
  ACTIVATED = 3,
  /** 已使用 */
  USED = 4,
  /** 已过期 */
  EXPIRED = 5,
  /** 已作废 */
  VOIDED = 6,
  /** 变更中 */
  CHANGING = 7
}

/** 权益状态选项 */
export const EQUITY_STATUS_OPTIONS = [
  { label: '待入库', value: EquityStatus.PENDING_STOCK_IN },
  { label: '在库', value: EquityStatus.IN_STOCK },
  { label: '已出库', value: EquityStatus.OUT_BOUND },
  { label: '已激活', value: EquityStatus.ACTIVATED },
  { label: '已使用', value: EquityStatus.USED },
  { label: '已过期', value: EquityStatus.EXPIRED },
  { label: '已作废', value: EquityStatus.VOIDED },
  { label: '变更中', value: EquityStatus.CHANGING }
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
