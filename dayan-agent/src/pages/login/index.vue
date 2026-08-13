<template>
  <view class="login-page">
    <!-- 品牌区 -->
    <view class="brand-section">
      <view class="brand-logo">
        <text class="brand-title">大雁养老</text>
        <text class="brand-subtitle">代理人工作台</text>
      </view>
    </view>

    <!-- 表单卡片 -->
    <view class="form-card">
      <!-- Tab 切换 -->
      <view class="tab-bar">
        <view
          class="tab-item"
          :class="{ active: loginTab === 'sms' }"
          @click="loginTab = 'sms'"
        >
          <text class="tab-text">验证码登录</text>
          <view v-if="loginTab === 'sms'" class="tab-underline" />
        </view>
        <view
          class="tab-item"
          :class="{ active: loginTab === 'pwd' }"
          @click="loginTab = 'pwd'"
        >
          <text class="tab-text">密码登录</text>
          <view v-if="loginTab === 'pwd'" class="tab-underline" />
        </view>
      </view>

      <!-- 验证码登录 -->
      <view v-if="loginTab === 'sms'" class="tab-content">
        <!-- 手机号 -->
        <view class="form-item">
          <text class="form-label">手机号</text>
          <input
            v-model="smsMobile"
            placeholder="请输入手机号"
            placeholder-class="input-placeholder"
            inputmode="numeric"
            maxlength="11"
            class="dy-input"
          />
        </view>

        <!-- 渠道列表 -->
        <view v-if="smsChannels.length || smsLoading" class="channel-list">
          <text class="channel-hint">
            {{ smsLoading ? '正在查询关联渠道...' : '请选择所属渠道' }}
          </text>
          <view
            v-for="ch in smsChannels"
            :key="ch.channelCode"
            class="channel-item"
            :class="{ active: smsChannel === ch.channelCode }"
            @click="smsChannel = ch.channelCode"
          >
            <view class="channel-radio">
              <view v-if="smsChannel === ch.channelCode" class="radio-dot" />
            </view>
            <view class="channel-info">
              <text class="channel-name">{{ ch.shortName || ch.fullName || ch.channelCode }}（{{ ch.channelCode }}）</text>
            </view>
          </view>
        </view>

        <!-- 验证码 -->
        <view class="form-item">
          <text class="form-label">验证码</text>
          <view class="code-wrap">
            <input
              v-model="smsCode"
              placeholder="请输入验证码"
              placeholder-class="input-placeholder"
              inputmode="numeric"
              maxlength="6"
              class="dy-input code-input"
            />
            <view
              class="code-btn"
              :class="{ disabled: countdown > 0 || sendingCode }"
              @click="onSendCode"
            >
              <text v-if="sendingCode">发送中</text>
              <text v-else-if="countdown > 0">{{ countdown }}s</text>
              <text v-else>获取验证码</text>
            </view>
          </view>
        </view>

        <button
          class="dy-btn dy-btn-primary"
          :class="{ 'dy-btn-disabled': submitting }"
          :disabled="submitting"
          @click="onSmsLogin"
        >
          <text v-if="submitting">登录中...</text>
          <text v-else>登 录</text>
        </button>
      </view>

      <!-- 密码登录 -->
      <view v-if="loginTab === 'pwd'" class="tab-content">
        <!-- 手机号 / 用户名 -->
        <view class="form-item">
          <text class="form-label">手机号 / 用户名</text>
          <input
            v-model="pwdInput"
            placeholder="请输入手机号或用户名"
            placeholder-class="input-placeholder"
            inputmode="text"
            maxlength="64"
            class="dy-input"
          />
        </view>

        <!-- 渠道列表 -->
        <view v-if="pwdChannels.length || pwdLoading" class="channel-list">
          <text class="channel-hint">
            {{ pwdLoading ? '正在查询关联渠道...' : '请选择所属渠道' }}
          </text>
          <view
            v-for="ch in pwdChannels"
            :key="ch.channelCode"
            class="channel-item"
            :class="{ active: pwdChannel === ch.channelCode }"
            @click="pwdChannel = ch.channelCode"
          >
            <view class="channel-radio">
              <view v-if="pwdChannel === ch.channelCode" class="radio-dot" />
            </view>
            <view class="channel-info">
              <text class="channel-name">{{ ch.shortName || ch.fullName || ch.channelCode }}（{{ ch.channelCode }}）</text>
            </view>
          </view>
        </view>

        <!-- 密码 -->
        <view class="form-item">
          <text class="form-label">密码</text>
          <view class="pwd-wrap">
            <input
              v-model="password"
              :password="!showPwd"
              placeholder="请输入密码"
              placeholder-class="input-placeholder"
              class="dy-input"
              style="padding-right: 120rpx"
            />
            <text class="pwd-toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</text>
          </view>
        </view>

        <button
          class="dy-btn dy-btn-primary"
          :class="{ 'dy-btn-disabled': submitting }"
          :disabled="submitting"
          @click="onPwdLogin"
        >
          <text v-if="submitting">登录中...</text>
          <text v-else>登 录</text>
        </button>
      </view>
    </view>

    <!-- 微信授权登录（仅小程序） -->
    <!-- #ifdef MP-WEIXIN -->
    <view class="other-login">
      <view class="divider">
        <view class="divider-line" />
        <text class="divider-text">微信授权登录</text>
        <view class="divider-line" />
      </view>
      <view class="wx-card">
        <view class="form-item">
          <text class="form-label">手机号</text>
          <input
            v-model="wxMobile"
            placeholder="请输入手机号"
            placeholder-class="input-placeholder"
            inputmode="numeric"
            maxlength="11"
            class="dy-input"
          />
        </view>
        <view v-if="wxChannels.length || wxLoading" class="channel-list">
          <text class="channel-hint">
            {{ wxLoading ? '正在查询关联渠道...' : '请选择所属渠道' }}
          </text>
          <view
            v-for="ch in wxChannels"
            :key="ch.channelCode"
            class="channel-item"
            :class="{ active: wxChannel === ch.channelCode }"
            @click="wxChannel = ch.channelCode"
          >
            <view class="channel-radio">
              <view v-if="wxChannel === ch.channelCode" class="radio-dot" />
            </view>
            <view class="channel-info">
              <text class="channel-name">{{ ch.shortName || ch.fullName || ch.channelCode }}（{{ ch.channelCode }}）</text>
            </view>
          </view>
        </view>
        <view class="wx-btn" @click="onWxLogin">
          <text class="wx-icon">微</text>
          <text class="wx-text">微信授权登录</text>
        </view>
      </view>
    </view>
    <!-- #endif -->

    <!-- 底部版权 -->
    <view class="footer">
      <text class="footer-text">大雁养老 · 专业养老服务平台</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch, type Ref } from 'vue';
