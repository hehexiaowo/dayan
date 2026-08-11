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
      <!-- 共享：手机号/用户名 -->
      <view class="form-item">
        <text class="form-label">{{ loginTab === 'sms' ? '手机号' : '手机号 / 用户名' }}</text>
        <input
          v-model="identifier"
          :placeholder="loginTab === 'sms' ? '请输入手机号' : '请输入手机号或用户名'"
          placeholder-class="input-placeholder"
          :inputmode="loginTab === 'sms' ? 'numeric' : 'text'"
          :maxlength="loginTab === 'sms' ? 11 : 64"
          class="dy-input"
        />
      </view>

      <!-- 渠道列表（输入后自动查询） -->
      <view v-if="channels.length || loadingChannels" class="channel-list">
        <text class="channel-hint">
          {{ loadingChannels ? '正在查询关联渠道...' : '请选择所属渠道' }}
        </text>
        <view
          v-for="ch in channels"
          :key="ch.channelCode"
          class="channel-item"
          :class="{ active: selectedChannel === ch.channelCode }"
          @click="selectedChannel = ch.channelCode"
        >
          <view class="channel-radio">
            <view v-if="selectedChannel === ch.channelCode" class="radio-dot" />
          </view>
          <view class="channel-info">
            <text class="channel-name">{{ ch.shortName || ch.fullName || ch.channelCode }}（{{ ch.channelCode }}）</text>
          </view>
        </view>
      </view>

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

    <!-- 其他方式登录 -->
    <!-- #ifdef MP-WEIXIN -->
    <view class="other-login">
      <view class="divider">
        <view class="divider-line" />
        <text class="divider-text">其他方式登录</text>
        <view class="divider-line" />
      </view>
      <view class="wx-btn" @click="onWxLogin">
        <text class="wx-icon">微</text>
        <text class="wx-text">微信登录</text>
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
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { useUserStore } from '@/stores/user';
import type { ChannelOption } from '@/api/auth';

const userStore = useUserStore();

// 共享状态
const identifier = ref('');
const channels = ref<ChannelOption[]>([]);
const selectedChannel = ref('');
const loadingChannels = ref(false);
const submitting = ref(false);

// Tab 切换：sms=验证码登录（默认），pwd=密码登录
const loginTab = ref<'sms' | 'pwd'>('sms');

// 验证码
const smsCode = ref('');
const countdown = ref(0);
const sendingCode = ref(false);

// 密码
const password = ref('');
const showPwd = ref(false);

let channelTimer: ReturnType<typeof setTimeout> | null = null;
let countdownTimer: ReturnType<typeof setInterval> | null = null;
let channelSeq = 0;

// 输入后自动查询关联渠道
// 验证码 Tab：只接受完整手机号；密码 Tab：手机号或用户名(≥4字)均可
watch(identifier, (val) => {
  channelSeq++;
  loadingChannels.value = false;
  channels.value = [];
  selectedChannel.value = '';
  if (channelTimer) clearTimeout(channelTimer);
  const trimmed = val.trim();
  if (!trimmed) return;
  const isPhone = /^1\d{10}$/.test(trimmed);
  if (loginTab.value === 'sms') {
    // 验证码登录只能发手机号，非完整手机号不查询
    if (isPhone) {
      channelTimer = setTimeout(() => loadChannels(), 500);
    }
  } else {
    // 密码登录：手机号立即查(500ms)；用户名≥4字延迟查(800ms)
    if (isPhone || trimmed.length >= 4) {
      channelTimer = setTimeout(() => loadChannels(), isPhone ? 500 : 800);
    }
  }
});

onUnmounted(() => {
  if (channelTimer) clearTimeout(channelTimer);
  if (countdownTimer) clearInterval(countdownTimer);
});

onMounted(() => {
  identifier.value = uni.getStorageSync('agent_remember_mobile') || '';
});

async function loadChannels() {
  if (!identifier.value) return;
  const seq = ++channelSeq;
  loadingChannels.value = true;
  try {
    const result = await userStore.getChannels(identifier.value);
    if (seq !== channelSeq) return;
    channels.value = result;
    if (channels.value.length === 1) {
      selectedChannel.value = channels.value[0].channelCode;
    }
  } catch {
    // 错误已由 request 拦截器提示
  } finally {
    if (seq === channelSeq) loadingChannels.value = false;
  }
}

/** 发送验证码 */
async function onSendCode() {
  if (countdown.value > 0 || sendingCode.value) return;
  if (!selectedChannel.value) {
    uni.showToast({ title: '请先选择渠道', icon: 'none' });
    return;
  }
  if (!/^1\d{10}$/.test(identifier.value)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' });
    return;
  }
  sendingCode.value = true;
  try {
    const result = await userStore.sendSmsCode(identifier.value, selectedChannel.value);
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
  if (!selectedChannel.value) {
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
      mobile: identifier.value,
      channelCode: selectedChannel.value,
      code: smsCode.value,
    });
    uni.setStorageSync('agent_remember_mobile', identifier.value);
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
  if (!selectedChannel.value) {
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
      channelCode: selectedChannel.value,
      identifier: identifier.value,
      password: password.value,
    });
    uni.setStorageSync('agent_remember_mobile', identifier.value);
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
  if (!selectedChannel.value) {
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
    await userStore.wxLogin({ code: res.code, channelCode: selectedChannel.value });
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
@import '@/styles/variables.scss';

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

/* 其他方式登录 */
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

.wx-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-md;
}

.wx-icon {
  width: 96rpx;
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  border-radius: 50%;
  background: #07c160;
  color: #fff;
  font-size: 40rpx;
  font-weight: bold;
}

.wx-text {
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: $spacing-sm;
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
