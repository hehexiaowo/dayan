import request from '@/utils/request';

export interface ChannelOption {
  channelCode: string;
  channelName: string;
}

export interface LoginResult {
  token: string;
  tokenName: string;
  agentCode: string;
  channelCode: string;
}

/** 选渠道：按手机号/openId 检索关联渠道列表 */
export function getChannelsApi(mobile?: string, openId?: string): Promise<ChannelOption[]> {
  const params: Record<string, string> = {};
  if (mobile) params.mobile = mobile;
  if (openId) params.openId = openId;
  return request<ChannelOption[]>({ url: '/auth/channels', method: 'GET', data: params });
}

/** 登录 */
export function loginApi(params: {
  channelCode: string;
  identifier: string;
  password: string;
}): Promise<LoginResult> {
  return request<LoginResult>({ url: '/auth/login', method: 'POST', data: params });
}

/** 登出 */
export function logoutApi(): Promise<void> {
  return request<void>({ url: '/auth/logout', method: 'POST' });
}