import { useUserStore } from '@/stores/user';
import type { ChannelOption } from '@/api/auth';

const userStore = useUserStore();

// Tab 切换：sms=验证码登录（默认），pwd=密码登录
const loginTab = ref<'sms' | 'pwd'>('sms');

// 通用状态
const submitting = ref(false);
const countdown = ref(0);
const sendingCode = ref(false);
let countdownTimer: ReturnType<typeof setInterval> | null = null;

// 验证码 Tab 独立状态
const smsMobile = ref('');
const smsChannels = ref<ChannelOption[]>([]);
const smsChannel = ref('');
const smsLoading = ref(false);
const smsCode = ref('');

// 密码 Tab 独立状态
const pwdInput = ref('');
const pwdChannels = ref<ChannelOption[]>([]);
const pwdChannel = ref('');
const pwdLoading = ref(false);
const password = ref('');
const showPwd = ref(false);

// 微信独立状态（#ifdef MP-WEIXIN）
const wxMobile = ref('');
const wxChannels = ref<ChannelOption[]>([]);
const wxChannel = ref('');
const wxLoading = ref(false);

/**
 * 渠道自动查询接线函数。
 * 为每组 (input, channels, selected, loading) 绑定独立的 watch + 防抖 + seq 防竞态。
 * phoneOnly=true: 只接受完整手机号触发查询（验证码/微信）
 * phoneOnly=false: 手机号或用户名(≥4字)均可（密码）
 */
