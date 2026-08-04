/**
 * Agent 端请求封装（基于 uni.request）。
 * 拦截器：注入 Agent-Token 头；处理统一响应 R<T>（code===0 取 data，10100/10101 跳登录）。
 */

const BASE_URL = '/agent-api';

interface R<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
  traceId: string;
}

export function getToken(): string {
  return uni.getStorageSync('agent_token') || '';
}

export function setToken(token: string): void {
  uni.setStorageSync('agent_token', token);
}

export function clearToken(): void {
  uni.removeStorageSync('agent_token');
  uni.removeStorageSync('agent_user');
  uni.removeStorageSync('agent_channel_code');
}

export function request<T = any>(options: UniApp.RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const header = { ...(options.header || {}), 'Agent-Token': getToken() };
    uni.request({
      ...options,
      url: BASE_URL + options.url,
      header,
      success: (res: any) => {
        const body: R<T> = res.data;
        if (body.code === 0) {
          resolve(body.data);
        } else if (body.code === 10100 || body.code === 10101) {
          clearToken();
          uni.reLaunch({ url: '/pages/login/index' });
          reject(new Error(body.message || '未登录'));
        } else {
          uni.showToast({ title: body.message || '请求失败', icon: 'none' });
          reject(new Error(body.message));
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' });
        reject(err);
      },
    });
  });
}

export default request;
