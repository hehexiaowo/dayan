import request from '@/utils/request';
import type { Customer, PageQuery, PageResult } from '@/types';

/**
 * 我的客户列表（GET /agent-api/customers）。
 * 后端业务接口未实现时降级（由调用方 try/catch）。
 */
export function getCustomers(query?: PageQuery): Promise<PageResult<Customer>> {
  return request<PageResult<Customer>>({
    url: '/customers',
    method: 'GET',
    data: (query || {}) as Record<string, unknown>,
  });
}
