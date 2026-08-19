/**
 * 公开分享 API（不走 request.ts，避免 401 跳登录）。
 * 客户打开分享链接时由这些接口提供数据。
 */

const BASE_URL = '/agent-api';

export interface ShareContent {
  title?: string;
  subtitle?: string;
  contentBody?: string;
  contentType?: number;
  coverImage?: string;
  summary?: string;
  authorName?: string;
  publishTime?: string;
  viewCount?: number;
  collectCount?: number;
  tags?: string;
  sourceUrl?: string;
  sourceType?: number;
  categoryName?: string;
  [key: string]: unknown;
}

export interface ShareCard {
  displayName?: string;
  title?: string;
  phone?: string;
  wechat?: string;
  email?: string;
  company?: string;
  avatar?: string;
  intro?: string;
  tags?: string;
  accountCode?: string;
  [key: string]: unknown;
}

export interface ShareResult {
  content: ShareContent | null;
  card: ShareCard | null;
}

/**
 * 获取分享内容 + 分享人名片（公开接口，无需登录）。
 */
export function getShareContent(code: string, agent: string): Promise<ShareResult> {
  return new Promise((resolve) => {
    uni.request({
      url: BASE_URL + '/open/share/content/' + code + '?agent=' + encodeURIComponent(agent),
      method: 'GET',
      success: (res: UniApp.RequestSuccessCallbackResult) => {
        const body = res.data as { code?: number; data?: { content?: ShareContent; card?: ShareCard } };
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

export interface PosterData {
  title?: string;
  subtitle?: string;
  bodyText?: string;
  coverImage?: string;
  categoryName?: string;
  [key: string]: unknown;
}

export interface PosterShareResult {
  poster: PosterData | null;
  card: ShareCard | null;
}

/** 获取分享海报 + 分享人名片（公开接口，无需登录）。 */
export function getSharePoster(code: string, agent: string): Promise<PosterShareResult> {
  return new Promise((resolve) => {
    uni.request({
      url: BASE_URL + '/open/share/poster/' + code + '?agent=' + encodeURIComponent(agent),
      method: 'GET',
      success: (res: UniApp.RequestSuccessCallbackResult) => {
        const body = res.data as { code?: number; data?: { poster?: PosterData; card?: ShareCard } };
        if (body && body.code === 0) {
          resolve({
            poster: body.data?.poster || null,
            card: body.data?.card || null,
          });
        } else {
          resolve({ poster: null, card: null });
        }
      },
      fail: () => resolve({ poster: null, card: null }),
    });
  });
}

/** 获取代理人名片（公开接口，工具分享用）。 */
export function getShareAgentCard(agent: string): Promise<ShareCard | null> {
  return new Promise((resolve) => {
    uni.request({
      url: BASE_URL + '/open/share/agent-card?agent=' + encodeURIComponent(agent),
      method: 'GET',
      success: (res: UniApp.RequestSuccessCallbackResult) => {
        const body = res.data as { code?: number; data?: ShareCard };
        if (body && body.code === 0) {
          resolve(body.data || null);
        } else {
          resolve(null);
        }
      },
      fail: () => resolve(null),
    });
  });
}

// ===== 访客追踪 =====

const VISITOR_TOKEN_KEY = 'share_visitor_token';

/** 获取本地存储的访客令牌（首次为空，由后端生成后存回） */
export function getVisitorToken(): string {
  return uni.getStorageSync(VISITOR_TOKEN_KEY) || '';
}

/** 存储访客令牌 */
export function saveVisitorToken(token: string): void {
  uni.setStorageSync(VISITOR_TOKEN_KEY, token);
}

/** 检测微信环境 */
export function isWechatBrowser(): boolean {
  // #ifdef H5
  return navigator.userAgent.includes('MicroMessenger');
  // #endif
  // #ifndef H5
  return false;
  // #endif
}

/**
 * 追踪分享链接打开（公开接口，自动创建/更新访客线索）。
 * 分享页 onLoad 时调用，记录访客浏览行为。
 */
export function trackShare(data: {
  agentCode: string;
  shareType: number; // 1=内容 2=工具 3=海报
  bizCode: string;
  bizTitle: string;
  visitorToken?: string;
  visitorSource?: string;
}): Promise<string> {
  return new Promise((resolve) => {
    uni.request({
      url: BASE_URL + '/open/share/track',
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: {
        agentCode: data.agentCode,
        shareType: data.shareType,
        bizCode: data.bizCode,
        bizTitle: data.bizTitle,
        visitorToken: data.visitorToken || getVisitorToken(),
        visitorSource: data.visitorSource || (isWechatBrowser() ? 'wechat' : 'browser'),
      },
      success: (res: UniApp.RequestSuccessCallbackResult) => {
        const body = res.data as { code?: number; data?: { visitorToken?: string } };
        if (body && body.code === 0 && body.data?.visitorToken) {
          saveVisitorToken(body.data.visitorToken);
          resolve(body.data.visitorToken);
        } else {
          resolve('');
        }
      },
      fail: () => resolve(''),
    });
  });
}

/**
 * 客户留资（公开接口，更新访客线索的手机号/姓名）。
 */
export function leaveContact(data: {
  visitorToken: string;
  phone: string;
  name?: string;
}): Promise<boolean> {
  return new Promise((resolve) => {
    uni.request({
      url: BASE_URL + '/open/share/contact',
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: {
        visitorToken: data.visitorToken || getVisitorToken(),
        phone: data.phone,
        name: data.name,
      },
      success: (res: UniApp.RequestSuccessCallbackResult) => {
        const body = res.data as { code?: number };
        resolve(body && body.code === 0);
      },
      fail: () => resolve(false),
    });
  });
}
