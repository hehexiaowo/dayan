import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types'
import type { AiProject, AiProjectListItem } from '@/types/aiCreation'

export interface AiProjectCreatePayload {
  purpose: string
  contentType: number
  styleCode: string
  audience?: string
  topic?: string
  refContentCode?: string
  kbFileIds?: string[]
  goodsCodes?: string[]
  parkCodes?: string[]
}

/** 创建创作项目 */
export function createAiProject(payload: AiProjectCreatePayload): Promise<number> {
  return request<number>({ url: '/ai/projects', method: 'POST', data: payload })
}

/** 我的创作列表 */
export function getAiProjects(query?: PageQuery & { status?: string }): Promise<PageResult<AiProjectListItem>> {
  return request<PageResult<AiProjectListItem>>({ url: '/ai/projects/list', method: 'GET', data: (query || {}) as Record<string, unknown> })
}

/** 项目详情（恢复草稿） */
export function getAiProject(id: number): Promise<AiProject> {
  return request<AiProject>({ url: `/ai/projects/${id}`, method: 'GET' })
}

export function deleteAiProject(id: number): Promise<void> {
  return request<void>({ url: `/ai/projects/${id}`, method: 'DELETE' })
}

/** 生成策略+5标题 */
export function genAiStrategy(id: number): Promise<AiProject> {
  return request<AiProject>({ url: `/ai/projects/${id}/strategy`, method: 'POST' })
}

/** 带反馈重出标题（策略锁定） */
export function regenAiTitles(id: number, feedback?: string): Promise<AiProject> {
  return request<AiProject>({ url: `/ai/projects/${id}/titles/regenerate`, method: 'POST', data: { feedback } })
}

export function confirmAiStrategy(id: number, payload: { selectedTitle: string; targetAudience?: string; corePainPoint?: string; viralLogic?: string; advantageHook?: string }): Promise<AiProject> {
  return request<AiProject>({ url: `/ai/projects/${id}/strategy/confirm`, method: 'POST', data: payload })
}

export function genAiOutline(id: number): Promise<AiProject> {
  return request<AiProject>({ url: `/ai/projects/${id}/outline`, method: 'POST' })
}

export function regenAiOutline(id: number, feedback?: string): Promise<AiProject> {
  return request<AiProject>({ url: `/ai/projects/${id}/outline/regenerate`, method: 'POST', data: { feedback } })
}

export function confirmAiOutline(id: number, outline: string): Promise<AiProject> {
  return request<AiProject>({ url: `/ai/projects/${id}/outline/confirm`, method: 'POST', data: { outline } })
}

/** 非流式生成正文（小程序降级） */
export function genAiBody(id: number): Promise<AiProject> {
  return request<AiProject>({ url: `/ai/projects/${id}/body`, method: 'POST' })
}

/** 段落勘误（最小化修订） */
export function reviseAiBody(id: number, feedback: string, anchor?: string): Promise<AiProject> {
  return request<AiProject>({ url: `/ai/projects/${id}/revise`, method: 'POST', data: { feedback, anchor } })
}

/** 图文 HTML 成品预览 */
export function getAiPreview(id: number): Promise<string> {
  return request<string>({ url: `/ai/projects/${id}/preview`, method: 'GET' })
}

/** 保存到内容中心，返回 agent_content id */
export function saveAiProject(id: number): Promise<number> {
  return request<number>({ url: `/ai/projects/${id}/save`, method: 'POST' })
}
