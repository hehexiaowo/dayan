/**
 * AI 问答人物配置类型（对齐后端 ToolAiQaConfigVO / ToolAiQaConfigQueryDTO）。
 */
import type { PageQuery } from '@/types/common'

/** AI 问答人物配置 */
export interface ToolAiQaConfig {
  id: number
  /** 配置编码（QAC+5位序列） */
  configCode: string
  /** 人物名称 */
  personaName: string
  /** 头像（文字或图标名） */
  icon?: string
  /** 图标色（blue/green/orange/red/gray） */
  iconColor?: string
  /** 人设描述（注入 system prompt） */
  systemPrompt: string
  /** 开场白/欢迎语 */
  welcomeMsg?: string
  /** 推荐问题数组 */
  recommendQuestions?: string[]
  /** 绑定知识库 ID 数组 */
  repoIds?: number[]
  /** 排序号 */
  sortOrder?: number
  /** 状态（0=禁用 1=启用） */
  status?: number
  /** 备注 */
  remark?: string
  createdAt?: string
}

/** AI 问答人物分页查询 */
export interface ToolAiQaConfigQuery extends PageQuery {
  /** 人物名称（模糊） */
  personaName?: string
  /** 状态（0=禁用 1=启用） */
  status?: number
}

/** 头像图标色选项（对齐后端 iconColor 枚举） */
export const QA_ICON_COLOR_OPTIONS = [
  { value: 'blue', label: '蓝色' },
  { value: 'green', label: '绿色' },
  { value: 'orange', label: '橙色' },
  { value: 'red', label: '红色' },
  { value: 'gray', label: '灰色' }
] as const

/** 状态标签：1=启用 0=禁用 */
export function qaStatusLabel(v?: number): string {
  return v === 1 ? '启用' : v === 0 ? '禁用' : '--'
}

export function qaStatusTagType(v?: number): 'success' | 'info' | 'warning' {
  return v === 1 ? 'success' : v === 0 ? 'info' : 'warning'
}

/** 图标色 → 对应 Element 标签/文字类型 */
export function iconColorTagType(v?: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  switch (v) {
    case 'blue':
      return 'primary'
    case 'green':
      return 'success'
    case 'orange':
      return 'warning'
    case 'red':
      return 'danger'
    default:
      return 'info'
  }
}
