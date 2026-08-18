import request from '@/utils/request'
import type { AiRefTemplateOption } from '@/types/aiContent'
import type { PageQuery, PageResult } from '@/types'
import type { AiMaterialBlock, AiMaterialRefs, AiProject, AiProjectListItem } from '@/types/toolAiCreator'

export interface AiProjectCreatePayload {
  purpose: string
  contentType: number
  styleCode: string
  audience?: string
  topic?: string
  /** 素材引用（含展示名，保存成品与回显用） */
  materialRefs?: AiMaterialRefs
  /** 素材快照（前端聚合块，digest 阶段一次性消费） */
  materials?: AiMaterialBlock[]
}

/** 创建创作项目 */
export function createAiProject(payload: AiProjectCreatePayload): Promise<number> {
  return request<number>({ url: '/tools/ai-creator', method: 'POST', data: payload })
}

/** 我的创作列表 */
export function getAiProjects(query?: PageQuery & { status?: string }): Promise<PageResult<AiProjectListItem>> {
  return request<PageResult<AiProjectListItem>>({ url: '/tools/ai-creator/list', method: 'GET', data: (query || {}) as Record<string, unknown> })
}

/** 项目详情（恢复草稿） */
export function getAiProject(id: number): Promise<AiProject> {
  return request<AiProject>({ url: `/tools/ai-creator/${id}`, method: 'GET' })
}

export function deleteAiProject(id: number): Promise<void> {
  return request<void>({ url: `/tools/ai-creator/${id}`, method: 'DELETE' })
}

/** 生成策略+5标题 */
export function genAiStrategy(id: number): Promise<AiProject> {
  return request<AiProject>({ url: `/tools/ai-creator/${id}/strategy`, method: 'POST' })
}

/** 带反馈重出标题（策略锁定） */
export function regenAiTitles(id: number, feedback?: string): Promise<AiProject> {
  return request<AiProject>({ url: `/tools/ai-creator/${id}/titles/regenerate`, method: 'POST', data: { feedback } })
}

export function confirmAiStrategy(id: number, payload: { selectedTitle: string; targetAudience?: string; corePainPoint?: string; viralLogic?: string; advantageHook?: string }): Promise<AiProject> {
  return request<AiProject>({ url: `/tools/ai-creator/${id}/strategy/confirm`, method: 'POST', data: payload })
}

export function genAiOutline(id: number): Promise<AiProject> {
  return request<AiProject>({ url: `/tools/ai-creator/${id}/outline`, method: 'POST' })
}

export function regenAiOutline(id: number, feedback?: string): Promise<AiProject> {
  return request<AiProject>({ url: `/tools/ai-creator/${id}/outline/regenerate`, method: 'POST', data: { feedback } })
}

export function confirmAiOutline(id: number, outline: string): Promise<AiProject> {
  return request<AiProject>({ url: `/tools/ai-creator/${id}/outline/confirm`, method: 'POST', data: { outline } })
}

/** 非流式生成正文（小程序降级） */
export function genAiBody(id: number): Promise<AiProject> {
  return request<AiProject>({ url: `/tools/ai-creator/${id}/body`, method: 'POST' })
}

/** 段落勘误（最小化修订） */
export function reviseAiBody(id: number, feedback: string, anchor?: string): Promise<AiProject> {
  return request<AiProject>({ url: `/tools/ai-creator/${id}/revise`, method: 'POST', data: { feedback, anchor } })
}

/** 图文 HTML 成品预览 */
export function getAiPreview(id: number): Promise<string> {
  return request<string>({ url: `/tools/ai-creator/${id}/preview`, method: 'GET' })
}

/** 保存到内容中心，返回 agent_content id */
export function saveAiProject(id: number): Promise<number> {
  return request<number>({ url: `/tools/ai-creator/${id}/save`, method: 'POST' })
}

/** 内置范文模板（平台风格参考） */
export function getAiTemplates(): Promise<AiRefTemplateOption[]> {
  return request<AiRefTemplateOption[]>({ url: '/tools/ai-creator/templates', method: 'GET' })
}
