/**
 * 商品相关类型。
 *
 * 字段对齐后端 com.dayan.goods.entity.GoodsInfo 及子表（GoodsSkuEquity / GoodsScene /
 * GoodsCourse / GoodsSojourn）与对应的 DTO（DDL 来源：db/migration/12_goods.sql）。
 *
 * 枚举说明（重要，对齐 DDL 5 态）：
 * - GoodsType：4 值（1权益/2场景/3课程/4旅居），按 goodsType 互斥决定显示哪个 SKU 子表。
 * - GoodsStatus：5 态（0草稿/1待上架/2已上架/3已下架/4已售罄）。
 * - shelf 接口语义偏差：上下架接口 GoodsInfoShelfDTO 只支持 0/1 二态（0下架/1上架），
 *   与 DDL 5 态存在语义偏差（shelf 的 1 在 DDL 是"待上架"，而非"已上架"）。
 *   前端列表按 DDL 5 态展示，shelf 按钮按契约传 0/1，详见 api/goods.ts 的 TODO 注释。
 */
import type { PageQuery } from '@/types/common'
import { AuditStatus, AUDIT_STATUS_OPTIONS } from '@/types/scene'

/**
 * 商品类型：1=权益商品 / 2=场景商品 / 3=课程商品 / 4=旅居商品（对齐 DDL）。
 *
 * 创建后不可改（UpdateDTO 无此字段）；按该值互斥决定详情页显示哪个 SKU 子表。
 */
export enum GoodsType {
  /** 权益商品 */
  EQUITY = 1,
  /** 场景商品 */
  SCENE = 2,
  /** 课程商品 */
  COURSE = 3,
  /** 旅居商品 */
  SOJOURN = 4
}

/** 商品类型选项 */
export const GOODS_TYPE_OPTIONS = [
  { label: '权益商品', value: GoodsType.EQUITY },
  { label: '场景商品', value: GoodsType.SCENE },
  { label: '课程商品', value: GoodsType.COURSE },
  { label: '旅居商品', value: GoodsType.SOJOURN }
] as const

/**
 * 商品状态：0=草稿 / 1=待上架 / 2=已上架 / 3=已下架 / 4=已售罄（对齐 DDL）。
 *
 * 注意：create 强制 0（草稿），CreateDTO 不含 goodsStatus 字段；
 * 实际上下架由 shelf 接口控制（0/1 二态，语义有偏差，见文件头注释）。
 */
export enum GoodsStatus {
  /** 草稿 */
  DRAFT = 0,
  /** 待上架 */
  PENDING = 1,
  /** 已上架 */
  ON_SHELF = 2,
  /** 已下架 */
  OFF_SHELF = 3,
  /** 已售罄 */
  SOLD_OUT = 4
}

/** 商品状态选项 */
export const GOODS_STATUS_OPTIONS = [
  { label: '草稿', value: GoodsStatus.DRAFT },
  { label: '待上架', value: GoodsStatus.PENDING },
  { label: '已上架', value: GoodsStatus.ON_SHELF },
  { label: '已下架', value: GoodsStatus.OFF_SHELF },
  { label: '已售罄', value: GoodsStatus.SOLD_OUT }
] as const

/**
 * 审核状态（复用 scene 的枚举）：0待审 / 1通过 / 2驳回。
 */
export { AuditStatus as GoodsAuditStatus }
export { AUDIT_STATUS_OPTIONS as GOODS_AUDIT_STATUS_OPTIONS }

// ============================================================================
// 主表 GoodsInfo
// ============================================================================

/**
 * 商品信息实体（后端 GoodsInfo，表 goods_info）。
 *
 * 主键：物理 id（自增），业务键 goodsCode（服务端 CodeGenerator 生成 `GD`+5 位，新增表单不含）。
 * 只读字段（UpdateDTO 不含）：id / goodsCode / goodsType / salesCount / viewCount / collectCount / createdAt。
 */
