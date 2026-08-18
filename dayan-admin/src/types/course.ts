/**
 * 课程相关类型。
 *
 * 字段对齐后端 com.dayan.course.vo.CourseInfoVO 及相关 DTO。
 */
import type { PageQuery } from '@/types/common'

/** 课程类型：1=线上录播 2=线上直播 3=线下课程 4=混合课程 */
export enum CourseType {
  ONLINE_RECORDED = 1,
  ONLINE_LIVE = 2,
  OFFLINE = 3,
  MIXED = 4
}

/** 课程类型选项 */
export const COURSE_TYPE_OPTIONS = [
  { label: '线上录播', value: CourseType.ONLINE_RECORDED },
  { label: '线上直播', value: CourseType.ONLINE_LIVE },
  { label: '线下课程', value: CourseType.OFFLINE },
  { label: '混合课程', value: CourseType.MIXED }
] as const

/** 课程状态（DDL 5 态）：0=草稿 1=待上架 2=已上架 3=已下架 4=已结课 */
export enum CourseStatus {
  DRAFT = 0,
  PENDING = 1,
  ONLINE = 2,
  OFFLINE = 3,
  FINISHED = 4,
}

/** 课程状态选项（DDL 5 态） */
export const COURSE_STATUS_OPTIONS = [
  { label: '草稿', value: CourseStatus.DRAFT },
  { label: '待上架', value: CourseStatus.PENDING },
  { label: '已上架', value: CourseStatus.ONLINE },
  { label: '已下架', value: CourseStatus.OFFLINE },
  { label: '已结课', value: CourseStatus.FINISHED }
] as const

/** 板块来源：1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯 */
export enum CourseSource {
  SELF = 1,
  CHANNEL = 2,
  EXTERNAL = 3,
  YANMING = 4
}

/** 板块来源选项（学习中心四板块） */
export const COURSE_SOURCE_OPTIONS = [
  { label: '大雁课程', value: CourseSource.SELF },
  { label: '渠道课程', value: CourseSource.CHANNEL },
  { label: '外部课程', value: CourseSource.EXTERNAL },
  { label: '雁鸣中国', value: CourseSource.YANMING }
] as const

/** 状态 el-tag type 映射 */
export function courseStatusTagType(status?: number): 'info' | 'warning' | 'success' | 'danger' {
  switch (status) {
    case CourseStatus.ONLINE: return 'success'
    case CourseStatus.PENDING: return 'warning'
    case CourseStatus.FINISHED: return 'danger'
    default: return 'info'
  }
}

/**
 * 课程信息实体（后端 CourseInfoVO）。
 *
 * 价格字段后端用 BigDecimal，前端按 number 处理；时间字段后端 LocalDate/LocalDateTime，
 * 前端按 string（ISO 日期）处理。
 */
export interface CourseInfo {
  id?: number
  /** 课程编码（CR 前缀，系统生成） */
  courseCode?: string
  /** 课程名称 */
  courseName: string
  /** 课程类型（非平台自研板块可为空） */
  courseType?: CourseType
  /** 板块来源：1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯 */
  courseSource?: CourseSource
  /** 分类编码 */
  categoryCode?: string
  /** 封面图地址 */
  coverImage?: string
  /** 视频地址（线上录播） */
  videoUrl?: string
  /** 课程简介 */
  courseDescription?: string
  /** 正文（详情页长文，纯文本） */
  courseBody?: string
  /** 课程大纲 */
  courseOutline?: string
  /** 目标人群 */
  targetAudience?: string
  /** 学习目标 */
  learningObjectives?: string
  /** 作者/来源（渠道/外部/资讯用，平台课程走讲师） */
  author?: string
  /** 时长展示文本（如 28:30 / 约 15 分钟） */
  durationText?: string
  /** 讲师编码 */
  lecturerCode?: string
  /** 总课时数 */
  totalClass?: number
  /** 总时长（分钟） */
  totalDuration?: number
  /** 有效天数 */
  validDays?: number
  /** 原价 */
  originalPrice?: number
  /** 售价 */
  salePrice?: number
  /** 最大学员数（线下/直播课，为空表示不限） */
  maxStudents?: number
  /** 当前学员数 */
  currentStudents?: number
  /** 浏览数 */
  viewCount?: number
  /** 销量 */
  salesCount?: number
  /** 平均评分 */
  ratingAvg?: number
  /** 是否免费：0否 1是 */
  isFree?: number
  /** 是否推荐：0否 1是 */
  isRecommend?: number
  /** 课程开始日期（ISO 字符串） */
  courseStartDate?: string
  /** 课程结束日期（ISO 字符串） */
  courseEndDate?: string
  /** 角标（热/新/要闻/人物/动态/洞察） */
  badge?: string
  /** 发布时间（资讯/内容用，课程走开课日期） */
  publishTime?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：0=草稿 1=待上架 2=已上架 3=已下架 4=已结课 */
  courseStatus?: CourseStatus
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createdAt?: string
  /** 更新时间 */
  updatedAt?: string
}

