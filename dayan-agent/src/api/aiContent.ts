import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types'
import type { AiContent, AiGeneratePayload, AiGenerateResult } from '@/types/aiContent'

/** AI 生成内容（POST /agent-api/ai/generate，不落库） */
export function generateAiContent(payload: AiGeneratePayload): Promise<AiGenerateResult> {
  return request<AiGenerateResult>({ url: '/ai/generate', method: 'POST', data: payload })
}

/** 保存生成内容（POST /agent-api/ai/contents） */
export function saveAiContent(data: Partial<AiContent>): Promise<number> {
  return request<number>({ url: '/ai/contents', method: 'POST', data })
}

/** 我的内容分页（GET /agent-api/ai/contents） */
export function getMyContents(query?: PageQuery & { contentType?: number; keyword?: string }): Promise<PageResult<AiContent>> {
  return request<PageResult<AiContent>>({
    url: '/ai/contents',
    method: 'GET',
    data: (query || {}) as Record<string, unknown>
  })
}

/** 我的内容详情 */
export function getMyContentDetail(id: number): Promise<AiContent> {
  return request<AiContent>({ url: `/ai/contents/${id}`, method: 'GET' })
}

/** 编辑我的内容 */
export function updateAiContent(id: number, data: Partial<AiContent>): Promise<void> {
  return request<void>({ url: `/ai/contents/${id}`, method: 'PUT', data })
}

/** 删除我的内容 */
export function deleteAiContent(id: number): Promise<void> {
  return request<void>({ url: `/ai/contents/${id}`, method: 'DELETE' })
}
