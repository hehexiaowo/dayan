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
      <view class="form-item">
        <text class="form-label">手机号</text>
        <input
          v-model="mobile"
          placeholder="请输入手机号"
          placeholder-class="input-placeholder"
          inputmode="numeric"
          maxlength="11"
          class="form-input"
        />
      </view>

      <button
        class="btn btn-outline"
        :class="{ 'is-disabled': loadingChannels }"
        :disabled="loadingChannels"
        @click="onManualQuery"
      >
        <text v-if="loadingChannels">查询中...</text>
        <text v-else>查询关联渠道</text>
      </button>

      <!-- 渠道列表 -->
      <view v-if="channels.length" class="channel-list">
        <text class="channel-hint">请选择所属渠道</text>
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
            <text class="channel-name">{{ ch.channelName }}</text>
            <text class="channel-code">{{ ch.channelCode }}</text>
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">密码</text>
        <view class="pwd-wrap">
          <input
            v-model="password"
            :password="!showPwd"
            placeholder="请输入密码"
            placeholder-class="input-placeholder"
            class="form-input"
            style="padding-right: 120rpx"
          />
          <text class="pwd-toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</text>
        </view>
      </view>

      <button
        class="btn btn-primary"
        :class="{ 'is-disabled': submitting }"
        :disabled="submitting"
        @click="handleLogin"
      >
        <text v-if="submitting">登录中...</text>
        <text v-else>登 录</text>
      </button>
    </view>

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
const mobile = ref('');
const password = ref('');
const showPwd = ref(false);
const channels = ref<ChannelOption[]>([]);
const selectedChannel = ref('');
const loadingChannels = ref(false);
const submitting = ref(false);

let channelTimer: ReturnType<typeof setTimeout> | null = null;
let channelSeq = 0;
watch(mobile, (val) => {
  channelSeq++;
  channels.value = [];
  selectedChannel.value = '';
  if (channelTimer) clearTimeout(channelTimer);
  if (/^1\d{10}$/.test(val)) {
    channelTimer = setTimeout(() => loadChannels(), 500);
  }
});

onUnmounted(() => {
  if (channelTimer) clearTimeout(channelTimer);
});

onMounted(() => {
  mobile.value = uni.getStorageSync('agent_remember_mobile') || '';
});

async function loadChannels() {
  if (!mobile.value) {
    uni.showToast({ title: '请输入手机号', icon: 'none' });
    return;
  }
  // 序号守卫：改号/重复触发时，旧响应落地即丢弃
  const seq = ++channelSeq;
  loadingChannels.value = true;
  try {
    const result = await userStore.getChannels(mobile.value);
    if (seq !== channelSeq) return;
    channels.value = result;
    if (channels.value.length === 1) {
      selectedChannel.value = channels.value[0].channelCode;
    }
    if (channels.value.length === 0) {
      uni.showToast({ title: '未找到关联渠道', icon: 'none' });
    }
  } catch (e) {
    // 错误已由 request 拦截器提示
  } finally {
    if (seq === channelSeq) loadingChannels.value = false;
  }
}

function onManualQuery() {
  if (channelTimer) clearTimeout(channelTimer);
  loadChannels();
}

async function handleLogin() {
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
      identifier: mobile.value,
      password: password.value,
    });
    uni.setStorageSync('agent_remember_mobile', mobile.value);
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => uni.switchTab({ url: '/pages/acquisition/index' }), 500);
  } catch (e) {
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

.form-input {
  width: 100%;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  padding: 24rpx 28rpx;
  font-size: 30rpx;
  color: $text-primary;
  transition: border-color $transition-base;

  &:focus {
    border-color: $brand-primary;
  }
}

.input-placeholder {
  color: $text-placeholder;
  font-size: 28rpx;
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
}

/* 按钮 */
.btn {
  width: 100%;
  border-radius: $radius-md;
  font-size: 32rpx;
  font-weight: 500;
  padding: 24rpx 0;
  margin-top: $spacing-sm;
  transition: all $transition-base;

  &:active {
    transform: scale(0.98);
  }

  &.is-disabled {
    opacity: 0.6;
  }
}

.btn-primary {
  background: $gradient-blue;
  color: #fff;
  box-shadow: 0 8rpx 20rpx rgba(64, 158, 255, 0.3);
}

.btn-outline {
  background: $bg-card;
  color: $brand-primary;
  border: 2rpx solid $brand-primary;
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

.channel-info {
  display: flex;
  flex-direction: column;
}

.channel-name {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
}

.channel-code {
  font-size: 22rpx;
  color: $text-secondary;
  margin-top: 4rpx;
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
