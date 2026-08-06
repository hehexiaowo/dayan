<template>
  <view class="login-page">
    <view class="logo">大雁养老 · 客户端</view>
    <view class="form">
      <view class="form-item">
        <input v-model="mobile" placeholder="请输入手机号" class="input" />
      </view>
      <button class="btn btn-outline" @click="loadChannels" :disabled="loadingChannels">
        {{ loadingChannels ? '查询中...' : '查询关联渠道' }}
      </button>
      <view v-if="channels.length" class="channel-list">
        <view
          v-for="ch in channels"
          :key="ch.channelCode"
          class="channel-item"
          :class="{ active: selectedChannel === ch.channelCode }"
          @click="selectedChannel = ch.channelCode"
        >
          {{ ch.channelName }} ({{ ch.channelCode }})
        </view>
      </view>
      <view class="form-item">
        <input v-model="password" password placeholder="请输入密码" class="input" />
      </view>
      <button class="btn btn-primary" @click="handleLogin" :disabled="submitting">
        {{ submitting ? '登录中...' : '登录' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useUserStore } from '@/stores/user';
import type { ChannelOption } from '@/api/auth';

const userStore = useUserStore();
const mobile = ref('');
const password = ref('');
const channels = ref<ChannelOption[]>([]);
const selectedChannel = ref('');
const loadingChannels = ref(false);
const submitting = ref(false);

async function loadChannels() {
  if (!mobile.value) {
    uni.showToast({ title: '请输入手机号', icon: 'none' });
    return;
  }
  loadingChannels.value = true;
  try {
    channels.value = await userStore.getChannels(mobile.value);
    if (channels.value.length === 1) {
      selectedChannel.value = channels.value[0].channelCode;
    }
    if (channels.value.length === 0) {
      uni.showToast({ title: '未找到关联渠道', icon: 'none' });
    }
  } catch (e) {
    // 错误已由 request 拦截器提示
  } finally {
    loadingChannels.value = false;
  }
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
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => uni.reLaunch({ url: '/pages/home/index' }), 500);
  } catch (e) {
    // 错误已提示
  } finally {
    submitting.value = false;
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  padding: 60rpx 40rpx;
  min-height: 100vh;
  background: #fff;
}
.logo {
  text-align: center;
  font-size: 44rpx;
  font-weight: bold;
  color: #409eff;
  margin: 80rpx 0 60rpx;
}
.form-item {
  margin: 30rpx 0;
}
.input {
  border: 1px solid #dcdfe6;
  border-radius: 8rpx;
  padding: 20rpx;
  font-size: 28rpx;
}
.btn {
  margin: 20rpx 0;
  border-radius: 8rpx;
  font-size: 30rpx;
  &-primary {
    background: #409eff;
    color: #fff;
  }
  &-outline {
    background: #fff;
    color: #409eff;
    border: 1px solid #409eff;
  }
}
.channel-list {
  margin: 20rpx 0;
}
.channel-item {
  padding: 20rpx;
  border: 1px solid #dcdfe6;
  border-radius: 8rpx;
  margin-bottom: 12rpx;
  &.active {
    border-color: #409eff;
    background: #ecf5ff;
    color: #409eff;
  }
}
</style>
