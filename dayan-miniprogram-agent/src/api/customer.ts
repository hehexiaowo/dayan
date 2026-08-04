import request from '@/utils/request';
import type { Customer, PageQuery } from '@/types';

/**
 * 我的客户列表（GET /agent-api/customers）。
 * 后端业务接口未实现时降级（由调用方 try/catch）。
 */
export function getCustomers(query?: PageQuery): Promise<Customer[]> {
  return request<Customer[]>({
    url: '/customers',
    method: 'GET',
    data: (query || {}) as Record<string, any>,
  });
}
