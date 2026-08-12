<template>
  <view class="page">
    <!-- 骨架屏 -->
    <view v-if="loading" class="skeleton-wrap">
      <DySkeleton :rows="1" avatar card />
      <DySkeleton :rows="4" card />
      <DySkeleton :rows="3" card />
    </view>

    <template v-else>
      <!-- 头部 hero（绿色品牌渐变） -->
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
              <text v-if="profile.isVip === 1" class="vip-badge">VIP</text>
              <text v-else-if="levelText" class="level-badge">{{ levelText }}</text>
            </view>
            <text class="hero-sub">{{ channelText }}</text>
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
        <view class="info-row">
          <text class="info-label">生日</text>
          <text class="info-value">{{ profile.birthday || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">身份证号</text>
          <text class="info-value">{{ profile.idCard || '-' }}</text>
        </view>
      </view>

      <!-- 账户信息 -->
      <view class="section-title">账户信息</view>
      <view class="info-card">
        <view class="info-row">
          <text class="info-label">客户号</text>
          <text class="info-value">{{ profile.clientCode || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">所属渠道</text>
          <text class="info-value">{{ channelText }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">客户等级</text>
          <text class="info-value">{{ levelText || '普通客户' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">会员身份</text>
          <text class="info-value">{{ profile.isVip === 1 ? 'VIP 客户' : '普通会员' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">注册时间</text>
          <text class="info-value">{{ formatTime(profile.registerTime) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">最近登录</text>
          <text class="info-value">{{ formatTime(profile.lastLoginTime) }}</text>
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

      <!-- 资产与统计 -->
      <view class="section-title">资产与统计</view>
      <view class="info-card">
        <view class="info-row">
          <text class="info-label">持有权益</text>
          <text class="info-value">{{ profile.equityCount ?? 0 }} 份</text>
        </view>
        <view class="info-row">
          <text class="info-label">已用权益</text>
          <text class="info-value">{{ profile.usedEquityCount ?? 0 }} 份</text>
        </view>
        <view class="info-row">
          <text class="info-label">累计服务</text>
          <text class="info-value">{{ profile.serviceCount ?? 0 }} 次</text>
        </view>
        <view class="info-row">
          <text class="info-label">累计消费</text>
          <text class="info-value">¥{{ formatAmount(profile.totalOrderAmount) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">最近服务</text>
          <text class="info-value">{{ formatTime(profile.lastServiceTime) }}</text>
        </view>
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
import { getProfileApi } from '@/api/auth';
import type { ClientProfile } from '@/types';
import { formatFileUrl } from '@/utils/file';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';

const userStore = useUserStore();
const profile = ref<Partial<ClientProfile>>({});
const loading = ref(true);

const displayName = computed(() => {
  return profile.value.fullName || userStore.userInfo.realName || profile.value.clientCode || '尊贵客户';
});

const avatarChar = computed(() => {
  const name = displayName.value;
  return name ? name.charAt(0) : '客';
});

const avatarUrl = computed(() => formatFileUrl(profile.value.avatar));

const channelText = computed(() => {
  return profile.value.channelName || profile.value.channelCode || userStore.channelCode || '-';
});

const levelText = computed(() => {
  const level = profile.value.clientLevel;
  if (!level || level <= 0) return '';
  return `Lv.${level}`;
});

const regionText = computed(() => {
  const p = profile.value;
  const parts = [p.provinceName, p.cityName, p.districtName].filter(Boolean);
  return parts.join(' ');
});

function genderText(g?: number): string {
  switch (g) {
    case 1:
      return '男';
    case 2:
      return '女';
    default:
      return '保密';
  }
}

function formatTime(t?: number | string): string {
  if (!t) return '-';
  // 后端 LocalDateTime 序列化为数字或 ISO 字符串，统一截取到分钟
  const s = String(t).replace('T', ' ');
  return s.length > 16 ? s.slice(0, 16) : s;
}

function formatAmount(v?: number): string {
  if (v == null) return '0.00';
  return Number(v).toFixed(2);
}

function goEdit() {
  uni.navigateTo({ url: '/pages/mine/edit' });
}

async function loadProfile() {
  loading.value = true;
  try {
    profile.value = (await getProfileApi()) || {};
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
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

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
  background: $gradient-brand;
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
.vip-badge {
  padding: 2rpx 16rpx;
  font-size: 20rpx;
  line-height: 1.6;
  font-weight: bold;
  color: #ff9900;
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
  background: $gradient-brand;
  box-shadow: 0 8rpx 20rpx rgba(103, 194, 58, 0.3);

  &::after {
    border: none;
  }
}
</style>
