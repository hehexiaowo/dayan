import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { Equity, EquityQuery } from '@/types/equity'

/**
 * 权益接口封装。
 *
 * 对应后端 Channel 端 EquityController（/channel-api/equities/*）。
 * 注意：本期后端业务端点尚未实现，调用会走 request.ts 响应拦截器报错，
 * 调用方需 try/catch 降级处理。
 */

/** 权益分页：GET /channel-api/equities */
export function pageEquities(query: EquityQuery): Promise<PageResult<Equity>> {
  return request<PageResult<Equity>>({
    url: '/channel-api/equities',
    method: 'get',
    params: query
  })
}
