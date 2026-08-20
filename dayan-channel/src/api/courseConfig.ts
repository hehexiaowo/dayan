/**
 * 课程配置 API（Channel 端渠道课程可见性配置）。
 *
 * 对齐后端 ChannelCourseConfigController（/channel-api/courses/config）。
 * 本渠道由后端 ContextHolder 强制注入，前端不传 channelCode。
 */
import { request } from '@/utils/request'
import type { CourseOption } from '@/types/courseConfig'

/** 可配置课程列表（平台课程 + 渠道课程） */
export function getAvailableCourses(): Promise<CourseOption[]> {
  return request<CourseOption[]>({ url: '/channel-api/courses/config/available', method: 'get' })
}

/** 本渠道已配置的课程编码列表 */
export function getConfiguredCourseCodes(): Promise<string[]> {
  return request<string[]>({ url: '/channel-api/courses/config/configured', method: 'get' })
}

/** 保存课程可见性配置（全量替换） */
export function saveCourseVisibility(courseCodes: string[]): Promise<void> {
  return request<void>({ url: '/channel-api/courses/config/visibility', method: 'put', data: courseCodes })
}
