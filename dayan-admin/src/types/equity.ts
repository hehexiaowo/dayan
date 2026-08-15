/**
 * 权益域相关类型。
 *
 * 字段对齐后端 com.dayan.equity 包下实体：
 * - EquityBatch（批次）/ EquityDepot（权益仓库）。
 *
 * 类型约定：Integer→number，BigDecimal→number，LocalDate/LocalDateTime→string。
 */
import type { PageQuery } from '@/types/common'

// ---------------- 权益类型（模板/仓库共用） ----------------

/**
 * 权益类型：对齐后端 DDL 10_equity.sql:17。
 * 1=机构入住权益 / 2=机构参观权益 / 3=场景活动权益 / 4=居家护理权益 /
 * 5=健康检测权益 / 6=课程学习权益 / 7=旅游短居体验权益。
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
  /** 旅游短居体验权益 */
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
  { label: '旅游短居体验权益', value: EquityType.SOJOURN_EXPERIENCE }
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
  /** 关联商品编码 */
  goodsCode?: string
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
  goodsCode?: string
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
  /** 关联商品编码 */
  goodsCode?: string
  /** 批次编码 */
  batchCode?: string
  /** 使用人人数快照 */
  personCount?: number
  /** 激活后有效天数快照 */
  validDays?: number
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
  goodsCode?: string
  batchCode?: string
  channelCode?: string
  agentCode?: string
  clientCode?: string
  carrierType?: CarrierType
  equityStatus?: EquityStatus
}

// ---------------- 激活渠道 ----------------

/**
 * 权益激活渠道：对齐后端 EquityActivateVO.activateChannel。
 * 1=APP / 2=小程序 / 3=H5 / 4=管家代激活 / 5=代理人代激活。
 */
export enum ActivateChannel {
  /** APP */
  APP = 1,
  /** 小程序 */
  MINI_PROGRAM = 2,
  /** H5 */
  H5 = 3,
  /** 管家代激活 */
  MANAGER = 4,
  /** 代理人代激活 */
  AGENT = 5
}

/** 激活渠道选项 */
export const ACTIVATE_CHANNEL_OPTIONS = [
  { label: 'APP', value: ActivateChannel.APP },
  { label: '小程序', value: ActivateChannel.MINI_PROGRAM },
  { label: 'H5', value: ActivateChannel.H5 },
  { label: '管家代激活', value: ActivateChannel.MANAGER },
  { label: '代理人代激活', value: ActivateChannel.AGENT }
] as const

// ---------------- 换持有人状态 ----------------

/**
 * 换持有人状态：对齐后端 EquityChangeHolderVO.changeStatus。
 * 0=待处理 / 1=已完成 / 2=已回滚。
 */
export enum ChangeHolderStatus {
  /** 待处理 */
  PENDING = 0,
  /** 已完成 */
  DONE = 1,
  /** 已回滚 */
  ROLLED_BACK = 2
}

/** 换持有人状态选项 */
export const CHANGE_HOLDER_STATUS_OPTIONS = [
  { label: '待处理', value: ChangeHolderStatus.PENDING },
  { label: '已完成', value: ChangeHolderStatus.DONE },
  { label: '已回滚', value: ChangeHolderStatus.ROLLED_BACK }
] as const

// ---------------- 权益激活记录 ----------------

/**
 * 权益激活记录实体（后端 EquityActivateVO，雪花ID）。
 *
 * 记录由 depot 激活生命周期自动产生，管理端仅查询。
 */
export interface EquityActivate {
  id?: number
  /** 激活码 */
  activateCode?: string
  /** 权益编码 */
  equityCode?: string
  /** 商品编码 */
  goodsCode?: string
  /** 客户编码 */
  clientCode?: string
  /** 客户全名 */
  clientFullName?: string
  /** 客户手机号 */
  clientPhone?: string
  /** 激活渠道：1=APP / 2=小程序 / 3=H5 / 4=管家代激活 / 5=代理人代激活 */
  activateChannel?: ActivateChannel
  /** 激活来源编码 */
  activateSourceCode?: string
  /** 激活时间（yyyy-MM-dd HH:mm:ss） */
  activateTime?: string
  /** 过期时间（yyyy-MM-dd HH:mm:ss） */
  expireTime?: string
  /** 是否已实名核验：1=是 / 0=否 */
  isIdCardVerified?: number
  /** 是否已签署协议：1=是 / 0=否 */
  isAgreementSigned?: number
  /** IP 地址 */
  ipAddress?: string
  /** 设备信息 */
  deviceInfo?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createdAt?: string
}

