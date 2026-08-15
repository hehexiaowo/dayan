import type { PageQuery } from './common'

// ==================== 商品展示板块 ====================

/**
 * 商品展示板块类型选项（C/Agent 端详情页 tab 名）。
 *
 * 与 park_display_block 的机构展示板块区分：这里是商品维度（产品介绍/权益详解/服务流程…），
 * 由「页面配置」Tab 维护，C 端详情页按此渲染 tab 结构。
 */
export const GOODS_DISPLAY_BLOCK_TYPE_OPTIONS = [
  { label: '产品介绍', value: 'product_intro' },
  { label: '权益详解', value: 'rights_detail' },
  { label: '服务流程', value: 'service_flow' },
  { label: '常见问题', value: 'faq' },
  { label: '购买须知', value: 'purchase_terms' },
  { label: '自定义', value: 'custom' }
] as const

export const goodsDisplayBlockTypeLabel = (v?: string) =>
  GOODS_DISPLAY_BLOCK_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? (v || '--')

/** 商品展示板块实体（后端 goods_display_block） */
export interface GoodsDisplayBlock {
  id?: number
  /** 商品编码 */
  goodsCode?: string
  /** 板块类型（product_intro/rights_detail/service_flow/faq/purchase_terms/custom） */
  blockType: string
  /** 板块标题（C端 tab 名） */
  blockTitle?: string
  /** 富文本内容（HTML） */
  content?: string
  /** 图片key列表（JSON数组字符串，后端存 TEXT） */
  images?: string
  /** 图片描述列表（JSON数组字符串，与 images 一一对应） */
  imageDescriptions?: string
  sortOrder?: number
  /** 状态（0=隐藏, 1=显示） */
  status?: number
  createdAt?: string
  updatedAt?: string
}

/** 展示板块查询入参 */
export interface GoodsDisplayBlockQuery extends PageQuery {
  goodsCode?: string
  blockType?: string
  status?: number
}

/**
 * 展示板块提交载荷。
 *
 * 与响应 VO 同构（images/imageDescriptions 均为 JSON 数组字符串），
 * 前端在数组形态（imagesArr/descsArr）上编辑、提交前 JSON.stringify。
 */
export type GoodsDisplayBlockPayload = Partial<Omit<GoodsDisplayBlock, 'id' | 'goodsCode' | 'createdAt' | 'updatedAt'>>
