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

/** 阅读记录 */
export interface ContentReadRecord {
  id?: number
  contentCode: string
  readerCode?: string
  readerName?: string
  readSource?: number
  readDuration?: number
  readTime?: string
}

/** 阅读记录查询 */
export interface ContentReadRecordQuery extends PageQuery {
  contentCode?: string
  readerCode?: string
  readSource?: number
}
