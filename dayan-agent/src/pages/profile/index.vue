<template>
  <view class="page">
    <!-- 个人信息卡（渐变，可点击进资料编辑） -->
    <view class="profile-card dy-clickable" @click="goEdit">
      <text class="edit-hint">编辑资料 ›</text>
      <view class="profile-row">
        <view class="avatar-wrap">
          <image
            v-if="avatarUrl"
            class="avatar"
            :src="avatarUrl"
            mode="aspectFill"
          />
          <view v-else class="avatar-fallback">
            <text class="avatar-text">{{ avatarChar }}</text>
          </view>
        </view>
        <view class="profile-text">
          <view class="name-row">
            <text class="name">{{ displayName }}</text>
            <text v-if="levelText" class="level-badge">{{ levelText }}</text>
            <text v-if="profile.isCertified === 1" class="cert-badge">已认证</text>
          </view>
          <view class="channel">渠道：{{ channelText }}</view>
        </view>
      </view>
    </view>

    <!-- 账号信息分组 -->
    <view class="info-card">
      <view class="info-title">账号信息</view>
      <view class="info-row">
        <text class="info-label">手机号</text>
        <text class="info-value">{{ maskPhone(profile.phone) }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">工号</text>
        <text class="info-value">{{ profile.employeeNo || profile.agentCode || storeAgentCode || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">执业证号</text>
        <text class="info-value">{{ profile.licenseNo || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">职位</text>
        <text class="info-value">{{ profile.position || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">地区</text>
        <text class="info-value">{{ regionText }}</text>
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
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import { getProfile } from '@/api/agent';
import type { AgentProfile } from '@/types';
import { AGENT_LEVEL_MAP } from '@/types';
import { formatFileUrl } from '@/utils/file';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';

type IconColor = 'blue' | 'green' | 'orange' | 'red' | 'gray';

const userStore = useUserStore();

const profile = ref<Partial<AgentProfile>>({});

/** 姓名：接口 fullName → store realName → agentCode → store accountCode → 兜底 */
const displayName = computed(() => {
  return (
    profile.value.fullName ||
    (userStore.userInfo && userStore.userInfo.realName) ||
    profile.value.agentCode ||
    (userStore.userInfo && userStore.userInfo.accountCode) ||
    '代理人'
  );
});

/** 头像首字（无图片时显示） */
const avatarChar = computed(() => {
  const name = displayName.value;
  return name ? name.charAt(0) : '代';
});

/** 头像 URL（OSS key 转可访问地址） */
const avatarUrl = computed(() => formatFileUrl(profile.value.avatar));

/** 渠道行：公司名 → 渠道名 → 渠道编码 */
const channelText = computed(() => {
  return (
    profile.value.companyName ||
    profile.value.channelName ||
    profile.value.channelCode ||
    userStore.channelCode ||
    '-'
  );
});

const storeAgentCode = computed(
  () => (userStore.userInfo && userStore.userInfo.accountCode) || '',
);

/** 等级文案（1 普通 / 2 银牌 / 3 金牌 / 4 钻石） */
const levelText = computed(() => {
  const level = profile.value.agentLevel;
  return level ? AGENT_LEVEL_MAP[level] || '' : '';
});

/** 地区：省市区空格拼接，有详细地址追加，全空显示 '-' */
const regionText = computed(() => {
  const p = profile.value;
  const parts = [p.provinceName, p.cityName, p.districtName].filter(Boolean);
  let text = parts.join(' ');
  if (p.address) {
    text = text ? `${text} ${p.address}` : p.address;
  }
  return text || '-';
});

/** 手机号脱敏：11 位手机号中间四位打码，其余原样 */
function maskPhone(p?: string): string {
  if (!p) return '-';
  return p.length === 11 ? `${p.slice(0, 3)}****${p.slice(7)}` : p;
}

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
  if (item.key === 'settings') {
    uni.navigateTo({ url: '/pages/profile/settings' });
    return;
  }
  const tips: Record<string, string> = {
    stats: '经营数据（Inc 6 上线）',
    equity: '我的权益（Inc 6 上线）',
    orders: '我的订单（Inc 6 上线）',
    shares: '分享记录（Inc 4 上线）',
  };
  uni.showToast({ title: tips[item.key] || '开发中', icon: 'none' });
}

function goEdit() {
  uni.navigateTo({ url: '/pages/profile/edit' });
}

async function loadProfile() {
  try {
    const data = await getProfile();
    profile.value = data || {};
  } catch (e) {
    // 加载失败降级：保留 store 的 realName/accountCode 兜底显示
    profile.value = {};
  }
}

onShow(() => {
  loadProfile();
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
  position: relative;
  background: $gradient-blue;
  border-radius: $radius-lg;
  padding: 40rpx $spacing-lg;
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(64, 158, 255, 0.25);
}
.edit-hint {
  position: absolute;
  top: $spacing-md;
  right: $spacing-lg;
  font-size: 24rpx;
  opacity: 0.85;
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
  min-width: 0;
}
.name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.name {
  font-size: 38rpx;
  font-weight: bold;
}
.level-badge {
  margin-left: $spacing-sm;
  padding: 2rpx 16rpx;
  font-size: 20rpx;
  line-height: 1.6;
  color: #fff;
  background: rgba(255, 255, 255, 0.22);
  border-radius: 999rpx;
}
.cert-badge {
  margin-left: $spacing-sm;
  padding: 2rpx 16rpx;
  font-size: 20rpx;
  line-height: 1.6;
  color: $brand-success;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 999rpx;
}
.channel {
  font-size: 26rpx;
  margin-top: $spacing-xs;
  opacity: 0.9;
}

/* 账号信息分组 */
.info-card {
  margin-top: $spacing-md;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md 28rpx;
  box-shadow: $shadow-card;
}
.info-title {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
  padding-bottom: $spacing-sm;
}
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid $border-light;
}
.info-row:last-child {
  border-bottom: none;
}
.info-label {
  font-size: 28rpx;
  color: $text-secondary;
  flex-shrink: 0;
}
.info-value {
  font-size: 28rpx;
  color: $text-primary;
  margin-left: $spacing-md;
  text-align: right;
  flex: 1;
  word-break: break-all;
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
