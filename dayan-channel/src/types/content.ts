import type { PageQuery } from './common'

/** 内容信息 */
export interface ContentInfo {
  id?: number
  contentCode: string
  title: string
  subtitle?: string
  contentType: number
  categoryCode?: string
  authorName?: string
  authorAvatar?: string
  coverImage?: string
  summary?: string
  contentBody?: string
  sourceType?: number
  sourceUrl?: string
  tags?: string
  isTop?: number
  isRecommend?: number
  contentStatus?: number
  auditStatus?: number
  createdAt?: string
  updatedAt?: string
}

/** 内容查询 */
export interface ContentInfoQuery extends PageQuery {
  contentCode?: string
  title?: string
  contentType?: number
  categoryCode?: string
  authorName?: string
  contentStatus?: number
  auditStatus?: number
}

/** 内容类型选项 */
export const CONTENT_TYPE_OPTIONS = [
  { value: 1, label: '文章' },
  { value: 2, label: '视频' },
  { value: 3, label: '图集' }
]

/** 内容配置项（channel_config_content）*/
export interface ChannelConfigContent {
  id?: number
  channelCode?: string
  contentCode: string
  contentType?: number
  appType: string
  position?: string
  sortOrder?: number
  isTop?: number
  effectiveTime?: string
  expireTime?: string
  status?: number
}

// ==================== 阅读记录（表 content_record_read） ====================

/** 阅读记录（表 content_record_read，对齐后端 ContentRecordReadVO） */
export interface ContentReadRecord {
  id?: number
  contentCode: string
  /** 读者类型：agent=代理人 / client=客户 / butler=管家 / guest=访客 */
  readerType?: string
  readerCode?: string
  /** 阅读来源（DB 现有注释权威）：1 自主浏览 / 2 分享链接 / 3 推荐 / 4 搜索 */
  readSource?: number
  readDuration?: number
  readTime?: string
}

/** 阅读来源选项（1 自主浏览 / 2 分享链接 / 3 推荐 / 4 搜索） */
export const READ_SOURCE_OPTIONS = [
  { value: 1, label: '自主浏览' },
  { value: 2, label: '分享链接' },
  { value: 3, label: '推荐' },
  { value: 4, label: '搜索' }
] as const

/** 读者类型选项（agent=代理人 / client=客户 / butler=管家 / guest=访客） */
export const READER_TYPE_OPTIONS = [
  { value: 'agent', label: '代理人' },
  { value: 'client', label: '客户' },
  { value: 'butler', label: '管家' },
  { value: 'guest', label: '访客' }
] as const

/** 阅读记录查询 */
export interface ContentReadRecordQuery extends PageQuery {
  contentCode?: string
  readerCode?: string
  readSource?: number
}
