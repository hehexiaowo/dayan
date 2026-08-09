import { defineStore } from 'pinia';
import { loginApi, getChannelsApi } from '@/api/auth';

interface UserInfo {
  accountCode?: string;
  realName?: string;
  avatar?: string;
  channelCode?: string;
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('agent_token') || '',
    userInfo: (uni.getStorageSync('agent_user') || {}) as UserInfo,
    channelCode: uni.getStorageSync('agent_channel_code') || '',
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
  },
  actions: {
    /** 选渠道：按手机号/openId 检索关联渠道 */
    async getChannels(mobile?: string, openId?: string) {
      return getChannelsApi(mobile, openId);
    },
    /** 登录：channelCode + identifier + password */
    async login(params: { channelCode: string; identifier: string; password: string }) {
      const data = await loginApi(params);
      this.token = data.token;
      this.channelCode = data.channelCode;
      this.userInfo = {
        accountCode: data.agentCode,
        realName: data.realName,
        channelCode: data.channelCode,
      };
      uni.setStorageSync('agent_token', data.token);
      uni.setStorageSync('agent_channel_code', data.channelCode);
      uni.setStorageSync('agent_user', this.userInfo);
      return data;
    },
    logout() {
      this.token = '';
      this.userInfo = {};
      this.channelCode = '';
      uni.removeStorageSync('agent_token');
      uni.removeStorageSync('agent_user');
      uni.removeStorageSync('agent_channel_code');
    },
  },
});