function wireChannelQuery(
  input: Ref<string>,
  channels: Ref<ChannelOption[]>,
  selected: Ref<string>,
  loading: Ref<boolean>,
  phoneOnly: boolean,
) {
  let timer: ReturnType<typeof setTimeout> | null = null;
  let seq = 0;

  async function doQuery() {
    const trimmed = input.value.trim();
    if (!trimmed) return;
    const current = ++seq;
    loading.value = true;
    try {
      const result = await userStore.getChannels(trimmed);
      if (current !== seq) return;
      channels.value = result;
      if (result.length === 1) {
        selected.value = result[0].channelCode;
      }
    } catch {
      // 错误已由 request 拦截器提示
    } finally {
      if (current === seq) loading.value = false;
    }
  }

  watch(input, (val) => {
    seq++;
    loading.value = false;
    channels.value = [];
    selected.value = '';
    if (timer) clearTimeout(timer);
    const trimmed = val.trim();
    if (!trimmed) return;
    const isPhone = /^1\d{10}$/.test(trimmed);
    if (phoneOnly) {
      if (isPhone) timer = setTimeout(doQuery, 500);
    } else {
      if (isPhone || trimmed.length >= 4) {
        timer = setTimeout(doQuery, isPhone ? 500 : 800);
      }
    }
  });

  onUnmounted(() => {
    if (timer) clearTimeout(timer);
  });
}

// 各登录方式独立绑定
wireChannelQuery(smsMobile, smsChannels, smsChannel, smsLoading, true);
wireChannelQuery(pwdInput, pwdChannels, pwdChannel, pwdLoading, false);
wireChannelQuery(wxMobile, wxChannels, wxChannel, wxLoading, true);

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer);
});

onMounted(() => {
  smsMobile.value = uni.getStorageSync('agent_remember_mobile') || '';
});

/** 发送验证码 */
async function onSendCode() {
  if (countdown.value > 0 || sendingCode.value) return;
  if (!smsChannel.value) {
    uni.showToast({ title: '请先选择渠道', icon: 'none' });
    return;
  }
  if (!/^1\d{10}$/.test(smsMobile.value)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' });
    return;
  }
  sendingCode.value = true;
  try {
    const result = await userStore.sendSmsCode(smsMobile.value, smsChannel.value);
    // 启动倒计时
    countdown.value = 60;
    countdownTimer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer);
        countdownTimer = null;
      }
    }, 1000);
    // Dev 环境：后端返回 devCode，toast 展示便于测试
    if (result.devCode) {
      uni.showToast({ title: `验证码：${result.devCode}`, icon: 'none', duration: 5000 });
    } else {
      uni.showToast({ title: '验证码已发送', icon: 'none' });
    }
  } catch {
    // 错误已提示
  } finally {
    sendingCode.value = false;
  }
}

/** 验证码登录 */
async function onSmsLogin() {
  if (!smsChannel.value) {
    uni.showToast({ title: '请先选择渠道', icon: 'none' });
    return;
  }
  if (!smsCode.value) {
    uni.showToast({ title: '请输入验证码', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    await userStore.smsLogin({
      mobile: smsMobile.value,
      channelCode: smsChannel.value,
      code: smsCode.value,
    });
    uni.setStorageSync('agent_remember_mobile', smsMobile.value);
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => uni.switchTab({ url: '/pages/acquisition/index' }), 500);
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false;
  }
}

/** 密码登录 */
async function onPwdLogin() {
  if (!pwdChannel.value) {
    uni.showToast({ title: '请先选择渠道', icon: 'none' });
    return;
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    await userStore.login({
      channelCode: pwdChannel.value,
      identifier: pwdInput.value,
      password: password.value,
    });
    uni.setStorageSync('agent_remember_mobile', pwdInput.value);
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => uni.switchTab({ url: '/pages/acquisition/index' }), 500);
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false;
  }
}

