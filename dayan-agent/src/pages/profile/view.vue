<template>
  <view class="page">
    <!-- 骨架屏 -->
    <view v-if="loading" class="skeleton-wrap">
      <DySkeleton :rows="1" avatar card />
      <DySkeleton :rows="4" card />
      <DySkeleton :rows="3" card />
    </view>

    <template v-else>
      <!-- 头部 -->
      <view class="hero">
        <view class="hero-row">
          <view class="avatar-wrap">
            <image v-if="avatarUrl" class="avatar" :src="avatarUrl" mode="aspectFill" />
            <view v-else class="avatar-fallback">
              <text class="avatar-text">{{ avatarChar }}</text>
            </view>
          </view>
          <view class="hero-info">
            <view class="name-row">
              <text class="name">{{ displayName }}</text>
              <text v-if="levelText" class="level-badge">{{ levelText }}</text>
              <text v-if="profile.isCertified === 1" class="cert-badge">已认证</text>
            </view>
            <text class="hero-sub">渠道：{{ channelText }}</text>
          </view>
        </view>
      </view>

      <!-- 基本信息 -->
      <view class="section-title">基本信息</view>
      <view class="info-card">
        <view class="info-row">
          <text class="info-label">姓名</text>
          <text class="info-value">{{ profile.fullName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">性别</text>
          <text class="info-value">{{ genderText(profile.gender) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">手机号</text>
          <text class="info-value">{{ profile.phone || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">邮箱</text>
          <text class="info-value">{{ profile.email || '-' }}</text>
        </view>
      </view>

      <!-- 组织信息 -->
      <view class="section-title">组织信息</view>
      <view class="info-card">
        <view class="info-row">
          <text class="info-label">渠道</text>
          <text class="info-value">{{ profile.channelName || profile.channelCode || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">公司</text>
          <text class="info-value">{{ profile.companyName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">分公司</text>
          <text class="info-value">{{ profile.branchName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">部门</text>
          <text class="info-value">{{ profile.department || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">职位</text>
          <text class="info-value">{{ profile.position || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">工号</text>
          <text class="info-value">{{ profile.employeeNo || profile.agentCode || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">执业证号</text>
          <text class="info-value">{{ profile.licenseNo || '-' }}</text>
        </view>
      </view>

      <!-- 地区信息 -->
      <view class="section-title">地区信息</view>
      <view class="info-card">
        <view class="info-row">
          <text class="info-label">所在地区</text>
          <text class="info-value">{{ regionText || '-' }}</text>
        </view>
        <view v-if="profile.address" class="info-row">
          <text class="info-label">详细地址</text>
          <text class="info-value">{{ profile.address }}</text>
        </view>
      </view>

      <!-- 服务简介 -->
      <view v-if="profile.serviceIntro" class="section-title">服务简介</view>
      <view v-if="profile.serviceIntro" class="info-card">
        <text class="intro-text">{{ profile.serviceIntro }}</text>
      </view>

      <!-- 编辑按钮 -->
      <button class="edit-btn dy-clickable" @click="goEdit">编辑资料</button>
    </template>
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
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';

const userStore = useUserStore();
const profile = ref<Partial<AgentProfile>>({});
const loading = ref(true);

const displayName = computed(() => {
  return (
    profile.value.fullName ||
    (userStore.userInfo && userStore.userInfo.realName) ||
    profile.value.agentCode ||
    (userStore.userInfo && userStore.userInfo.accountCode) ||
    '代理人'
  );
});

const avatarChar = computed(() => {
  const name = displayName.value;
  return name ? name.charAt(0) : '代';
});

const avatarUrl = computed(() => formatFileUrl(profile.value.avatar));

const channelText = computed(() => {
  return (
    profile.value.companyName ||
    profile.value.channelName ||
    profile.value.channelCode ||
    userStore.channelCode ||
    '-'
  );
});

const levelText = computed(() => {
  const level = profile.value.agentLevel;
  return level ? AGENT_LEVEL_MAP[level] || '' : '';
});

const regionText = computed(() => {
  const p = profile.value;
  const parts = [p.provinceName, p.cityName, p.districtName].filter(Boolean);
  return parts.join(' ');
});

function genderText(g?: number): string {
  switch (g) { case 1: return '男'; case 2: return '女'; default: return '保密'; }
}

function goEdit() {
  uni.navigateTo({ url: '/pages/profile/edit' });
}

async function loadProfile() {
  loading.value = true;
  try {
    profile.value = (await getProfile()) || {};
  } catch {
    profile.value = {};
  } finally {
    loading.value = false;
  }
}

onShow(() => {
  loadProfile();
});
</script>

<style lang="scss" scoped>

.page {
  padding: 0 0 160rpx;
  min-height: 100vh;
  background: $bg-page;
}

.skeleton-wrap {
  padding: $spacing-md;
}

/* 头部 hero */
.hero {
  background: $gradient-blue;
  padding: $spacing-xl $spacing-lg $spacing-lg;
}
.hero-row {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}
.avatar-wrap {
  flex-shrink: 0;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.6);
  background: #fff;
}
.avatar-fallback {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  border: 4rpx solid rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text {
  font-size: 48rpx;
  font-weight: bold;
  color: #fff;
}
.hero-info {
  flex: 1;
  min-width: 0;
}
.name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-xs;
}
.name {
  font-size: 38rpx;
  font-weight: bold;
  color: #fff;
}
.level-badge {
  padding: 2rpx 16rpx;
  font-size: 20rpx;
  line-height: 1.6;
  color: #fff;
  background: rgba(255, 255, 255, 0.22);
  border-radius: 999rpx;
}
.cert-badge {
  padding: 2rpx 16rpx;
  font-size: 20rpx;
  line-height: 1.6;
  color: $brand-success;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 999rpx;
}
.hero-sub {
  display: block;
  margin-top: $spacing-xs;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
}

/* 区块标题 */
.section-title {
  padding: $spacing-lg $spacing-lg $spacing-sm;
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}

/* 信息卡片 */
.info-card {
  background: $bg-card;
  margin: 0 $spacing-lg $spacing-sm;
  border-radius: $radius-md;
  padding: $spacing-sm $spacing-lg;
  box-shadow: $shadow-card;
}
.info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 22rpx 0;
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

/* 服务简介 */
.intro-text {
  font-size: 28rpx;
  color: $text-regular;
  line-height: 1.6;
  padding: $spacing-sm 0;
}

/* 编辑按钮 */
.edit-btn {
  position: fixed;
  left: $spacing-lg;
  right: $spacing-lg;
  bottom: calc(#{$spacing-lg} + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  justify-content: center;
  height: $control-height;
  border-radius: $radius-md;
  font-size: 30rpx;
  font-weight: 500;
  color: #fff;
  background: $gradient-blue;
  box-shadow: 0 8rpx 20rpx rgba(64, 158, 255, 0.3);

  &::after {
    border: none;
  }
}
</style>
