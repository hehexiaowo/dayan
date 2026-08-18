import request from '@/utils/request';
import type { AichatPersona, AichatSession, AichatMessage, AichatChatResult } from '@/types';

/**
 * AI 问答（你问我答）人物接口封装。
 * 路由前缀 /tools/aichat/*（agent-api）；人物 = tool_info 的 aichat 实例，标识为 toolCode。
 */

/** 人物列表 */
export function getAichatPersonas(): Promise<AichatPersona[]> {
  return request<AichatPersona[]>({ url: '/tools/aichat/configs', method: 'GET' });
}

/** 会话列表 */
export function getAichatSessions(toolCode: string): Promise<AichatSession[]> {
  if (!toolCode) return Promise.resolve([]);
  return request<AichatSession[]>({ url: '/tools/aichat/sessions', method: 'GET', data: { toolCode } });
}

/** 新建会话（返回 sessionCode） */
export function createAichatSession(toolCode: string): Promise<string> {
  return request<string>({ url: '/tools/aichat/sessions', method: 'POST', data: { toolCode } });
}

/** 删除会话 */
export function deleteAichatSession(sessionCode: string): Promise<void> {
  return request<void>({ url: `/tools/aichat/sessions/${sessionCode}`, method: 'DELETE' });
}

/** 消息历史 */
export function getAichatMessages(sessionCode: string): Promise<AichatMessage[]> {
  return request<AichatMessage[]>({ url: `/tools/aichat/messages/${sessionCode}`, method: 'GET' });
}

/** 问答（第一期走 JSON 非流式，保证小程序兼容） */
export function chatAichat(data: { toolCode: string; sessionCode?: string; question: string }): Promise<AichatChatResult> {
  return request<AichatChatResult>({ url: '/tools/aichat/chat', method: 'POST', data });
}
