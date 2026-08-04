import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { CourseInfo, CourseInfoQuery } from '@/types/course'

/**
 * 课程信息接口封装。
 *
 * 对应后端 CourseInfoAdminController（/admin-api/course/info/*）。
 *
 * 注意：
 * - update / publish / offline 均为 PUT 方法；
 * - courseCode 作为 PathVariable 走 path，不放入 body；
 * - publish / offline 为纯状态切换接口（PUT /{courseCode}/publish、/{courseCode}/offline）。
 */

/** 课程分页：GET /admin-api/course/info/page */
export function pageCourses(query: CourseInfoQuery): Promise<PageResult<CourseInfo>> {
  return request<PageResult<CourseInfo>>({
    url: '/admin-api/course/info/page',
    method: 'get',
    params: query
  })
}

/** 课程全量列表：GET /admin-api/course/info/list（供下拉） */
export function listCourses(): Promise<CourseInfo[]> {
  return request<CourseInfo[]>({
    url: '/admin-api/course/info/list',
    method: 'get'
  })
}

/** 课程详情：GET /admin-api/course/info/{courseCode} */
export function getCourse(courseCode: string): Promise<CourseInfo> {
  return request<CourseInfo>({
    url: `/admin-api/course/info/${courseCode}`,
    method: 'get'
  })
}

/** 新增课程：POST /admin-api/course/info */
export function createCourse(data: Partial<CourseInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/course/info',
    method: 'post',
    data
  })
}

/** 修改课程：PUT /admin-api/course/info/{courseCode}（courseCode 走 path） */
export function updateCourse(courseCode: string, data: Partial<CourseInfo>): Promise<void> {
  return request<void>({
    url: `/admin-api/course/info/${courseCode}`,
    method: 'put',
    data
  })
}

/** 删除课程：DELETE /admin-api/course/info/{courseCode} */
export function deleteCourse(courseCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/course/info/${courseCode}`,
    method: 'delete'
  })
}

/** 课程上架：PUT /admin-api/course/info/{courseCode}/publish */
export function publishCourse(courseCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/course/info/${courseCode}/publish`,
    method: 'put'
  })
}

/** 课程下架：PUT /admin-api/course/info/{courseCode}/offline */
export function offlineCourse(courseCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/course/info/${courseCode}/offline`,
    method: 'put'
  })
}