/**
 * 权益激活记录分页查询参数（后端 EquityActivateQueryDTO）。
 */
export interface EquityActivateQuery extends PageQuery {
  activateCode?: string
  equityCode?: string
  goodsCode?: string
  clientCode?: string
  activateChannel?: ActivateChannel
}

// ---------------- 权益更换权益人记录 ----------------

/**
 * 权益更换权益人记录实体（后端 EquityChangeHolderVO，雪花ID）。
 *
 * 记录由 depot 换持有人生命周期自动产生，管理端仅查询。
 * oldPersonIdCard / newPersonIdCard 在管理端按需返回解密后明文。
 */
export interface EquityChangeHolder {
  id?: number
  /** 权益编码 */
  equityCode?: string
  /** 原使用人编码 */
  oldUsePersonCode?: string
  /** 原权益人姓名 */
  oldPersonName?: string
  /** 原权益人身份证号（明文） */
  oldPersonIdCard?: string
  /** 新使用人编码 */
  newUsePersonCode?: string
  /** 新权益人姓名 */
  newPersonName?: string
  /** 新权益人身份证号（明文） */
  newPersonIdCard?: string
  /** 更换原因 */
  changeReason?: string
  /** 更换状态：0=待处理 / 1=已完成 / 2=已回滚 */
  changeStatus?: ChangeHolderStatus
  /** 操作时间（yyyy-MM-dd HH:mm:ss） */
  operateTime?: string
  /** 操作人编码 */
  operatorCode?: string
  /** 创建时间 */
  createdAt?: string
}

/**
 * 权益更换权益人记录分页查询参数（后端 EquityChangeHolderQueryDTO）。
 */
export interface EquityChangeHolderQuery extends PageQuery {
  equityCode?: string
  changeStatus?: ChangeHolderStatus
  operatorCode?: string
}

// ---------------- 权益使用人 ----------------

/**
 * 权益使用人实体（后端 EquityUsePersonVO，雪花ID）。
 *
 * id 序列化为字符串（雪花ID超 JS 安全整数，前端按 string 处理避免精度丢失）。
 * usePersonIdCard 在管理端返回解密后明文。
 * usePersonGender 复用 @/types/client 的 Gender（0未知 1男 2女）。
 */
export interface EquityUsePerson {
  /** 主键（雪花ID，后端序列化为字符串，前端按 string 处理） */
  id?: string
  /** 权益编码 */
  equityCode?: string
  /** 权益持有人编码 */
  clientCode?: string
  /** 使用人姓名 */
  usePersonName?: string
  /** 使用人性别：0未知 1男 2女（复用 Gender） */
  usePersonGender?: number
  /** 使用人出生日期（yyyy-MM-dd） */
  usePersonBirthday?: string
  /** 使用人年龄 */
  usePersonAge?: number
  /** 使用人手机号 */
  usePersonPhone?: string
  /** 使用人身份证号（明文） */
  usePersonIdCard?: string
  /** 与持有人关系（字典code：self/spouse/parent/parent_in_law/child/other） */
  relationWithHolder?: string
  /** 健康状况简述 */
  healthStatus?: string
  /** 照护需求简述 */
  careNeed?: string
  /** 是否默认权益人：1=是 / 0=否 */
  isDefaultHolder?: number
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createdAt?: string
}

/** 与持有人关系字典（system_dict: relation_with_holder，存 code 显示 label） */
export const RELATION_WITH_HOLDER_OPTIONS = [
  { value: 'self', label: '本人' },
  { value: 'spouse', label: '配偶' },
  { value: 'parent', label: '父母（含公婆/岳父母）' },
  { value: 'parent_in_law', label: '公婆/岳父母' },
  { value: 'child', label: '子女' },
  { value: 'other', label: '其他' },
] as const

/** 关系 code → 中文标签（未知值原样返回，兼容存量自由文本） */
export function relationLabel(v?: string | null): string {
  if (!v) return '--'
  return RELATION_WITH_HOLDER_OPTIONS.find(o => o.value === v)?.label ?? v
}

/**
 * 权益使用人分页查询参数（后端 EquityUsePersonQueryDTO）。
 */
export interface EquityUsePersonQuery extends PageQuery {
  equityCode?: string
  clientCode?: string
  usePersonName?: string
  isDefaultHolder?: number
}

/** 设置默认权益人入参（后端 SetDefaultHolderDTO）。 */
export interface SetDefaultHolderPayload {
  /** 使用人 id */
  id: string
  /** 权益编码 */
  equityCode: string
}
