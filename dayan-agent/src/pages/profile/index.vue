<template>
  <view class="page">
    <!-- 个人信息卡（蓝色渐变，点击进资料查看） -->
    <view class="profile-card dy-clickable" @click="goView">
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
          <view class="channel">手机：{{ maskPhone(profile.phone) }}</view>
        </view>
        <view class="profile-arrow">
          <text class="arrow-text">查看资料</text>
          <text class="arrow-icon">›</text>
        </view>
      </view>
    </view>

    <!-- 快捷入口（白色卡，浮于 hero） -->
    <view class="quick-card">
      <view class="quick-item tint-blue dy-clickable" @click="goNewLead">
        <view class="quick-icon-wrap">
          <DyIconBlock text="线" color="blue" size="md" shape="circle" />
        </view>
        <text class="quick-label">新增线索</text>
      </view>
      <view class="quick-item tint-green dy-clickable" @click="goEquityDepot">
        <view class="quick-icon-wrap">
          <DyIconBlock text="仓" color="green" size="md" shape="circle" />
        </view>
        <text class="quick-label">权益仓库</text>
      </view>
      <view class="quick-item tint-orange dy-clickable" @click="goLearning">
        <view class="quick-icon-wrap">
          <DyIconBlock text="课" color="orange" size="md" shape="circle" />
        </view>
        <text class="quick-label">新增课程</text>
      </view>
    </view>

    <!-- 菜单卡片 -->
    <view class="menu-card">
      <view class="menu-item dy-clickable" @click="onTodo('订单记录')">
        <DyIconBlock text="单" color="blue" size="sm" shape="circle" />
        <text class="menu-label">订单记录</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item dy-clickable" @click="onTodo('分享记录')">
        <DyIconBlock text="享" color="red" size="sm" shape="circle" />
        <text class="menu-label">分享记录</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item dy-clickable" @click="goSettings">
        <DyIconBlock text="设" color="gray" size="sm" shape="circle" />
        <text class="menu-label">系统设置</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item dy-clickable" @click="goAbout">
        <DyIconBlock text="关" color="gray" size="sm" shape="circle" />
        <text class="menu-label">关于大雁</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item menu-danger dy-clickable" @click="onLogout">
        <DyIconBlock text="退" color="red" size="sm" shape="circle" />
        <text class="menu-label danger">退出登录</text>
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
import { logoutApi } from '@/api/auth';
import type { AgentProfile } from '@/types';
import { AGENT_LEVEL_MAP } from '@/types';
import { formatFileUrl } from '@/utils/file';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';

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

/**
 * 渠道简称：channelName（后端=channel_info.short_name）→ channelCode → store
 * 注意：不再优先 companyName（那是代理人所属公司全称，偏长）。
 */
const channelText = computed(() => {
  return (
    profile.value.channelName ||
    profile.value.channelCode ||
    userStore.channelCode ||
    '-'
  );
});

/** 等级文案（1 普通 / 2 银牌 / 3 金牌 / 4 钻石） */
const levelText = computed(() => {
  const level = profile.value.agentLevel;
  return level ? AGENT_LEVEL_MAP[level] || '' : '';
});

/** 手机号脱敏：11 位手机号中间四位打码，其余原样 */
function maskPhone(p?: string): string {
  if (!p) return '-';
  return p.length === 11 ? `${p.slice(0, 3)}****${p.slice(7)}` : p;
}

function goView() {
  uni.navigateTo({ url: '/pages/profile/view' });
}
function goNewLead() {
  uni.navigateTo({ url: '/pages/acquisition/lead/form' });
}
function goEquityDepot() {
  uni.showToast({ title: '权益仓库开发中', icon: 'none' });
}
function goLearning() {
  uni.switchTab({ url: '/pages/learning/index' });
}
function goSettings() {
  uni.navigateTo({ url: '/pages/profile/settings' });
}
function goAbout() {
  uni.navigateTo({ url: '/pages/profile/about' });
}
function onTodo(name: string) {
  uni.showToast({ title: `${name}（开发中）`, icon: 'none' });
}
function onLogout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await logoutApi();
        } catch {
          // 忽略登出接口失败，本地清登录态即可
        }
        userStore.logout();
        uni.reLaunch({ url: '/pages/login/index' });
      }
    },
  });
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

// 仅用 onShow（避免 onMounted+onShow 双请求陷阱）
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
  overflow: hidden;
  background: $gradient-blue;
  border-radius: $radius-lg;
  padding: 40rpx $spacing-lg;
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(64, 158, 255, 0.25);
  touch-action: manipulation;
}
/* hero 右上角径向高光，增加质感 */
.profile-card::before {
  content: '';
  position: absolute;
  top: -60rpx;
  right: -40rpx;
  width: 240rpx;
  height: 240rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.18), transparent 70%);
  pointer-events: none;
}
.profile-arrow {
  display: flex;
  align-items: center;
  gap: 4rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 24rpx;
  padding: 10rpx 20rpx;
  flex-shrink: 0;
  align-self: center;
  transition: background $transition-fast;
}
.profile-card:active .profile-arrow {
  background: rgba(255, 255, 255, 0.32);
}
.arrow-text {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.95);
}
.arrow-icon {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.95);
}
.profile-row {
  display: flex;
  align-items: center;
}

/* 头像 */
.avatar-wrap {
  width: 120rpx;
  height: 120rpx;
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

/* 快捷入口卡（正常间距，不再上浮盖住 hero） */
.quick-card {
  display: flex;
  align-items: center;
  background: $bg-card;
  margin: $spacing-md 0 0;
  border-radius: $radius-lg;
  padding: 36rpx 0;
  position: relative;
  box-shadow: $shadow-card;
}
.quick-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  touch-action: manipulation;
  transition: transform $transition-fast, opacity $transition-fast;
}
.quick-item:active {
  transform: scale(0.96);
  opacity: 0.85;
}
/* 图标底座：浅色 tint 圆，app-icon 质感 */
.quick-icon-wrap {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform $transition-fast;
}
.tint-blue .quick-icon-wrap {
  background: rgba(64, 158, 255, 0.12);
}
.tint-green .quick-icon-wrap {
  background: rgba(25, 190, 107, 0.12);
}
.tint-orange .quick-icon-wrap {
  background: rgba(255, 153, 0, 0.12);
}
.quick-label {
  font-size: 24rpx;
  color: $text-regular;
  margin-top: 16rpx;
}

/* 菜单卡片 */
.menu-card {
  margin-top: $spacing-lg;
  background: $bg-card;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-card;
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx;
  border-bottom: 1rpx solid $border-light;
  touch-action: manipulation;
  transition: background $transition-fast;

  &:active {
    background: $bg-page;
  }
  &:last-child {
    border-bottom: none;
  }
}
.menu-label {
  margin-left: $spacing-md;
  font-size: 30rpx;
  color: $text-primary;
  flex: 1;

  &.danger {
    color: $brand-error;
  }
}
.menu-arrow {
  font-size: 36rpx;
  color: $text-placeholder;
}
.menu-danger {
  &:active {
    background: $brand-error-light;
  }
}
</style>