/** 课程分页查询参数（对齐 CourseInfoQueryDTO） */
export interface CourseInfoQuery extends PageQuery {
  /** 课程编码（模糊） */
  courseCode?: string
  /** 课程名称（模糊） */
  courseName?: string
  /** 课程类型 */
  courseType?: CourseType
  /** 板块来源 */
  courseSource?: CourseSource
  /** 分类编码 */
  categoryCode?: string
  /** 讲师编码 */
  lecturerCode?: string
  /** 上下架状态 */
  courseStatus?: CourseStatus
  /** 是否推荐 */
  isRecommend?: number
}

// ==================== 课程讲师 ====================

/**
 * 课程讲师实体（后端 CourseLecturerVO）。独立资源，被 CourseInfo.lecturerCode 引用。
 */
export interface CourseLecturer {
  id?: number
  /** 讲师编码（LT 前缀，系统生成） */
  lecturerCode?: string
  lecturerName: string
  /** 性别：1男 2女 0未知 */
  gender?: number
  avatar?: string
  /** 职称/头衔 */
  title?: string
  organization?: string
  specialty?: string
  introduction?: string
  certifications?: string
  phone?: string
  email?: string
  courseCount?: number
  studentCount?: number
  ratingAvg?: number
  /** 是否平台认证：0否 1是 */
  isCertified?: number
  sortOrder?: number
  /** 状态：0禁用 1启用 */
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface CourseLecturerQuery extends PageQuery {
  lecturerCode?: string
  lecturerName?: string
  organization?: string
  isCertified?: number
  status?: number
}

// ==================== 学习记录 ====================

/** 学习记录状态：0已退课 1学习中 2已完成 3已过期 */
export enum LearnStatus {
  REFUNDED = 0,
  LEARNING = 1,
  COMPLETED = 2,
  EXPIRED = 3
}

export const LEARN_STATUS_OPTIONS = [
  { label: '已退课', value: LearnStatus.REFUNDED },
  { label: '学习中', value: LearnStatus.LEARNING },
  { label: '已完成', value: LearnStatus.COMPLETED },
  { label: '已过期', value: LearnStatus.EXPIRED }
] as const

/**
 * 学习记录实体（后端 CourseRecordLearnVO，雪花 id）。按 courseCode 关联。
 */
export interface CourseRecordLearn {
  id?: number
  courseCode: string
  clientCode?: string
  agentCode?: string
  learnerName: string
  learnerPhone?: string
  enrollTime?: string
  orderCode?: string
  currentLesson?: number
  totalLesson: number
  /** 学习进度（%） */
  learnProgress?: number
  /** 累计学习时长（分钟） */
  totalLearnTime?: number
  lastLearnTime?: string
  /** 是否完成：0否 1是 */
  isCompleted?: number
  completeTime?: string
  certificateUrl?: string
  /** 课程评分 1-5 */
  rating?: number
  ratingContent?: string
  status?: LearnStatus
  createdAt?: string
  updatedAt?: string
}

export interface CourseRecordLearnQuery extends PageQuery {
  courseCode?: string
  clientCode?: string
  agentCode?: string
  learnerName?: string
  learnerPhone?: string
  isCompleted?: number
  status?: LearnStatus
}
