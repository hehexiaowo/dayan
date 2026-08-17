/**
 * 客户线索 API（渠道端视角）。
 *
 * 后端：com.dayan.lead.controller.channel.ChannelLeadController
 * 路径：/channel-api/leads（渠道隔离由后端上下文强制）。
 */
import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { LeadInfo, LeadInfoQuery, LeadTrace } from '@/types/lead'

/** 线索分页列表 */
export function pageLeads(query: LeadInfoQuery): Promise<PageResult<LeadInfo>> {
  return request<PageResult<LeadInfo>>({
    url: '/channel-api/leads',
    method: 'get',
    params: query
  })
}

/** 线索互动时间线（内容/工具/海报合并视图） */
export function getLeadTraces(leadCode: string): Promise<LeadTrace[]> {
  return request<LeadTrace[]>({
    url: `/channel-api/leads/${leadCode}/traces`,
    method: 'get'
  })
}
