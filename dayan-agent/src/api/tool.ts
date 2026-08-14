import request from '@/utils/request';
import type { ToolInfo } from '@/types';

/**
 * 获客工具列表（GET /agent-api/tools）。
 * 返回启用且 visibleScope 含 agent 的工具，按 sortOrder 升序。
 */
export function getTools(): Promise<ToolInfo[]> {
  return request<ToolInfo[]>({ url: '/tools', method: 'GET' });
}
