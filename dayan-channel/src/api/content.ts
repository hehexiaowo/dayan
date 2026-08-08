import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ContentInfo, ContentInfoQuery, ContentReadRecord, ContentReadRecordQuery, ChannelConfigContent } from '@/types/content'

/**
 * 内容域接口（渠道端）。
 * 对应后端 /channel-api/contents、/channel-api/content-read-records、/channel-api/channel-configs。
 */

// ==================== 内容列表/详情（/channel-api/contents）====================

/** 已配置内容分页：GET /channel-api/contents?appType=agent|client */
export function pageContents(
  query: ContentInfoQuery & { appType?: string }
): Promise<PageResult<ContentInfo>> {
  return request<PageResult<ContentInfo>>({
    url: '/channel-api/contents',
    method: 'get',
    params: query
  })
}

/** 内容详情：GET /channel-api/contents/{contentCode} */
export function getContent(contentCode: string): Promise<ContentInfo> {
  return request<ContentInfo>({
    url: `/channel-api/contents/${contentCode}`,
    method: 'get'
  })
}

// ==================== 阅读记录（/channel-api/content-read-records）====================

/** 阅读记录分页：GET /channel-api/content-read-records */
export function pageReadRecords(
  query: ContentReadRecordQuery
): Promise<PageResult<ContentReadRecord>> {
  return request<PageResult<ContentReadRecord>>({
    url: '/channel-api/content-read-records',
    method: 'get',
    params: query
  })
}

// ==================== 内容配置自管（/channel-api/channel-configs）====================

/** 查内容配置：GET /channel-api/channel-configs/content?appType=agent|client */
export function listContentConfig(appType?: string): Promise<ChannelConfigContent[]> {
  return request<ChannelConfigContent[]>({
    url: '/channel-api/channel-configs/content',
    method: 'get',
    params: appType ? { appType } : {}
  })
}

/** 保存内容配置（全量覆盖）：PUT /channel-api/channel-configs/content */
export function saveContentConfig(configs: ChannelConfigContent[]): Promise<unknown> {
  return request({
    url: '/channel-api/channel-configs/content',
    method: 'put',
    data: configs
  })
}
