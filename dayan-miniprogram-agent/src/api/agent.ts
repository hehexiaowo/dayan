import request from '@/utils/request';
import type { Agent, AgentNotification } from '@/types';

/**
 * 代理人信息（GET /agent-api/agent/info）。
 * 后端业务接口未实现时降级（由调用方 try/catch）。
 */
export function getAgentInfo(): Promise<Agent> {
  return request<Agent>({ url: '/agent/info', method: 'GET' });
}

/**
 * 待办/通知列表（GET /agent-api/notifications）。
 */
export function getNotifications(): Promise<AgentNotification[]> {
  return request<AgentNotification[]>({ url: '/notifications', method: 'GET' });
}
