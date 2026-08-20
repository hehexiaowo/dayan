/** 渠道课程配置相关类型 */

/** 课程信息（对齐后端 CourseAgentVO 子集） */
export interface CourseOption {
  courseCode: string
  courseName: string
  courseType?: number
  courseSource?: number
  coverImage?: string
  courseDescription?: string
  lecturerName?: string
}
