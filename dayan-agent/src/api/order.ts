import request from '@/utils/request';

/**
 * 权益商品下单（POST /agent-api/order-equities）。
 * 后端强制注入 channelCode/agentCode/orderSource=2，覆写 unitPrice/goodsName。
 */
export function createOrderEquity(data: {
  goodsCode: string;
  goodsName: string;
  quantity: number;
  unitPrice: number;
}): Promise<string> {
  return request<string>({
    url: '/order-equities',
    method: 'POST',
    data: data as Record<string, unknown>,
  });
}
