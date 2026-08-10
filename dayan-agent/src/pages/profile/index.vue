<template>
  <view class="page">
    <!-- 个人信息卡 -->
    <view class="profile-card">
      <view class="profile-row">
        <image
          class="avatar"
          :src="agentInfo.avatar || defaultAvatar"
          mode="aspectFill"
        />
        <view class="profile-text">
          <view class="name">{{ displayName }}</view>
          <view class="channel">
            渠道：{{ agentInfo.channelName || agentInfo.channelCode || channelCode || '-' }}
          </view>
        </view>
      </view>
      <view class="profile-meta">
        <text class="meta-item">工号：{{ agentInfo.agentCode || agentCode || '-' }}</text>
        <text class="meta-item" v-if="agentInfo.agentLevel">
          等级：{{ agentInfo.agentLevel }}
        </text>
      </view>
    </view>

    <!-- 菜单列表 -->
    <view class="menu">
      <view
        v-for="item in menuItems"
        :key="item.key"
        class="menu-item"
        @click="onMenu(item)"
      >
        <view class="menu-icon" :style="{ background: item.color }">
          <text class="menu-icon-text">{{ item.label.charAt(0) }}</text>
        </view>
        <text class="menu-label">{{ item.label }}</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import { getAgentInfo } from '@/api/agent';
import type { Agent } from '@/types';

const userStore = useUserStore();

const agentInfo = ref<Partial<Agent>>({});
const defaultAvatar = 'https://cdn.uviewui.com/uview/album/1.jpg';

const displayName = computed(() => {
  return (
    agentInfo.value.realName ||
    (userStore.userInfo && userStore.userInfo.realName) ||
    agentInfo.value.agentCode ||
    (userStore.userInfo && userStore.userInfo.accountCode) ||
    '代理人'
  );
});

const channelCode = computed(() => userStore.channelCode || '');
const agentCode = computed(
  () => (userStore.userInfo && userStore.userInfo.accountCode) || '',
);

interface MenuItem {
  key: string;
  label: string;
  color: string;
}

const menuItems: MenuItem[] = [
  { key: 'stats', label: '经营数据', color: '#409eff' },
  { key: 'equity', label: '我的权益', color: '#19be6b' },
  { key: 'orders', label: '我的订单', color: '#ff9900' },
  { key: 'shares', label: '分享记录', color: '#fa3534' },
  { key: 'settings', label: '设置', color: '#909399' },
];

function onMenu(item: MenuItem) {
  const tips: Record<string, string> = {
    stats: '经营数据（Inc 6 上线）',
    equity: '我的权益（Inc 6 上线）',
    orders: '我的订单（Inc 6 上线）',
    shares: '分享记录（Inc 4 上线）',
    settings: '设置（开发中）',
  };
  uni.showToast({ title: tips[item.key] || '开发中', icon: 'none' });
}

async function loadAgentInfo() {
  try {
    const data = await getAgentInfo();
    agentInfo.value = data || {};
  } catch (e) {
    agentInfo.value = {};
  }
}

onMounted(() => {
  loadAgentInfo();
});

onShow(() => {
  loadAgentInfo();
});
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 60rpx;
  min-height: 100vh;
  background: #f5f7fa;
}
.profile-card {
  background: linear-gradient(135deg, #409eff 0%, #5f8afe 100%);
  border-radius: 16rpx;
  padding: 36rpx 32rpx;
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(64, 158, 255, 0.25);
}
.profile-row {
  display: flex;
  align-items: center;
}
.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.6);
  background: #fff;
}
.profile-text {
  margin-left: 24rpx;
  flex: 1;
}
.name {
  font-size: 36rpx;
  font-weight: bold;
}
.channel {
  font-size: 26rpx;
  margin-top: 8rpx;
  opacity: 0.9;
}
.profile-meta {
  margin-top: 24rpx;
  font-size: 24rpx;
  opacity: 0.92;
}
.meta-item {
  margin-right: 32rpx;
}
.menu {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.menu-item:last-child {
  border-bottom: none;
}
.menu-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.menu-icon-text {
  color: #fff;
  font-size: 30rpx;
  font-weight: bold;
}
.menu-label {
  margin-left: 24rpx;
  font-size: 30rpx;
  color: #303133;
  flex: 1;
}
.menu-arrow {
  font-size: 36rpx;
  color: #c0c4cc;
}
</style>
