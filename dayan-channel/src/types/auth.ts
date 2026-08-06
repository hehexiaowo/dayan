/**
 * Channel 端认证相关类型。
 */

/** 登录请求参数 */
export interface LoginParams {
  /** 登录标识：用户名 / 手机号 / 邮箱（三合一） */
  username: string
  /** 密码 */
  password: string
}

/**
 * 登录成功响应（后端 ChannelAuthController AuthLoginVO）。
 *
 * Channel 端额外包含 channelCode。
 */
export interface LoginResult {
  /** Sa-Token 签发的 Token 值 */
  token: string
  /** Token 请求头名称（Channel-Token） */
  tokenName: string
  /** 账号编码 */
  accountCode: string
  /** 真实姓名 */
  realName: string
  /** 头像地址 */
  avatar?: string
  /** 所属渠道编码（Channel 端） */
  channelCode?: string
}

/** 当前登录人信息（同 LoginResult 结构） */
export type UserInfo = LoginResult
