import { defineStore } from 'pinia';
import {
  loginApi,
  getChannelsApi,
  sendSmsCodeApi,
  smsLoginApi,
  wxLoginApi,
} from '@/api/auth';
import type { LoginResult } from '@/api/auth';

interface UserInfo {
  accountCode?: string;
  realName?: string;
  avatar?: string;
  channelCode?: string;
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('client_token') || '',
    userInfo: (uni.getStorageSync('client_user') || {}) as UserInfo,
    channelCode: uni.getStorageSync('client_channel_code') || '',
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
  },
  actions: {
    /** 选渠道：按手机号/openId 检索关联渠道 */
    async getChannels(mobile?: string, openId?: string) {
      return getChannelsApi(mobile, openId);
    },

    /** 密码登录：channelCode + identifier + password */
    async login(params: { channelCode: string; identifier: string; password: string }) {
      const data = await loginApi(params);
      this.persistLoginResult(data);
      return data;
    },

    /** 发送短信验证码 */
    async sendSmsCode(mobile: string, channelCode: string) {
      return sendSmsCodeApi({ mobile, channelCode });
    },

    /** 验证码登录：channelCode + mobile + code */
    async smsLogin(params: { mobile: string; channelCode: string; code: string }) {
      const data = await smsLoginApi(params);
      this.persistLoginResult(data);
      return data;
    },

    /** 微信授权登录：channelCode + code */
    async wxLogin(params: { code: string; channelCode: string }) {
      const data = await wxLoginApi(params);
      this.persistLoginResult(data);
      return data;
    },

    /** 将登录结果持久化到 state + storage */
    persistLoginResult(data: LoginResult) {
      this.token = data.token;
      this.channelCode = data.channelCode;
      // clientName 来自后端 ClientLoginVO.clientName（client_account.username），用于首页/我的页问候展示
      this.userInfo = {
        accountCode: data.clientCode,
        realName: data.clientName,
        channelCode: data.channelCode,
      };
      uni.setStorageSync('client_token', data.token);
      uni.setStorageSync('client_channel_code', data.channelCode);
      uni.setStorageSync('client_user', this.userInfo);
    },

    logout() {
      this.token = '';
      this.userInfo = {};
      this.channelCode = '';
      uni.removeStorageSync('client_token');
      uni.removeStorageSync('client_user');
      uni.removeStorageSync('client_channel_code');
    },
  },
});
