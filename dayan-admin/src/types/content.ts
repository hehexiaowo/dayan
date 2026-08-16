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

/** 审核动作选项（audit 入参 auditStatus：2=通过 / 3=拒绝，对齐后端 ContentInfoAuditDTO） */
export const AUDIT_STATUS_OPTIONS = [
  { label: '审核通过', value: 2 },
  { label: '审核驳回', value: 3 }
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
  /** 适用业态（逗号分隔 vital/care/sojourn），空=全部 */
  networkTags?: string
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
  /** 审核状态：0未审核/2通过/3驳回 */
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
  /** 适用业态过滤（逗号分隔，空=全部） */
  network?: string
}

// ==================== 内容多媒体 ====================

/** 媒体类型：1图片/2视频/3音频/4文件 */
export enum MediaType {
  IMAGE = 1,
  VIDEO = 2,
  AUDIO = 3,
  FILE = 4
}

export const MEDIA_TYPE_OPTIONS = [
  { label: '图片', value: MediaType.IMAGE },
  { label: '视频', value: MediaType.VIDEO },
  { label: '音频', value: MediaType.AUDIO },
  { label: '文件', value: MediaType.FILE }
] as const

/**
 * 内容多媒体实体（后端 ContentMediaVO）。按 contentCode 分组。
 */
export interface ContentMedia {
  id?: number
  contentCode: string
  mediaType: MediaType
  mediaUrl: string
  thumbnailUrl?: string
  mediaName?: string
  fileFormat?: string
  /** 文件大小（KB） */
  fileSize?: number
  width?: number
  height?: number
  /** 时长（秒） */
  duration?: number
  mediaDescription?: string
  /** 是否在正文中：1是 0否 */
  isInBody?: number
  sortOrder?: number
  createdAt?: string
  updatedAt?: string
}

export interface ContentMediaQuery extends PageQuery {
  contentCode?: string
  mediaType?: MediaType
}

// ==================== 阅读记录 + 统计 ====================

/**
 * 阅读记录实体（后端 ContentRecordReadVO，雪花 id）。前端上报，管理端只读+删除+统计。
 */
export interface ContentRecordRead {
  id?: number
  contentCode: string
  readerType?: string
  readerCode?: string
  /** 阅读时长（秒） */
  readDuration?: number
  /** 阅读进度（%） */
  readProgress?: number
  readSource?: number
  ipAddress?: string
  deviceType?: string
  readTime?: string
  createdAt?: string
}

export interface ContentRecordReadQuery extends PageQuery {
  contentCode?: string
  readerCode?: string
  readSource?: number
}

/** 阅读统计（后端 ContentReadStatsVO） */
export interface ContentReadStats {
  contentCode: string
  /** 阅读次数（记录总数） */
  pv: number
  /** 去重访客数（按 readerCode） */
  uv: number
}

// ==================== 分享记录 ====================

/** 分享渠道：1微信/2朋友圈/3QQ/4微博/5复制链接/99其它 */
export enum ShareChannel {
  WECHAT = 1,
  MOMENTS = 2,
  QQ = 3,
  WEIBO = 4,
  COPY_LINK = 5,
  OTHER = 99
}

export const SHARE_CHANNEL_OPTIONS = [
  { label: '微信', value: ShareChannel.WECHAT },
  { label: '朋友圈', value: ShareChannel.MOMENTS },
  { label: 'QQ', value: ShareChannel.QQ },
  { label: '微博', value: ShareChannel.WEIBO },
  { label: '复制链接', value: ShareChannel.COPY_LINK },
  { label: '其它', value: ShareChannel.OTHER }
] as const

/**
 * 分享记录实体（后端 ContentRecordShareVO，雪花 id）。
 */
export interface ContentRecordShare {
  id?: number
  contentCode: string
  sharerType?: string
  sharerCode?: string
  shareChannel?: ShareChannel
  shareUrl?: string
  shareTitle?: string
  shareDescription?: string
  shareImage?: string
  clickCount?: number
  convertCount?: number
  shareTime?: string
  createdAt?: string
}

export interface ContentRecordShareQuery extends PageQuery {
  contentCode?: string
  sharerCode?: string
  shareChannel?: ShareChannel
}

