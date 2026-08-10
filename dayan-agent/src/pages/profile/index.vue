<template>
  <view class="page">
    <!-- 个人信息卡（渐变） -->
    <view class="profile-card">
      <view class="profile-row">
        <!-- 本地头像占位（无 CDN 依赖） -->
        <view class="avatar-wrap">
          <image
            v-if="agentInfo.avatar"
            class="avatar"
            :src="agentInfo.avatar"
            mode="aspectFill"
          />
          <view v-else class="avatar-fallback">
            <text class="avatar-text">{{ avatarChar }}</text>
          </view>
        </view>
        <view class="profile-text">
          <view class="name">{{ displayName }}</view>
          <view class="channel">
            渠道：{{ agentInfo.channelName || agentInfo.channelCode || channelCode || '-' }}
          </view>
        </view>
      </view>
      <view class="profile-meta">
        <text class="meta-item">工号：{{ agentInfo.agentCode || agentCode || '-' }}</text>
        <text v-if="agentInfo.agentLevel" class="meta-item">
          等级：{{ agentInfo.agentLevel }}
        </text>
      </view>
    </view>

    <!-- 菜单列表 -->
    <view class="menu">
      <view
        v-for="item in menuItems"
        :key="item.key"
        class="menu-item dy-clickable"
        @click="onMenu(item)"
      >
        <DyIconBlock
          :text="item.label.charAt(0)"
          :color="item.color"
          size="sm"
          shape="circle"
        />
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
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';

type IconColor = 'blue' | 'green' | 'orange' | 'red' | 'gray';

const userStore = useUserStore();

const agentInfo = ref<Partial<Agent>>({});

const displayName = computed(() => {
  return (
    agentInfo.value.realName ||
    (userStore.userInfo && userStore.userInfo.realName) ||
    agentInfo.value.agentCode ||
    (userStore.userInfo && userStore.userInfo.accountCode) ||
    '代理人'
  );
});

/** 头像首字（无图片时显示） */
const avatarChar = computed(() => {
  const name = displayName.value;
  return name ? name.charAt(0) : '代';
});

const channelCode = computed(() => userStore.channelCode || '');
const agentCode = computed(
  () => (userStore.userInfo && userStore.userInfo.accountCode) || '',
);

interface MenuItem {
  key: string;
  label: string;
  color: IconColor;
}

const menuItems: MenuItem[] = [
  { key: 'stats', label: '经营数据', color: 'blue' },
  { key: 'equity', label: '我的权益', color: 'green' },
  { key: 'orders', label: '我的订单', color: 'orange' },
  { key: 'shares', label: '分享记录', color: 'red' },
  { key: 'settings', label: '设置', color: 'gray' },
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
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  padding: $spacing-md $spacing-md 60rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* 渐变 profile 卡片 */
.profile-card {
  background: $gradient-blue;
  border-radius: $radius-lg;
  padding: 40rpx $spacing-lg;
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(64, 158, 255, 0.25);
}
.profile-row {
  display: flex;
  align-items: center;
}

/* 头像 */
.avatar-wrap {
  width: 96rpx;
  height: 96rpx;
  flex-shrink: 0;
}
.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.6);
  background: #fff;
}
.avatar-fallback {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  border: 4rpx solid rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text {
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
}

.profile-text {
  margin-left: $spacing-md;
  flex: 1;
}
.name {
  font-size: 38rpx;
  font-weight: bold;
}
.channel {
  font-size: 26rpx;
  margin-top: $spacing-xs;
  opacity: 0.9;
}
.profile-meta {
  margin-top: $spacing-md;
  font-size: 24rpx;
  opacity: 0.92;
}
.meta-item {
  margin-right: $spacing-lg;
}

/* 菜单 */
.menu {
  margin-top: $spacing-md;
  background: $bg-card;
  border-radius: $radius-md;
  overflow: hidden;
  box-shadow: $shadow-card;
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx;
  border-bottom: 1rpx solid $border-light;
}
.menu-item:last-child {
  border-bottom: none;
}
.menu-label {
  margin-left: $spacing-md;
  font-size: 30rpx;
  color: $text-primary;
  flex: 1;
}
.menu-arrow {
  font-size: 36rpx;
  color: $text-placeholder;
}
</style>
