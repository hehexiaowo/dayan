import request from '@/utils/request';
import type { Course } from '@/types';

/**
 * 上架课程列表（GET /agent-api/courses，学习中心四板块统一 course_info）。
 * @param courseType 1=线上录播 2=线上直播 3=线下课程 4=混合课程，不传 = 全部
 * @param courseSource 1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯，不传 = 全部
 */
export function getCourses(courseType?: number, courseSource?: number): Promise<Course[]> {
  return request<Course[]>({
    url: '/courses',
    method: 'GET',
    data: {
      ...(courseType != null ? { courseType } : {}),
      ...(courseSource != null ? { courseSource } : {}),
    },
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
