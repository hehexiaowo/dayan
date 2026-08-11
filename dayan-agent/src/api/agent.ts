import request from '@/utils/request';
import type { Agent, AgentNotification, AgentProfile, AgentProfileUpdatePayload, PageResult } from '@/types';
import type { SmsSendResult } from '@/api/auth';

/**
 * 代理人信息（GET /agent-api/auth/info）。
 * 后端 AgentAuthController.current()，返回 AgentLoginVO（含 agentCode/channelCode）。
 */
export function getAgentInfo(): Promise<Agent> {
  return request<Agent>({ url: '/auth/info', method: 'GET' });
}

/**
 * 待办/通知列表（GET /agent-api/notifications）。
 * 后端业务接口未实现时降级（由调用方 try/catch）。
 */
export function getNotifications(): Promise<PageResult<AgentNotification>> {
  return request<PageResult<AgentNotification>>({ url: '/notifications', method: 'GET' });
}

/** 我的资料（GET /agent-api/profile） */
export function getProfile(): Promise<AgentProfile> {
  return request<AgentProfile>({ url: '/profile', method: 'GET' });
}

/** 更新基础资料（PUT /agent-api/profile） */
export function updateProfile(data: AgentProfileUpdatePayload): Promise<void> {
  return request<void>({ url: '/profile', method: 'PUT', data });
}

/** 换绑手机号-发验证码（POST /agent-api/profile/phone/send） */
export function sendPhoneChangeCode(mobile: string): Promise<SmsSendResult> {
  return request<SmsSendResult>({ url: '/profile/phone/send', method: 'POST', data: { mobile } });
}

/** 换绑手机号（POST /agent-api/profile/phone/change） */
export function changePhone(mobile: string, code: string): Promise<void> {
  return request<void>({ url: '/profile/phone/change', method: 'POST', data: { mobile, code } });
}
