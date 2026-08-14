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

/** 课程上下架状态：0=下架 1=上架 */
export enum CourseStatus {
  OFFLINE = 0,
  ONLINE = 1
}

/** 课程上下架状态选项 */
export const COURSE_STATUS_OPTIONS = [
  { label: '下架', value: CourseStatus.OFFLINE },
  { label: '上架', value: CourseStatus.ONLINE }
] as const

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
  /** 课程类型 */
  courseType: CourseType
  /** 分类编码 */
  categoryCode?: string
  /** 封面图地址 */
  coverImage?: string
  /** 视频地址（线上录播） */
  videoUrl?: string
  /** 课程简介 */
  courseDescription?: string
  /** 课程大纲 */
  courseOutline?: string
  /** 目标人群 */
  targetAudience?: string
  /** 学习目标 */
  learningObjectives?: string
  /** 讲师编码 */
  lecturerCode?: string
  /** 总课时数 */
  totalClass?: number
  /** 总时长（分钟） */
  totalDuration?: number
  /** 有效天数 */
  validDays?: number
  /** 原价 */
  originalPrice: number
  /** 售价 */
  salePrice: number
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
  /** 排序号 */
  sortOrder?: number
  /** 上下架状态：0=下架 1=上架 */
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
