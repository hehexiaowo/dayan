/**
 * 开放平台相关类型。
 *
 * 字段对齐后端 ChannelOpenPlatformVO（com.dayan.channel.vo）。
 * appSecret 后端脱敏为 ***，明文不回传。
 */

/** 渠道开放平台配置 */
export interface ChannelOpenPlatform {
  id?: number
  channelCode?: string
  platformName?: string
  dockType?: number
  apiBaseUrl?: string
  appKey?: string
  appSecret?: string
  callbackUrl?: string
  h5Domain?: string
  h5Theme?: string
  authType?: number
  ipWhitelist?: string
  rateLimit?: number
  timeout?: number
  extraConfig?: string
  status?: number
  createdAt?: string
  updatedAt?: string
}

/** 对接类型选项 */
export const DOCK_TYPE_OPTIONS = [
  { value: 1, label: 'H5 嵌入' },
  { value: 2, label: 'API 对接' },
  { value: 3, label: 'SDK 集成' }
]

/** 认证方式选项 */
export const AUTH_TYPE_OPTIONS = [
  { value: 1, label: 'Token' },
  { value: 2, label: '签名' }
]
