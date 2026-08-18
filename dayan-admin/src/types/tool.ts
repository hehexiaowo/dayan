/**
 * 工具域相关类型（工具配置）。
 *
 * 字段对齐后端 com.dayan.tool.entity.ToolInfo / ToolInfoVO。
 * - Integer → number
 * - LocalDateTime → string
 * - configJson 为 JSON 字符串（原样透传，端上自行解析）
 */
import type { PageQuery } from '@/types/common'

/**
 * 工具实例类型（tool_info.tool_type）。
 * 四类固定：pension 社保养老计算器 / gap 养老缺口计算器 / aiartist AI 创作 / aichat 你问我答。
 */
export const TOOL_TYPE_OPTIONS = [
  { label: '社保养老计算器', value: 'pension' },
  { label: '养老缺口计算器', value: 'gap' },
  { label: 'AI 创作', value: 'aiartist' },
  { label: '你问我答', value: 'aichat' }
] as const

/** 工具类型标签文本。 */
export function toolTypeLabel(v?: string): string {
  const found = TOOL_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? v : '--'
}

/**
 * 工具实例实体（后端 ToolInfoVO）。
 *
 * 仅承载定义与配置；图标、颜色、页面路径、可见端、排序等展示细节
 * 由端上按 tool_type 固定映射，类型化配置走 configJson。
 */
export interface ToolInfo {
  id?: number
  /** 工具编码（服务端生成，TL 前缀） */
  toolCode?: string
  /** 工具名称（必填） */
  toolName: string
  /** 工具类型：pension/gap/aiartist/aichat */
  toolType?: string
  /** 工具简介 */
  toolDesc?: string
  /** 工具配置 JSON（按类型承载提示词/默认值等） */
  configJson?: string
  /** 状态：1启用 0禁用 */
  status?: number
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 工具分页查询参数。
 */
export interface ToolInfoQuery extends PageQuery {
  toolCode?: string
  toolName?: string
  toolType?: string
  status?: number
}
