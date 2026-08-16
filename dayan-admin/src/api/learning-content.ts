import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { LearningContentItem, LearningContentQuery } from '@/types/learning-content'

/**
 * 学习中心板块内容接口封装（渠道课程/外部课程/雁鸣中国）。
 *
 * 对应后端 LearningContentAdminController（/admin-api/learning-contents/*）。
 * 主键 id（雪花，后端序列化为字符串），update 用 path id；contentCode 系统生成。
 */
export function pageLearningContents(
  query: Partial<LearningContentQuery>
): Promise<PageResult<LearningContentItem>> {
  return request<PageResult<LearningContentItem>>({
    url: '/admin-api/learning-contents',
    method: 'get',
    params: query
  })
}

export function createLearningContent(data: Partial<LearningContentItem>): Promise<string> {
  return request<string>({ url: '/admin-api/learning-contents', method: 'post', data })
}

export function updateLearningContent(id: string, data: Partial<LearningContentItem>): Promise<void> {
  return request<void>({ url: `/admin-api/learning-contents/${id}`, method: 'put', data })
}

export function deleteLearningContent(id: string): Promise<void> {
  return request<void>({ url: `/admin-api/learning-contents/${id}`, method: 'delete' })
}
