/**
 * 内容素材相关类型。
 *
 * 字段对齐后端 com.dayan.content.vo.ContentInfoVO 及 DTO。
 */
import type { PageQuery } from '@/types/common'

/** 内容类型：1文章/2视频/3图片集/4专题/5问答 */
export enum ContentType {
  ARTICLE = 1,
  VIDEO = 2,
  IMAGE_GROUP = 3,
  TOPIC = 4,
  QA = 5
}

/** 内容类型选项 */
export const CONTENT_TYPE_OPTIONS = [
  { label: '文章', value: ContentType.ARTICLE },
  { label: '视频', value: ContentType.VIDEO },
  { label: '图片集', value: ContentType.IMAGE_GROUP },
  { label: '专题', value: ContentType.TOPIC },
  { label: '问答', value: ContentType.QA }
] as const

/** 内容状态：0草稿/1待审/2通过/3拒绝/4下线 */
export enum ContentStatus {
  DRAFT = 0,
  PENDING = 1,
  PASS = 2,
  REJECT = 3,
  OFFLINE = 4
}

/** 内容状态选项 */
export const CONTENT_STATUS_OPTIONS = [
  { label: '草稿', value: ContentStatus.DRAFT },
  { label: '待审核', value: ContentStatus.PENDING },
  { label: '审核通过', value: ContentStatus.PASS },
  { label: '审核驳回', value: ContentStatus.REJECT },
  { label: '已下线', value: ContentStatus.OFFLINE }
] as const

/** 审核状态选项（auditStatus 字段） */
export const AUDIT_STATUS_OPTIONS = [
  { label: '待审核', value: 0 },
  { label: '审核通过', value: 1 },
  { label: '审核驳回', value: 2 }
] as const

/** 来源类型选项（sourceType 字段） */
export const SOURCE_TYPE_OPTIONS = [
  { label: '原创', value: 1 },
  { label: '转载', value: 2 },
  { label: '采编', value: 3 }
] as const

/**
 * 内容信息实体（后端 ContentInfoVO）。
 */
export interface ContentInfo {
  id?: number
  /** 内容编码（CT 前缀，系统生成） */
  contentCode?: string
  title: string
  subtitle?: string
  contentType: ContentType
  categoryCode?: string
  authorName?: string
  authorAvatar?: string
  coverImage?: string
  summary?: string
  contentBody?: string
  /** 来源类型：1原创/2转载/3采编 */
  sourceType?: number
  sourceUrl?: string
  tags?: string
  /** 是否置顶：1是 0否 */
  isTop?: number
  /** 是否推荐：1是 0否 */
  isRecommend?: number
  /** 是否允许评论：1是 0否 */
  isComment?: number
  viewCount?: number
  likeCount?: number
  commentCount?: number
  shareCount?: number
  collectCount?: number
  /** 发布时间（后端发布动作置位） */
  publishTime?: string
  sortOrder?: number
  /** 内容状态：0草稿/1待审/2通过/3拒绝/4下线 */
  contentStatus?: ContentStatus
  /** 审核状态：0待审/1通过/2驳回 */
  auditStatus?: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 内容分页查询参数（后端 ContentInfoQueryDTO）。
 */
export interface ContentInfoQuery extends PageQuery {
  contentCode?: string
  title?: string
  contentType?: ContentType
  categoryCode?: string
  authorName?: string
  contentStatus?: ContentStatus
  auditStatus?: number
  isTop?: number
  isRecommend?: number
}
