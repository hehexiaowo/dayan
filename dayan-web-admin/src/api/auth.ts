import { request } from '@/utils/request'
import type { LoginParams, LoginResult } from '@/types/auth'

/**
 * Admin 端认证接口封装。
 *
 * 对应后端 AdminAuthController（/admin-api/auth/*）。
 * path 不带 /admin-api 前缀时由 Vite 代理转发到网关；生产环境由网关/反代处理同源。
 */

/** 登录：POST /admin-api/auth/login */
export function login(data: LoginParams): Promise<LoginResult> {
  return request<LoginResult>({
    url: '/admin-api/auth/login',
    method: 'post',
    data
  })
}

/** 登出：POST /admin-api/auth/logout */
export function logout(): Promise<void> {
  return request<void>({
    url: '/admin-api/auth/logout',
    method: 'post'
  })
}

/** 当前登录人信息：GET /admin-api/auth/info */
export function getInfo(): Promise<LoginResult> {
  return request<LoginResult>({
    url: '/admin-api/auth/info',
    method: 'get'
  })
}
