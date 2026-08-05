/**
 * 商品相关类型。
 *
 * 字段对齐后端 com.dayan.goods.entity.GoodsInfo 及
 * GoodsInfoQueryDTO / GoodsInfoShelfDTO。
 */
import type { PageQuery } from '@/types/common'
import { AuditStatus, AUDIT_STATUS_OPTIONS } from '@/types/scene'

/**
 * 商品类型：1=实物 / 2=虚拟 / 3=服务。
 */
export enum GoodsType {
  /** 实物 */
  PHYSICAL = 1,
  /** 虚拟 */
  VIRTUAL = 2,
  /** 服务 */
  SERVICE = 3
}

/** 商品类型选项 */
export const GOODS_TYPE_OPTIONS = [
  { label: '实物', value: GoodsType.PHYSICAL },
  { label: '虚拟', value: GoodsType.VIRTUAL },
  { label: '服务', value: GoodsType.SERVICE }
] as const

/**
 * 商品状态：0=下架 / 1=上架 / 2=预览。
 */
export enum GoodsStatus {
  /** 下架 */
  OFF_SHELF = 0,
  /** 上架 */
  ON_SHELF = 1,
  /** 预览 */
  PREVIEW = 2
}

/** 商品状态选项 */
export const GOODS_STATUS_OPTIONS = [
  { label: '下架', value: GoodsStatus.OFF_SHELF },
  { label: '上架', value: GoodsStatus.ON_SHELF },
  { label: '预览', value: GoodsStatus.PREVIEW }
] as const

/**
 * 审核状态（复用 scene 的枚举）：0待审 / 1通过 / 2驳回。
 */
export { AuditStatus as GoodsAuditStatus }
export { AUDIT_STATUS_OPTIONS as GOODS_AUDIT_STATUS_OPTIONS }

/**
 * 商品信息实体（后端 GoodsInfo，表 goods_info）。
 *
 * 主键 goodsCode 由服务端 CodeGenerator 生成，新增表单不包含此字段。
 */
export interface GoodsInfo {
  id?: number
  /** 商品编码（服务端生成） */
  goodsCode?: string
  /** 商品名称 */
  goodsName: string
  /** 商品简称 */
  goodsShortName?: string
  /** 商品类型：1实物/2虚拟/3服务 */
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
  /** 销量 */
  salesCount?: number
  /** 浏览量 */
  viewCount?: number
  /** 收藏量 */
  collectCount?: number
  /** 是否热门：0否/1是 */
  isHot?: number
  /** 是否新品：0否/1是 */
  isNew?: number
  /** 是否推荐：0否/1是 */
  isRecommend?: number
  /** 排序号 */
  sortOrder?: number
  /** 商品状态：0下架/1上架/2预览 */
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
 *
 * 后端字段：current / size / goodsCode / goodsName / goodsType / categoryCode /
 * goodsStatus / auditStatus。
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
 */
export interface GoodsInfoShelfDTO {
  goodsCode: string
  /** 目标上下架状态：0下架 / 1上架 */
  shelfStatus: number
}
