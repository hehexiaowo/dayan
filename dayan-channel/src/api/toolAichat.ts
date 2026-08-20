/**
 * 问答人物 API（Channel 端渠道补充知识库）。
 *
 * 对齐后端 ChannelToolAichatController（/channel-api/tools/aichat）。
 * 本渠道由后端 ContextHolder 强制注入，前端不传 channelCode。
 */
import { request } from '@/utils/request'
import type { ToolChannelPersona, ToolChannelRepoOption } from '@/types/toolAichat'

/** 启用中问答人物列表（含 admin 全局库与本渠道补充库） */
export function getChannelAichatPersonas(): Promise<ToolChannelPersona[]> {
  return request<ToolChannelPersona[]>({ url: '/channel-api/tools/aichat/personas', method: 'get' })
}

/** 可补充知识库（本渠道 + 后代渠道名下，不含平台库） */
export function getChannelAichatRepoOptions(): Promise<ToolChannelRepoOption[]> {
  return request<ToolChannelRepoOption[]>({ url: '/channel-api/tools/aichat/repos/options', method: 'get' })
}

/** 保存人物补充知识库（全量替换；空数组 = 清空补充） */
export function saveChannelPersonaRepos(toolCode: string, repoIds: number[]): Promise<void> {
  return request<void>({ url: `/channel-api/tools/aichat/personas/${toolCode}/repos`, method: 'put', data: { repoIds } })
}
