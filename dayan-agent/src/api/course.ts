import request from '@/utils/request';
import type { Course } from '@/types';

/**
 * 大雁课程列表（GET /agent-api/courses，course_info 平台自研课程）。
 * @param courseType 1=线上录播 2=线上直播 3=线下课程 4=混合课程，不传 = 全部
 */
export function getCourses(courseType?: number): Promise<Course[]> {
  return request<Course[]>({
    url: '/courses',
    method: 'GET',
    data: courseType != null ? { courseType } : {},
  });
}

/**
 * 课程详情（GET /agent-api/courses/{courseCode}，同时累加浏览量）。
 */
export function getCourseDetail(courseCode: string): Promise<Course> {
  return request<Course>({
    url: `/courses/${courseCode}`,
    method: 'GET',
  });
}
