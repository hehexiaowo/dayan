import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  CourseLecturer,
  CourseLecturerQuery,
  CourseRecordLearn,
  CourseRecordLearnQuery
} from '@/types/course'

/**
 * 课程子表接口封装（讲师 / 学习记录）。
 *
 * 对应后端：
 * - CourseLecturerAdminController（/admin-api/course/lecturer/*）
 * - CourseRecordLearnAdminController（/admin-api/course/record-learn/*）
 *
 * 注意：course 子表 update 用 path `/{id}`（与 content 子表的 query ?id 不同）。
 */

// ==================== 课程讲师 ====================

export function pageCourseLecturers(query: CourseLecturerQuery): Promise<PageResult<CourseLecturer>> {
  return request<PageResult<CourseLecturer>>({
    url: '/admin-api/course/lecturer/page',
    method: 'get',
    params: query
  })
}

/** 讲师全量列表（下拉用） */
export function listCourseLecturers(query?: Partial<CourseLecturerQuery>): Promise<CourseLecturer[]> {
  return request<CourseLecturer[]>({
    url: '/admin-api/course/lecturer/list',
    method: 'get',
    params: query
  })
}

export function getCourseLecturer(id: number): Promise<CourseLecturer> {
  return request<CourseLecturer>({ url: `/admin-api/course/lecturer/${id}`, method: 'get' })
}

export function createCourseLecturer(data: Partial<CourseLecturer>): Promise<string> {
  return request<string>({ url: '/admin-api/course/lecturer', method: 'post', data })
}

/** 修改讲师：PUT /course/lecturer/{id}（id 走 path） */
export function updateCourseLecturer(id: number, data: Partial<CourseLecturer>): Promise<void> {
  return request<void>({ url: `/admin-api/course/lecturer/${id}`, method: 'put', data })
}

export function deleteCourseLecturer(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/course/lecturer/${id}`, method: 'delete' })
}

// ==================== 学习记录 ====================

export function pageCourseRecordLearn(query: CourseRecordLearnQuery): Promise<PageResult<CourseRecordLearn>> {
  return request<PageResult<CourseRecordLearn>>({
    url: '/admin-api/course/record-learn/page',
    method: 'get',
    params: query
  })
}

export function getCourseRecordLearn(id: number): Promise<CourseRecordLearn> {
  return request<CourseRecordLearn>({ url: `/admin-api/course/record-learn/${id}`, method: 'get' })
}

export function createCourseRecordLearn(data: Partial<CourseRecordLearn>): Promise<number> {
  return request<number>({ url: '/admin-api/course/record-learn', method: 'post', data })
}

/** 修改学习记录：PUT /course/record-learn/{id}（id 走 path） */
export function updateCourseRecordLearn(id: number, data: Partial<CourseRecordLearn>): Promise<void> {
  return request<void>({ url: `/admin-api/course/record-learn/${id}`, method: 'put', data })
}

export function deleteCourseRecordLearn(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/course/record-learn/${id}`, method: 'delete' })
}
