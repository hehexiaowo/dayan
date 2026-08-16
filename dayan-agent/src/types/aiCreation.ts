/**
 * AI 创作六阶段流水线（agent 端）类型定义。
 */
import type { AiMaterialSource } from './aiContent'

export type AiPurpose = 'product' | 'park' | 'science'

export const AI_PURPOSE_OPTIONS = [
  { value: 'product', label: '产品宣传', desc: '推荐「保险产品+养老权益」解决方案，必选知识库保险资料与权益商品' },
  { value: 'park', label: '机构推荐', desc: '围绕养老机构写种草文，机构信息全部来自平台机构库' },
  { value: 'science', label: '科普获客', desc: '热点/趋势切入建立专业人设，不推销具体产品' }
] as const

export interface AiMaterialRefs {
  refContentCode?: string
  kbFileIds?: string[]
  goodsCodes?: string[]
  parkCodes?: string[]
}

export interface AiHardFact { fact: string; source: string }

export interface AiFactDigest {
  hardFacts: AiHardFact[]
  softPoints?: string[]
  missing?: string[]
}

export interface AiStrategy {
  targetAudience?: string
  corePainPoint?: string
  viralLogic?: string
  advantageHook?: string
  coreExecutionPrompt?: string
}

export interface AiTitle { title: string; tag?: string; viralScore?: number; reasoning?: string }

export interface AiImageSpec { source?: string; size?: string; prompt?: string; imagePromptZh?: string }

export interface AiOutlineNode {
  id?: string
  section: string
  corePoints?: string[]
  arguments?: string[]
  viralTags?: string[]
  imageInsertion?: AiImageSpec
}

export interface AiOutline { coverImage?: AiImageSpec; nodes: AiOutlineNode[] }

export interface AiScores {
  naturalness?: number
  viralDesign?: number
  styleSimilarity?: number
  emotionalImpact?: number
  conversionRate?: number
  editorCritique?: string
}

export interface AiProjectImage {
  placeholder: string
  size?: string
  prompt?: string
  promptZh?: string
  fileKey?: string
  url?: string
  status: 'pending' | 'generating' | 'done' | 'failed' | 'skipped'
  error?: string
}

export interface AiAuditItem { type?: string; message?: string }

export interface AiProject {
  id: number
  purpose: AiPurpose
  contentType: number
  styleCode?: string
  audience?: string
  topic?: string
  materialRefs?: AiMaterialRefs
  status: string
  factDigest?: AiFactDigest
  strategy?: AiStrategy
  titles?: AiTitle[]
  selectedTitle?: string
  outline?: AiOutline
  body?: string
  auditLog?: AiAuditItem[]
  scores?: AiScores
  images?: AiProjectImage[]
  warnings?: string[]
  refContentName?: string
  kbFileNames?: string[]
  goodsNames?: string[]
  parkNames?: string[]
  createdAt?: string
  updatedAt?: string
}

export interface AiProjectListItem {
  id: number
  purpose: AiPurpose
  contentType: number
  topic?: string
  selectedTitle?: string
  status: string
  updatedAt?: string
}

export const AI_PHASE_LABELS: Record<string, string> = {
  CREATED: '待生成策略',
  DIGESTED: '待确认策略',
  STRATEGY_CONFIRMED: '待生成大纲',
  OUTLINE_CONFIRMED: '待生成正文',
  BODY_DONE: '正文已完成',
  IMAGES_DONE: '配图已完成',
  SAVED: '已保存'
}

/** 按状态路由到对应步骤页 */
export function phaseStep(status?: string, contentType?: number): string {
  const base = '/pages/acquisition/ai-create/'
  switch (status) {
    case 'CREATED':
    case 'DIGESTED':
      return base + 'step-strategy'
    case 'STRATEGY_CONFIRMED':
      return contentType === 2 ? base + 'step-body' : base + 'step-outline'
    case 'OUTLINE_CONFIRMED':
      return base + 'step-body'
    case 'BODY_DONE':
    case 'IMAGES_DONE':
    case 'SAVED':
      return contentType === 2 ? base + 'step-body' : base + 'step-preview'
    default:
      return base + 'step-strategy'
  }
}

export function purposeLabel(p?: string): string {
  return AI_PURPOSE_OPTIONS.find((o) => o.value === p)?.label ?? '创作'
}

/** 标题 tag 中文 */
export const AI_TITLE_TAG_LABELS: Record<string, string> = {
  kb_number: '素材硬数据',
  doc_logic: '资料逻辑',
  emotion_hook: '情绪悬念'
}
