import request from '@/utils/request';

export interface ChannelOption {
  channelCode: string;
  shortName?: string;
  fullName?: string;
}

export interface LoginResult {
  token: string;
  tokenName: string;
  agentCode: string;
  channelCode: string;
  realName?: string;
}

export interface SmsSendResult {
  sent: boolean;
  devCode?: string;
}

/** 选渠道：按手机号/用户名/openId 检索关联渠道列表 */
export function getChannelsApi(mobile?: string, openId?: string): Promise<ChannelOption[]> {
  const params: Record<string, string> = {};
  if (mobile) params.mobile = mobile;
  if (openId) params.openId = openId;
  return request<ChannelOption[]>({ url: '/auth/channels', method: 'GET', data: params });
}

/** 密码登录 */
export function loginApi(params: {
  channelCode: string;
  identifier: string;
  password: string;
}): Promise<LoginResult> {
  return request<LoginResult>({ url: '/auth/login', method: 'POST', data: params });
}

/** 发送短信验证码 */
export function sendSmsCodeApi(params: {
  mobile: string;
  channelCode: string;
}): Promise<SmsSendResult> {
  return request<SmsSendResult>({ url: '/auth/sms/send', method: 'POST', data: params });
}

/** 验证码登录 */
export function smsLoginApi(params: {
  mobile: string;
  channelCode: string;
  code: string;
}): Promise<LoginResult> {
  return request<LoginResult>({ url: '/auth/sms/login', method: 'POST', data: params });
}

/** 微信授权登录 */
export function wxLoginApi(params: {
  code: string;
  channelCode: string;
}): Promise<LoginResult> {
  return request<LoginResult>({ url: '/auth/wx/login', method: 'POST', data: params });
}

/** 登出 */
export function logoutApi(): Promise<void> {
  return request<void>({ url: '/auth/logout', method: 'POST' });
}
