import request from '@/utils/request';
import type { Course, CourseRecordLearn, PageResult } from '@/types';

/**
 * 上架课程列表（GET /agent-api/courses，全量，兼容旧接口）。
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

/** 课程分页查询参数 */
export interface CoursePageParams {
  courseType?: number;
  courseSource?: number;
  keyword?: string;
  categoryCode?: string;
  current?: number;
  size?: number;
}

/**
 * 上架课程分页（GET /agent-api/courses/page，支持关键词/分类筛选）。
 */
export function pageCourses(params: CoursePageParams): Promise<PageResult<Course>> {
  return request<PageResult<Course>>({
    url: '/courses/page',
    method: 'GET',
    data: params,
  });
}

/**
 * 各板块上架课程计数（GET /agent-api/courses/count）。
 * 返回 { "1": 8, "2": 12, "3": 5, "4": 20 }  key=courseSource, value=count
 */
export function getCourseCounts(): Promise<Record<string, number>> {
  return request<Record<string, number>>({
    url: '/courses/count',
    method: 'GET',
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

// ====== 学习记录 ======

/** 进度上报请求体 */
export interface ProgressPayload {
  courseCode: string;
  /** 当前课时（可选） */
  currentLesson?: number;
  /** 本次学习时长增量（分钟） */
  learnTimeDelta?: number;
}

/**
 * 上报学习进度（POST /agent-api/course-records/progress，首次自动建档）。
 */
export function reportProgress(payload: ProgressPayload): Promise<CourseRecordLearn> {
  return request<CourseRecordLearn>({
    url: '/course-records/progress',
    method: 'POST',
    data: payload,
  });
}

/**
 * 我的学习记录列表（GET /agent-api/course-records/my）。
 */
export function getMyLearnRecords(): Promise<CourseRecordLearn[]> {
  return request<CourseRecordLearn[]>({
    url: '/course-records/my',
    method: 'GET',
  });
}

/**
 * 某课程的学习记录（GET /agent-api/course-records/my/{courseCode}）。
 * 未开始学习时返回 null。
 */
export function getMyCourseRecord(courseCode: string): Promise<CourseRecordLearn | null> {
  return request<CourseRecordLearn | null>({
    url: `/course-records/my/${courseCode}`,
    method: 'GET',
  });
}
