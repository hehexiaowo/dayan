import { request } from '@/utils/request'
import type { PageResult, PageQuery } from '@/types/common'

/**
 * 商品接口封装。
 *
 * 对应后端 Channel 端 ChannelGoodsController（/channel-api/goods-infos）。
 *
 * 注意：后端 list 端点返回 R<List<GoodsInfoVO>>（非分页，已按渠道白名单 + 上架状态过滤），
 * 与 PageResult 结构不一致。此处 `pageGoodsInfos` 做适配——把后端 List 包装为 PageResult，
 * 供 useCrud/usePagination 消费。包装在前端完成，避免改动后端契约。
 */

/** 商品实体（对齐后端 GoodsInfoVO，渠道商城展示子集）。 */
export interface GoodsInfo {
  id?: number
  /** 商品编码（主键业务码） */
  goodsCode?: string
  /** 商品名称 */
  goodsName?: string
  /** 商品简称 */
  goodsShortName?: string
  /** 商品类型：1=权益 / 2=场景 / 3=课程 / 4=旅居 */
  goodsType?: number
  /** 分类编码 */
  categoryCode?: string
  /** 品牌名称 */
  brandName?: string
  /** 封面图 URL */
  coverImage?: string
  /** 图片 URL 列表（后端为 JSON 字符串或逗号分隔，前端按需解析） */
  imageUrls?: string
  /** 商品详情（富文本） */
  goodsDescription?: string
  /** 商品摘要 */
  summary?: string
  /** 原价（单位：元，后端 BigDecimal，前端直接显示不要除以 100） */
  originalPrice?: number
  /** 售价（单位：元） */
  salePrice?: number
  /** 成本价（单位：元；渠道端一般不可见，预留字段） */
  costPrice?: number
  /** 价格单位（如「元/张」「元/年」） */
  priceUnit?: string
  /** 库存 */
  stock?: number
  /** 销量 */
  salesCount?: number
  /** 浏览量 */
  viewCount?: number
  /** 收藏量 */
  collectCount?: number
  /** 上架状态：1=上架 */
  goodsStatus?: number
  /** 审核状态 */
  auditStatus?: number
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createdAt?: string
}

/** 商品分页查询参数（对齐 GoodsInfoQueryDTO）。 */
export interface GoodsInfoQuery extends PageQuery {
  /** 商品编码（模糊匹配，可选） */
  goodsCode?: string
  /** 商品名称（模糊匹配，可选） */
  goodsName?: string
  /** 商品类型：1/2/3/4（可选） */
  goodsType?: number
  /** 分类编码（可选） */
  categoryCode?: string
  /** 上架状态（可选；商城页默认查 1=上架） */
  goodsStatus?: number
  /** 审核状态（可选） */
  auditStatus?: number
}

/** 商品类型选项（搜索栏下拉 + 表格 tag 文案）。 */
export const GOODS_TYPE_OPTIONS = [
  { label: '权益', value: 1 },
  { label: '场景', value: 2 },
  { label: '课程', value: 3 },
  { label: '旅居', value: 4 }
] as const

/**
 * 商品分页：GET /channel-api/goods-infos。
 *
 * 后端实际返回 `R<List<GoodsInfoVO>>`（非分页），此处适配为 `PageResult<GoodsInfo>`。
 * 后端已按「渠道白名单 + goodsStatus=1 上架」过滤，无需前端再筛；但为对齐搜索条件，
 * 仍把 query（goodsName/goodsType 等）作为 params 传给后端做服务端筛选。
 */
export async function pageGoodsInfos(query: GoodsInfoQuery): Promise<PageResult<GoodsInfo>> {
  const list = await request<GoodsInfo[]>({
    url: '/channel-api/goods-infos',
    method: 'get',
    params: query
  })
  // 后端返回 List；前端包装为 PageResult（current/size 取 query 值，pages 始终为 1）
  const current = query.current ?? 1
  const size = query.size ?? 20
  return {
    current,
    size,
    total: list.length,
    pages: list.length === 0 ? 0 : Math.max(1, Math.ceil(list.length / size)),
    records: list
  }
}

/** 商品详情：GET /channel-api/goods-infos/{goodsCode} */
export function getGoodsInfo(goodsCode: string): Promise<GoodsInfo> {
  return request<GoodsInfo>({
    url: `/channel-api/goods-infos/${goodsCode}`,
    method: 'get'
  })
}