export interface GoodsInfo {
  id?: number
  /** 商品编码（服务端生成，只读） */
  goodsCode?: string
  /** 商品名称 */
  goodsName: string
  /** 商品简称 */
  goodsShortName?: string
  /** 商品类型：1权益/2场景/3课程/4旅居（创建后不可改） */
  goodsType?: number
  /** 分类编码 */
  categoryCode?: string
  /** 品牌名称 */
  brandName?: string
  /** 封面图 URL */
  coverImage?: string
  /** 图集（JSON 字符串或逗号分隔） */
  imageUrls?: string
  /** 视频 URL */
  videoUrl?: string
  /** 商品描述 */
  goodsDescription?: string
  /** 摘要 */
  summary?: string
  /** 原价 */
  originalPrice?: number
  /** 售价 */
  salePrice?: number
  /** 成本价 */
  costPrice?: number
  /** 价格单位 */
  priceUnit?: string
  /** 库存 */
  stock?: number
  /** 销量（统计字段，只读） */
  salesCount?: number
  /** 浏览量（统计字段，只读） */
  viewCount?: number
  /** 收藏量（统计字段，只读） */
  collectCount?: number
  /** 开售时间 */
  saleStartTime?: string
  /** 结束开售时间 */
  saleEndTime?: string
  /** 是否热门：0否/1是 */
  isHot?: number
  /** 是否新品：0否/1是 */
  isNew?: number
  /** 是否推荐：0否/1是 */
  isRecommend?: number
  /** 排序号 */
  sortOrder?: number
  /** 商品状态：0草稿/1待上架/2已上架/3已下架/4已售罄 */
  goodsStatus?: number
  /** 审核状态：0待审/1通过/2驳回 */
  auditStatus?: number
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 商品分页查询参数（后端 GoodsInfoQueryDTO）。
 */
export interface GoodsInfoQuery extends PageQuery {
  goodsCode?: string
  goodsName?: string
  goodsType?: number
  categoryCode?: string
  goodsStatus?: number
  auditStatus?: number
}

/**
 * 商品上下架参数（后端 GoodsInfoShelfDTO）。
 *
 * 字段为 goodsStatus（非 shelfStatus，与 DDL 字段名对齐）。
 *
 * 语义偏差（已知遗留）：shelf 接口只支持 0/1 二态切换（0下架/1上架），
 * 但 DDL 的 goodsStatus 是 5 态，1 在 DDL 是"待上架"而非"已上架"。
 * 后端无完整状态机（statemachine 目录为空），前端按 shelf 契约传 0/1，
 * 列表按 DDL 5 态展示。详见 api/goods.ts shelfGoods 注释。
 */
export interface GoodsInfoShelfDTO {
  goodsCode: string
  /** 目标状态：0下架 / 1上架（shelf 接口语义，非 DDL 5 态） */
  goodsStatus: number
}

// ============================================================================
// 子表公共枚举
// ============================================================================

/**
 * SKU 状态：0停售 / 1在售（4 个 SKU 子表共用）。
 *
 * status 默认 1（在售）。
 */
export enum SkuStatus {
  OFF_SHELF = 0,
  ON_SHELF = 1
}

/** SKU 状态选项（4 子表共用） */
export const SKU_STATUS_OPTIONS = [
  { label: '停售', value: SkuStatus.OFF_SHELF },
  { label: '在售', value: SkuStatus.ON_SHELF }
] as const

/**
 * 课程类型（仅课程 SKU 用，有 DDL 文档）：1线上课程/2线下课程/3直播课程。
 */
export enum CourseType {
  /** 线上课程 */
  ONLINE = 1,
  /** 线下课程 */
  OFFLINE = 2,
  /** 直播课程 */
  LIVE = 3
}

/** 课程类型选项 */
export const COURSE_TYPE_OPTIONS = [
  { label: '线上课程', value: CourseType.ONLINE },
  { label: '线下课程', value: CourseType.OFFLINE },
  { label: '直播课程', value: CourseType.LIVE }
] as const

// ============================================================================
// 子表 A - 权益规格 Equity（GoodsSkuEquity，skuCode 前缀 GE）
// ============================================================================

/**
 * 权益规格（后端 GoodsSkuEquity，表 goods_sku_equity）。
 *
 * 主键 id（自增 number），业务键 skuCode（服务端生成 GE 前缀，前端不传）。
 * 关联键 goodsCode（弱外键）。
 */
export interface GoodsSkuEquity {
  /** 自增 id（主键） */
  id?: number
  /** 商品编码（关联键） */
  goodsCode: string
  /** 权益规格编码（服务端生成） */
  skuCode?: string
  /** 规格名称 */
  skuName?: string
  // TODO: templateCode 无跨模块选择器文档，暂用 el-input 兜底
  /** 权益模板编码（create 必填） */
  templateCode: string
  // TODO: equityType 枚举无文档（DDL 只写"权益类型"），暂用 el-input-number 兜底
  /** 权益类型 */
  equityType?: number
  /** 权益值 */
  equityValue?: string
  /** SKU 价格 */
  skuPrice?: number
  /** 库存 */
  stock?: number
  /** 销量（统计字段，create 硬编码 0，只读） */
  salesCount?: number
  /** 规格描述 */
  specDescription?: string
  /** 排序号（默认 0） */
  sortOrder?: number
  /** 状态：0停售/1在售（默认 1） */
  status?: number
  createdAt?: string
}

/**
 * 权益规格分页查询参数（后端 GoodsSkuEquityQueryDTO）。
 */
export interface GoodsSkuEquityQuery extends PageQuery {
  /** 商品编码（详情页 tab 固定携带） */
  goodsCode?: string
  skuName?: string
  templateCode?: string
  status?: number
}

// ============================================================================
// 子表 B - 场景配置 Scene（GoodsScene，表 goods_scene）
// ============================================================================

/**
 * 场景配置（后端 GoodsScene，表 goods_scene）。
 *
 * 主键 id（自增 number），业务键 skuCode（服务端生成 GS 前缀）。
 * 关联键 goodsCode。
 *
 * 注意：parkCode DDL 是 NOT NULL 但 DTO 无 @NotBlank——前端表单把 parkCode 设为必填。
 */
export interface GoodsScene {
  /** 自增 id（主键） */
  id?: number
  /** 商品编码（关联键） */
  goodsCode: string
  /** 场景规格编码（服务端生成） */
  skuCode?: string
  /** 规格名称 */
  skuName?: string
  // TODO: sceneCode 无跨模块选择器文档，暂用 el-input 兜底
  /** 场景编码（create 必填） */
  sceneCode: string
  /** 园区编码（DDL NOT NULL，前端表单必填以避免 DB 报错） */
  parkCode: string
  /** SKU 价格 */
  skuPrice?: number
  /** 人数上限 */
  personLimit?: number
  /** 时长（小时） */
  durationHours?: number
  /** 排期说明 */
  scheduleDescription?: string
  /** 库存 */
  stock?: number
  /** 销量（统计字段，create 硬编码 0，只读） */
  salesCount?: number
  /** 排序号（默认 0） */
  sortOrder?: number
  /** 状态：0停售/1在售（默认 1） */
  status?: number
  createdAt?: string
}

/**
 * 场景配置分页查询参数（后端 GoodsSceneQueryDTO）。
 */
export interface GoodsSceneQuery extends PageQuery {
  /** 商品编码（详情页 tab 固定携带） */
  goodsCode?: string
  skuName?: string
  sceneCode?: string
  status?: number
}

// ============================================================================
// 子表 C - 课程配置 Course（GoodsCourse，表 goods_course）
// ============================================================================

/**
 * 课程配置（后端 GoodsCourse，表 goods_course）。
 *
 * 主键 id（自增 number），业务键 skuCode（服务端生成 GC 前缀）。
 *
 * 语义陷阱：stock 字段复用承载"学员上限 maxStudents"语义（表里无 maxStudents 列），
 * 前端课程 tab 的 stock label 标"库存/学员上限"。
 */
export interface GoodsCourse {
  /** 自增 id（主键） */
  id?: number
  /** 商品编码（关联键） */
  goodsCode: string
  /** 课程规格编码（服务端生成） */
  skuCode?: string
  /** 规格名称 */
  skuName?: string
  // TODO: courseCode 无跨模块选择器文档，暂用 el-input 兜底
  /** 课程编码（create 必填） */
  courseCode: string
  /** 课程类型：1线上/2线下/3直播（DDL 文档） */
  courseType?: CourseType
  /** SKU 价格 */
  skuPrice?: number
  /** 课时数 */
  classCount?: number
  /** 有效天数 */
  validDays?: number
  /** 库存（语义复用：课程 tab 承载"学员上限"） */
  stock?: number
  /** 销量（统计字段，create 硬编码 0，只读） */
  salesCount?: number
  /** 排序号（默认 0） */
  sortOrder?: number
  /** 状态：0停售/1在售（默认 1） */
  status?: number
  createdAt?: string
}

/**
 * 课程配置分页查询参数（后端 GoodsCourseQueryDTO）。
 */
export interface GoodsCourseQuery extends PageQuery {
  /** 商品编码（详情页 tab 固定携带） */
  goodsCode?: string
  skuName?: string
  courseCode?: string
  courseType?: CourseType
  status?: number
}

// ============================================================================
// 子表 D - 旅居配置 Sojourn（GoodsSojourn，表 goods_sojourn）
// ============================================================================

/**
 * 旅居配置（后端 GoodsSojourn，表 goods_sojourn）。
 *
 * 主键 id（自增 number），业务键 skuCode（服务端生成 GJ 前缀）。
 *
 * 后端校验：minDays ≤ maxDays（maxDays 可空=不限）、effectiveDate ≤ expireDate（expireDate 可空=不限）。
 * priceUnit 旅居默认"元/月"。effectiveDate/expireDate 是 LocalDate（YYYY-MM-DD）。
 */
export interface GoodsSojourn {
  /** 自增 id（主键） */
  id?: number
  /** 商品编码（关联键） */
  goodsCode: string
  /** 旅居规格编码（服务端生成） */
  skuCode?: string
  /** 规格名称 */
  skuName?: string
  /** 园区编码（create 必填） */
  parkCode: string
  // TODO: roomTypeCode/careTypeCode/foodTypeCode 无跨模块选择器文档，暂用 el-input 兜底
  /** 房型编码（create 必填） */
  roomTypeCode: string
  /** 房型名称 */
  roomTypeName?: string
  /** 照护类型编码 */
  careTypeCode?: string
  /** 餐饮类型编码 */
  foodTypeCode?: string
  /** SKU 价格 */
  skuPrice?: number
  /** 价格单位（旅居默认"元/月"） */
  priceUnit?: string
  /** 最少入住天数（≤ maxDays，maxDays 可空） */
  minDays?: number
  /** 最多入住天数（可空=不限，≥ minDays） */
  maxDays?: number
  /** 库存 */
  stock?: number
  /** 销量（统计字段，create 硬编码 0，只读） */
  salesCount?: number
  /** 生效日期（LocalDate YYYY-MM-DD，≤ expireDate） */
  effectiveDate?: string
  /** 失效日期（LocalDate YYYY-MM-DD，可空=不限，≥ effectiveDate） */
  expireDate?: string
  /** 排序号（默认 0） */
  sortOrder?: number
  /** 状态：0停售/1在售（默认 1） */
  status?: number
  createdAt?: string
}

/**
 * 旅居配置分页查询参数（后端 GoodsSojournQueryDTO）。
 */
export interface GoodsSojournQuery extends PageQuery {
  /** 商品编码（详情页 tab 固定携带） */
  goodsCode?: string
  skuName?: string
  parkCode?: string
  roomTypeCode?: string
  status?: number
}

// ============================================================================
// 枚举 label 函数（供列表/详情页使用）
// ============================================================================

/** 商品类型标签文本 */
export function goodsTypeLabel(v?: number): string {
  const found = GOODS_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 商品状态标签文本（DDL 5 态） */
export function goodsStatusLabel(v?: number): string {
  const found = GOODS_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/**
 * 商品状态 el-tag 配色（DDL 5 态）。
 *
 * 已上架=success / 待上架=warning / 已下架=info / 已售罄=danger / 草稿=info。
 */
export function goodsStatusTagType(
  v?: number
): 'success' | 'info' | 'warning' | 'danger' {
  switch (v) {
    case GoodsStatus.ON_SHELF:
      return 'success'
    case GoodsStatus.PENDING:
      return 'warning'
    case GoodsStatus.SOLD_OUT:
      return 'danger'
    case GoodsStatus.DRAFT:
    case GoodsStatus.OFF_SHELF:
    default:
      return 'info'
  }
}

/** 课程类型标签文本 */
export function courseTypeLabel(v?: number): string {
  const found = COURSE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** SKU 状态标签文本（4 子表共用） */
export function skuStatusLabel(v?: number): string {
  const found = SKU_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** SKU 状态 el-tag 配色（4 子表共用） */
export function skuStatusTagType(v?: number): 'success' | 'info' {
  return v === SkuStatus.ON_SHELF ? 'success' : 'info'
}
