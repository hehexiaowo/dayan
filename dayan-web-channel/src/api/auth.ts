import { request } from '@/utils/request'
import type { LoginParams, LoginResult } from '@/types/auth'

/**
 * Channel 端认证接口封装。
 *
 * 对应后端 ChannelAuthController（/channel-api/auth/*）。
 * path 不带 /channel-api 前缀时由 Vite 代理转发到网关；生产环境由网关/反代处理同源。
 */

/** 登录：POST /channel-api/auth/login */
export function login(data: LoginParams): Promise<LoginResult> {
  return request<LoginResult>({
    url: '/channel-api/auth/login',
    method: 'post',
    data
  })
}

/** 登出：POST /channel-api/auth/logout */
export function logout(): Promise<void> {
  return request<void>({
    url: '/channel-api/auth/logout',
    method: 'post'
  })
}

/** 当前登录人信息：GET /channel-api/auth/info */
export function getInfo(): Promise<LoginResult> {
  return request<LoginResult>({
    url: '/channel-api/auth/info',
    method: 'get'
  })
}
