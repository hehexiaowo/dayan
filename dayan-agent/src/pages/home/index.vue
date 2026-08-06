<template>
  <view class="page">
    <!-- 欢迎卡 -->
    <view class="welcome-card">
      <view class="welcome-row">
        <image
          class="avatar"
          :src="agentInfo.avatar || defaultAvatar"
          mode="aspectFill"
        />
        <view class="welcome-text">
          <view class="greet">你好，{{ displayName }}</view>
          <view class="channel">
            渠道：{{ agentInfo.channelName || agentInfo.channelCode || channelCode || '-' }}
          </view>
        </view>
      </view>
      <view class="welcome-meta">
        <text class="meta-item">工号：{{ agentInfo.agentCode || agentCode || '-' }}</text>
        <text class="meta-item" v-if="agentInfo.agentLevel">
          等级：{{ agentInfo.agentLevel }}
        </text>
      </view>
    </view>

    <!-- 功能入口宫格 -->
    <view class="grid">
      <view
        v-for="item in entries"
        :key="item.key"
        class="grid-item"
        @click="onEntry(item)"
      >
        <view class="grid-icon" :style="{ background: item.color }">
          <text class="grid-icon-text">{{ item.label.charAt(0) }}</text>
        </view>
        <text class="grid-label">{{ item.label }}</text>
      </view>
    </view>

    <!-- 待办/通知列表 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">待办 / 通知</text>
        <text class="section-more" v-if="notifications.length" @click="onMore">
          更多
        </text>
      </view>

      <view v-if="loadingNotif && !notifications.length" class="empty">
        加载中...
      </view>
      <view v-else-if="!notifications.length" class="empty">
        暂无通知（接口待后端提供）
      </view>

      <view v-else class="notif-list">
        <view
          v-for="n in notifications"
          :key="n.id"
          class="notif-card"
          @click="onNotif(n)"
        >
          <view class="notif-title">{{ n.title }}</view>
          <view class="notif-content" v-if="n.content">{{ n.content }}</view>
          <view class="notif-time">{{ n.createdAt || '' }}</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import { getAgentInfo, getNotifications } from '@/api/agent';
import type { Agent, AgentNotification } from '@/types';

const userStore = useUserStore();

const agentInfo = ref<Partial<Agent>>({});
const notifications = ref<AgentNotification[]>([]);
const loadingNotif = ref(false);

const defaultAvatar =
  'https://cdn.uviewui.com/uview/album/1.jpg';

/** 显示名优先用 realName，其次 userInfo 内字段，最后 agentCode。 */
const displayName = computed(() => {
  return (
    agentInfo.value.realName ||
    (userStore.userInfo && userStore.userInfo.realName) ||
    agentInfo.value.agentCode ||
    (userStore.userInfo && userStore.userInfo.accountCode) ||
    '代理人'
  );
});

/** 本地兜底：渠道编码（登录后 store 内已存）。 */
const channelCode = computed(() => userStore.channelCode || '');
const agentCode = computed(
  () => (userStore.userInfo && userStore.userInfo.accountCode) || '',
);

interface EntryItem {
  key: string;
  label: string;
  color: string;
  /** tabBar 页面用 switchTab；非 tabBar 用 navigateTo。 */
  tab?: boolean;
  url: string;
}

const entries: EntryItem[] = [
  { key: 'acquisition', label: '获客', color: '#409eff', tab: true, url: '/pages/acquisition/index' },
  { key: 'customer', label: '客户', color: '#19be6b', tab: true, url: '/pages/customer/index' },
  { key: 'activity', label: '活动', color: '#ff9900', tab: true, url: '/pages/activity/index' },
  { key: 'performance', label: '业绩', color: '#fa3534', tab: false, url: '/pages/home/index' },
];

function onEntry(item: EntryItem) {
  if (item.key === 'performance') {
    // 业绩页本期未实现，占位提示
    uni.showToast({ title: '业绩模块开发中', icon: 'none' });
    return;
  }
  if (item.tab) {
    uni.switchTab({ url: item.url });
  } else {
    uni.navigateTo({ url: item.url });
  }
}

async function loadAgentInfo() {
  try {
    const data = await getAgentInfo();
    agentInfo.value = data || {};
  } catch (e) {
    // 接口待后端提供：使用 store 兜底字段，不崩溃
    agentInfo.value = {};
  }
}

async function loadNotifications() {
  loadingNotif.value = true;
  try {
    const list = await getNotifications();
    notifications.value = Array.isArray(list) ? list : [];
  } catch (e) {
    notifications.value = [];
  } finally {
    loadingNotif.value = false;
  }
}

function onMore() {
  uni.showToast({ title: '更多通知开发中', icon: 'none' });
}

function onNotif(n: AgentNotification) {
  uni.showModal({
    title: n.title,
    content: n.content || '（无详细内容）',
    showCancel: false,
  });
}

onMounted(() => {
  loadAgentInfo();
  loadNotifications();
});

// Tab 页再次显示时刷新通知
onShow(() => {
  if (agentInfo.value.agentCode) {
    loadNotifications();
  }
});
</script>

<style lang="scss" scoped>
.page {
  padding: 30rpx 24rpx 60rpx;
  min-height: 100vh;
  background: #f5f7fa;
}

/* 欢迎卡 */
.welcome-card {
  background: linear-gradient(135deg, #409eff 0%, #5f8afe 100%);
  border-radius: 16rpx;
  padding: 36rpx 32rpx;
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(64, 158, 255, 0.25);
}
.welcome-row {
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
.welcome-text {
  margin-left: 24rpx;
  flex: 1;
}
.greet {
  font-size: 36rpx;
  font-weight: bold;
}
.channel {
  font-size: 26rpx;
  margin-top: 8rpx;
  opacity: 0.9;
}
.welcome-meta {
  margin-top: 24rpx;
  font-size: 24rpx;
  opacity: 0.92;
}
.meta-item {
  margin-right: 32rpx;
}

/* 宫格 */
.grid {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx 0;
  display: flex;
  flex-wrap: wrap;
}
.grid-item {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12rpx 0;
}
.grid-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.grid-icon-text {
  color: #fff;
  font-size: 40rpx;
  font-weight: bold;
}
.grid-label {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #303133;
}

/* 通知 */
.section {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
}
.section-more {
  font-size: 26rpx;
  color: #409eff;
}
.empty {
  text-align: center;
  color: #909399;
  font-size: 26rpx;
  padding: 60rpx 0;
}
.notif-card {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}
.notif-card:last-child {
  border-bottom: none;
}
.notif-title {
  font-size: 28rpx;
  color: #303133;
  font-weight: 500;
}
.notif-content {
  font-size: 26rpx;
  color: #606266;
  margin-top: 8rpx;
}
.notif-time {
  font-size: 24rpx;
  color: #909399;
  margin-top: 8rpx;
}
</style>
