/** 学习中心板块内容（learning_content，Admin 端） */

/** 板块分类：1=渠道课程 2=外部课程 3=雁鸣中国（大雁课程=course_info，走课程管理首 tab） */
export const LEARNING_CATEGORY_OPTIONS = [
  { label: '渠道课程', value: 1 },
  { label: '外部课程', value: 2 },
  { label: '雁鸣中国', value: 3 }
] as const

export interface LearningContentItem {
  /** 雪花 id，后端序列化为字符串防 JS 精度丢失 */
  id?: string
  contentCode?: string
  title: string
  summary?: string
  category: number
  author?: string
  duration?: string
  body?: string
  viewCount?: number
  badge?: string
  publishTime?: string
  sortOrder?: number
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface LearningContentQuery {
  current: number
  size: number
  title?: string
  category?: number
  status?: number
}
