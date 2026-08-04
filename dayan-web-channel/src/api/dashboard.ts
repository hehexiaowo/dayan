import { request } from '@/utils/request'

/**
 * Channel 端工作台统计接口封装。
 *
 * 对应后端 Channel 端 DashboardController（/channel-api/dashboard/*）。
 * 注意：本期后端业务端点尚未实现，调用会走 request.ts 响应拦截器报错，
 * 调用方需 try/catch 降级处理（卡片显示 --）。
 */

/** 工作台汇总统计 */
export interface DashboardStats {
  /** 代理人总数 */
  agentCount?: number
  /** 客户总数 */
  clientCount?: number
  /** 权益总数 */
  equityCount?: number
  /** 订单总数 */
  orderCount?: number
}

/** 工作台统计：GET /channel-api/dashboard/stats */
export function getDashboardStats(): Promise<DashboardStats> {
  return request<DashboardStats>({
    url: '/channel-api/dashboard/stats',
    method: 'get'
  })
}
