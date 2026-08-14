import { request } from '@/utils/request'
import type { ChannelInfo, ChannelInfoQuery } from '@/types/channel'

/**
 * 渠道接口封装。
 *
 * 对应后端 ChannelInfoController（/admin-api/channels/*），RESTful 复数风格
 * （唯一不带 /info 后缀的模块）。
 *
 * 注意：
 * - GET / 返回 List 非 PageResult（无分页字段）；
 * - GET /tree 直接返回树形结构 List<ChannelInfoVO>；
 * - 主键 channelCode 由服务端生成。
 */

/**
 * 渠道列表（平铺，无分页）：GET /admin-api/channels
 *
 * 返回 List 非 PageResult。前端可调用 buildChannelTree 组树，或直接用 /tree 接口。
 */
export function listChannels(params?: ChannelInfoQuery): Promise<ChannelInfo[]> {
  return request<ChannelInfo[]>({
    url: '/admin-api/channels',
    method: 'get',
    params
  })
}

/** 渠道树形结构（后端组树）：GET /admin-api/channels/tree */
export function treeChannels(params?: ChannelInfoQuery): Promise<ChannelInfo[]> {
  return request<ChannelInfo[]>({
    url: '/admin-api/channels/tree',
    method: 'get',
    params
  })
}

/** 渠道详情：GET /admin-api/channels/{channelCode} */
export function getChannel(channelCode: string): Promise<ChannelInfo> {
  return request<ChannelInfo>({
    url: `/admin-api/channels/${channelCode}`,
    method: 'get'
  })
}

/** 新增渠道：POST /admin-api/channels */
export function createChannel(data: Partial<ChannelInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/channels',
    method: 'post',
    data
  })
}

/** 修改渠道：PUT /admin-api/channels/{channelCode} */
export function updateChannel(channelCode: string, data: Partial<ChannelInfo>): Promise<void> {
  return request<void>({
    url: `/admin-api/channels/${channelCode}`,
    method: 'put',
    data
  })
}

/** 删除渠道：DELETE /admin-api/channels/{channelCode} */
export function deleteChannel(channelCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/channels/${channelCode}`,
    method: 'delete'
  })
}

/**
 * 审核渠道（待审→通过/驳回）：POST /admin-api/channels/audit
 * auditStatus: 1=通过 / 2=驳回。
 */
export function auditChannel(data: {
  channelCode: string
  auditStatus: number
  auditRemark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/channels/audit',
    method: 'post',
    data
  })
}