/** 微信登录（仅小程序） */
async function onWxLogin() {
  if (!wxChannel.value) {
    uni.showToast({ title: '请先选择渠道', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    // #ifdef MP-WEIXIN
    const res = await uni.login({ provider: 'weixin' });
    if (!res || !res.code) {
      uni.showToast({ title: '微信授权失败', icon: 'none' });
      return;
    }
    await userStore.wxLogin({ code: res.code, channelCode: wxChannel.value });
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => uni.switchTab({ url: '/pages/acquisition/index' }), 500);
    // #endif
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false;
  }
}
</script>

<style lang="scss" scoped>

.login-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

/* 品牌区 */
.brand-section {
  background: $gradient-blue;
  padding: 100rpx 48rpx 80rpx;
  border-bottom-left-radius: 48rpx;
  border-bottom-right-radius: 48rpx;
}

.brand-logo {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 56rpx;
  font-weight: bold;
  color: #fff;
  letter-spacing: 4rpx;
}

.brand-subtitle {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 12rpx;
  letter-spacing: 2rpx;
}

/* 表单卡片 */
.form-card {
  margin: -32rpx $spacing-lg 0;
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  box-shadow: $shadow-hover;
}

.form-item {
  margin-bottom: $spacing-lg;
}

.form-label {
  display: block;
  font-size: 26rpx;
  color: $text-regular;
  margin-bottom: $spacing-sm;
  font-weight: 500;
}

.input-placeholder {
  color: $text-placeholder;
  font-size: 28rpx;
}

/* 登录页按钮间距 */
.form-card .dy-btn {
  margin-top: $spacing-sm;
}

.pwd-wrap,
.code-wrap {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.code-input {
  flex: 1;
  min-width: 0;
}

.pwd-wrap {
  position: relative;
}

.pwd-toggle {
  position: absolute;
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 26rpx;
  color: $brand-primary;
  padding: 8rpx;
  z-index: 2;
}

.code-btn {
  flex-shrink: 0;
  font-size: 24rpx;
  color: $brand-primary;
  padding: 14rpx 20rpx;
  border-radius: $radius-sm;
  background: $brand-primary-light;
  white-space: nowrap;

  &.disabled {
    color: $text-placeholder;
    background: $bg-page;
  }
}

/* 渠道列表 */
.channel-list {
  margin: $spacing-md 0 $spacing-lg;
}

.channel-hint {
  display: block;
  font-size: 26rpx;
  color: $text-secondary;
  margin-bottom: $spacing-sm;
}

.channel-item {
  display: flex;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  transition: all $transition-base;

  &:active {
    background: $bg-page;
  }

  &.active {
    border-color: $brand-primary;
    background: $brand-primary-light;
  }
}

.channel-radio {
  width: 36rpx;
  height: 36rpx;
  border: 4rpx solid $border-base;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: $spacing-md;
  flex-shrink: 0;
  transition: border-color $transition-base;

  .active & {
    border-color: $brand-primary;
  }
}

.radio-dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: $gradient-blue;
}

.channel-name {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
}

/* Tab 切换 */
.tab-bar {
  display: flex;
  margin-bottom: $spacing-lg;
  border-bottom: 2rpx solid $border-base;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: $spacing-sm 0 $spacing-md;
  position: relative;
}

.tab-text {
  font-size: 28rpx;
  color: $text-secondary;
}

.tab-item.active .tab-text {
  color: $brand-primary;
  font-weight: 600;
}

.tab-underline {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: $gradient-blue;
}

/* 微信授权登录 */
.other-login {
  margin: $spacing-xl $spacing-lg 0;
}

.divider {
  display: flex;
  align-items: center;
  margin-bottom: $spacing-lg;
}

.divider-line {
  flex: 1;
  height: 2rpx;
  background: $border-base;
}

.divider-text {
  font-size: 24rpx;
  color: $text-placeholder;
  padding: 0 $spacing-md;
}

.wx-card {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  box-shadow: $shadow-hover;
}

.wx-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
  padding: 24rpx;
  border-radius: $radius-md;
  background: #07c160;

  &:active {
    opacity: 0.85;
  }
}

.wx-icon {
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 28rpx;
  font-weight: bold;
}

.wx-text {
  font-size: 32rpx;
  color: #fff;
  font-weight: 500;
}

/* 底部 */
.footer {
  flex: 1;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 60rpx;
}

.footer-text {
  font-size: 22rpx;
  color: $text-placeholder;
}
</style>
