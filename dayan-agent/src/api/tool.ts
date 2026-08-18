import request from '@/utils/request';
import type { ToolInfo } from '@/types';

/**
 * 工具接口封装（agent 端）。
 * 工具列表：GET /agent-api/tools（返回启用且 visibleScope 含 agent 的工具，按 sortOrder 升序）。
 * 计算器使用记录：POST /agent-api/tools/calculator/{pension|gap}。
 */

/** 工具实例列表 */
export function getTools(): Promise<ToolInfo[]> {
  return request<ToolInfo[]>({ url: '/tools', method: 'GET' });
}

/** 保存社保养老计算器使用记录 */
export function savePensionCalculatorRecord(input: unknown, result: unknown, toolCode = 'TL00001'): Promise<string> {
  return request<string>({
    url: '/tools/calculator/pension',
    method: 'POST',
    data: { toolCode, inputJson: JSON.stringify(input), resultJson: JSON.stringify(result) }
  });
}

/** 保存养老缺口计算器使用记录 */
export function saveGapCalculatorRecord(input: unknown, result: unknown, toolCode = 'TL00002'): Promise<string> {
  return request<string>({
    url: '/tools/calculator/gap',
    method: 'POST',
    data: { toolCode, inputJson: JSON.stringify(input), resultJson: JSON.stringify(result) }
  });
}
