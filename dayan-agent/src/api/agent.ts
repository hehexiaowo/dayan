import request from '@/utils/request';
import type { Agent, AgentNotification, PageResult } from '@/types';

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
