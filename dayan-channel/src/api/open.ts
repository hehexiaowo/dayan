import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ChannelOpenPlatform } from '@/types/open'

/**
 * 开放平台接口封装。
 *
 * 对应后端 Channel 端 ChannelOpenPlatformController（/channel-api/open-platforms）。
 * 仅只读：查看本渠道对接配置（appSecret 脱敏）。
 */

/** 本渠道对接配置：GET /channel-api/open-platforms */
export function getOpenPlatform(): Promise<PageResult<ChannelOpenPlatform>> {
  return request<PageResult<ChannelOpenPlatform>>({
    url: '/channel-api/open-platforms',
    method: 'get',
    params: { current: 1, size: 1 }
  })
}

/** 对接配置详情：GET /channel-api/open-platforms/{id} */
export function getOpenPlatformDetail(id: number): Promise<ChannelOpenPlatform> {
  return request<ChannelOpenPlatform>({
    url: `/channel-api/open-platforms/${id}`,
    method: 'get'
  })
}
