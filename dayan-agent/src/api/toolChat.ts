import request from '@/utils/request';
import type { QaConfig, QaSession, QaMessage, QaChatResult } from '@/types';

/**
 * AI 问答（你问我答）人物接口封装。
 * 路由前缀 /tools/qa/*（agent-api），与后端 QaConfig 域对齐。
 */

/** 人物列表 */
export function getQaConfigs(): Promise<QaConfig[]> {
  return request<QaConfig[]>({ url: '/tools/qa/configs', method: 'GET' });
}

/** 会话列表 */
export function getQaSessions(configId: number): Promise<QaSession[]> {
  if (!Number.isFinite(configId) || configId <= 0) return Promise.resolve([]);
  return request<QaSession[]>({ url: '/tools/qa/sessions', method: 'GET', data: { configId } });
}

/** 新建会话（返回 sessionCode） */
export function createQaSession(configId: number, toolCode = 'TL00004'): Promise<string> {
  return request<string>({ url: '/tools/qa/sessions', method: 'POST', data: { configId, toolCode } });
}

/** 删除会话 */
export function deleteQaSession(sessionCode: string): Promise<void> {
  return request<void>({ url: `/tools/qa/sessions/${sessionCode}`, method: 'DELETE' });
}

/** 消息历史 */
export function getQaMessages(sessionCode: string): Promise<QaMessage[]> {
  return request<QaMessage[]>({ url: `/tools/qa/messages/${sessionCode}`, method: 'GET' });
}

/** 问答（第一期走 JSON 非流式，保证小程序兼容） */
export function chatQa(data: { configId: number; toolCode?: string; sessionCode?: string; question: string }): Promise<QaChatResult> {
  return request<QaChatResult>({ url: '/tools/qa/chat', method: 'POST', data });
}
