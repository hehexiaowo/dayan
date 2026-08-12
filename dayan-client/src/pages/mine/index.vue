<template>
  <view class="mine">
    <!-- 资料头（绿色品牌渐变） -->
    <view class="profile-header" @click="goProfile">
      <view class="avatar">
        <image v-if="userStore.userInfo.avatar" class="avatar-img" :src="userStore.userInfo.avatar" mode="aspectFill" />
        <view v-else class="avatar-ph">{{ avatarText }}</view>
      </view>
      <view class="profile-info">
        <text class="real-name">{{ displayName }}</text>
        <view v-if="userStore.channelCode" class="channel-tag">
          <text class="channel-tag-text">渠道：{{ userStore.channelCode }}</text>
        </view>
      </view>
      <view class="profile-arrow">
        <text class="arrow-text">查看资料</text>
        <text class="arrow-icon">›</text>
      </view>
    </view>

    <!-- 统计行（白色卡片浮于头部） -->
    <view class="stats-card">
      <view class="stat-item" @click="goEquity">
        <text class="stat-num">{{ equityCount }}</text>
        <text class="stat-label">我的权益</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item" @click="goService">
        <text class="stat-num">{{ activeServiceCount }}</text>
        <text class="stat-label">进行中服务</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item" @click="goUsePersons">
        <text class="stat-num">{{ usePersonCount }}</text>
        <text class="stat-label">权益人数</text>
      </view>
    </view>

    <!-- 菜单 -->
    <view class="menu-card">
      <view class="menu-item" @click="goOrders">
        <view class="menu-icon icon-cyan"><text class="icon-char">单</text></view>
        <text class="menu-text">我的订单</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="onTodo('帮助中心')">
        <view class="menu-icon icon-teal"><text class="icon-char">帮</text></view>
        <text class="menu-text">帮助中心</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="onAbout">
        <view class="menu-icon icon-gray"><text class="icon-char">关</text></view>
        <text class="menu-text">关于我们</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item menu-danger" @click="onLogout">
        <view class="menu-icon icon-red"><text class="icon-char">退</text></view>
        <text class="menu-text danger">退出登录</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import { logoutApi } from '@/api/auth';
import { getEquities, suggestUsePersons } from '@/api/equity';
import { getServices } from '@/api/service';

const userStore = useUserStore();
const equityCount = ref(0);
const activeServiceCount = ref(0);
const usePersonCount = ref(0);

const displayName = computed(() => userStore.userInfo.realName || '尊贵客户');
const avatarText = computed(() => (displayName.value || '客').charAt(0));

/** 加载统计：权益总数 / 进行中服务 / 权益人数 */
async function loadStats() {
  if (!userStore.isLoggedIn) return;
  // 权益总数（size=1 只为拿 total）
  try {
    const eq = await getEquities({ current: 1, size: 1 });
    equityCount.value = eq.total ?? 0;
  } catch {
    equityCount.value = 0;
  }
  // 进行中服务（状态 1~5 算进行中）
  try {
    const sv = await getServices({ current: 1, size: 50 });
    activeServiceCount.value = (sv.records ?? []).filter(
      (s) => [1, 2, 3, 4, 5].includes(s.sessionStatus),
    ).length;
  } catch {
    activeServiceCount.value = 0;
  }
  // 权益人数（跨权益去重的常用权益人）
  try {
    const persons = await suggestUsePersons();
    usePersonCount.value = persons?.length ?? 0;
  } catch {
    usePersonCount.value = 0;
  }
}

function goProfile() {
  uni.navigateTo({ url: '/pages/mine/view' });
}
function goOrders() {
  uni.navigateTo({ url: '/pages/mine/orders' });
}
function goEquity() {
  uni.navigateTo({ url: '/pages/equity/list' });
}
function goService() {
  uni.switchTab({ url: '/pages/service/index' });
}
function goUsePersons() {
  uni.navigateTo({ url: '/pages/equity/use-persons/index' });
}
function onTodo(name: string) {
  uni.showToast({ title: `${name}功能即将开放`, icon: 'none' });
}
function onAbout() {
  uni.showToast({ title: '大雁养老 客户端 v1.0.0', icon: 'none' });
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

// 仅用 onShow（避免 onMounted+onShow 双请求陷阱）
onShow(loadStats);
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.mine {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 140rpx;
}

/* 资料头 */
.profile-header {
  background: $gradient-brand;
  padding: 60rpx $spacing-lg 80rpx;
  display: flex;
  align-items: center;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  overflow: hidden;
  border: 4rpx solid rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.avatar-ph {
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.3);
  color: #fff;
  font-size: 48rpx;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-info {
  margin-left: $spacing-md;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.real-name {
  color: #fff;
  font-size: 38rpx;
  font-weight: bold;
}

.channel-tag {
  margin-top: 10rpx;
  display: inline-flex;
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.2);
  border: 2rpx solid rgba(255, 255, 255, 0.4);
  border-radius: 20rpx;
  padding: 4rpx 16rpx;
}

.channel-tag-text {
  font-size: 22rpx;
  color: #fff;
}

.profile-arrow {
  display: flex;
  align-items: center;
  gap: 4rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 24rpx;
  padding: 10rpx 20rpx;
}

.arrow-text {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.95);
}

.arrow-icon {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.95);
}

/* 统计行（浮于头部） */
.stats-card {
  display: flex;
  align-items: center;
  background: $bg-card;
  margin: -40rpx $spacing-lg 0;
  border-radius: $radius-lg;
  padding: 36rpx 0;
  position: relative;
  box-shadow: $shadow-card;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 44rpx;
  font-weight: bold;
  color: $brand-primary;
}

.stat-label {
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: 8rpx;
}

.stat-divider {
  width: 2rpx;
  height: 64rpx;
  background: $border-base;
}

/* 菜单卡片 */
.menu-card {
  background: $bg-card;
  margin: $spacing-md $spacing-lg 0;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-card;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx $spacing-lg;
  border-bottom: 2rpx solid $border-light;
  transition: background $transition-fast;

  &:active {
    background: $bg-page;
  }

  &:last-child {
    border-bottom: none;
  }
}

.menu-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: $spacing-md;
  flex-shrink: 0;
}

.icon-char {
  font-size: 30rpx;
  font-weight: bold;
  color: #fff;
}

/* 彩色圆形字符块（与 agent DyIconBlock 风格一致） */
.icon-green {
  background: $gradient-brand;
}

.icon-orange {
  background: $gradient-orange;
}

.icon-blue {
  background: $network-blue;
}

.icon-cyan {
  background: linear-gradient(135deg, #36cfc9, #5cdbd3);
}

.icon-teal {
  background: $gradient-green;
}

.icon-gray {
  background: $gradient-gray;
}

.icon-red {
  background: $gradient-red;
}

.menu-text {
  flex: 1;
  font-size: 30rpx;
  color: $text-primary;

  &.danger {
    color: $brand-error;
  }
}

.menu-arrow {
  color: $text-placeholder;
  font-size: 32rpx;
}

.menu-danger {
  &:active {
    background: $brand-error-light;
  }
}
</style>
