/**
 * 公开分享 API（不走 request.ts，避免 401 跳登录）。
 * 客户打开分享链接时由这些接口提供数据。
 */

const BASE_URL = '/agent-api';

export interface ShareResult {
  content: any | null;
  card: any | null;
}

/**
 * 获取分享内容 + 分享人名片（公开接口，无需登录）。
 */
export function getShareContent(code: string, agent: string): Promise<ShareResult> {
  return new Promise((resolve) => {
    uni.request({
      url: BASE_URL + '/open/share/content/' + code + '?agent=' + encodeURIComponent(agent),
      method: 'GET',
      success: (res: any) => {
        const body = res.data;
        if (body && body.code === 0) {
          resolve({
            content: body.data?.content || null,
            card: body.data?.card || null,
          });
        } else {
          resolve({ content: null, card: null });
        }
      },
      fail: () => resolve({ content: null, card: null }),
    });
  });
}
