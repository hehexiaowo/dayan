<template>
  <view class="home">
    <!-- 顶部欢迎 + banner -->
    <view class="header">
      <view class="welcome">
        <text class="greeting">{{ greeting }}</text>
        <text class="name">{{ displayName }}</text>
      </view>
      <view class="banner">
        <view class="banner-placeholder">
          <view class="banner-logo">大雁</view>
          <text class="banner-text">大雁养老 · 安心托付</text>
          <text class="banner-sub">您的养老权益管家</text>
        </view>
      </view>
    </view>

    <!-- 权益概览卡（已登录） -->
    <view v-if="loggedIn" class="overview card dy-clickable" @click="goEquityList">
      <!-- 加载骨架 -->
      <template v-if="loading">
        <DySkeleton :rows="1" card class="ov-skeleton" />
      </template>
      <template v-else>
        <view class="ov-item">
          <text class="ov-num">{{ equityCount }}</text>
          <text class="ov-label">我的权益（张）</text>
        </view>
        <view class="ov-divider"></view>
        <view class="ov-item" @click.stop="goServiceList">
          <text class="ov-num">{{ activeServiceCount }}</text>
          <text class="ov-label">进行中服务</text>
        </view>
      </template>
    </view>

    <!-- 未登录提示 -->
    <view v-else class="overview card login-prompt dy-clickable" @click="goLogin">
      <text class="login-tip">请登录后查看您的权益与服务</text>
      <text class="login-btn">去登录 ></text>
    </view>

    <!-- 主功能宫格 -->
    <view class="grid card">
      <view class="grid-item dy-clickable" @click="goActivate">
        <DyIconBlock text="活" color="green" size="md" shape="circle" />
        <text class="grid-label">激活权益</text>
        <text class="grid-sub">输入激活码</text>
      </view>
      <view class="grid-item dy-clickable" @click="goEquityList">
        <DyIconBlock text="益" color="blue" size="md" shape="circle" />
        <text class="grid-label">我的权益</text>
      </view>
      <view class="grid-item dy-clickable" @click="goServiceList">
        <DyIconBlock text="务" color="orange" size="md" shape="circle" />
        <text class="grid-label">我的服务</text>
      </view>
      <view class="grid-item dy-clickable" @click="goPark">
        <DyIconBlock text="网" color="gray" size="md" shape="circle" />
        <text class="grid-label">养老网络</text>
      </view>
    </view>

    <!-- 权益引导（已登录且无权益时） -->
    <view v-if="loggedIn && equityCount === 0 && !loading" class="guide card dy-clickable" @click="goActivate">
      <text class="guide-title">您还没有激活权益</text>
      <text class="guide-desc">手持权益卡？点此输入激活码，开启养老服务</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import { getEquities } from '@/api/equity';
import { getServices } from '@/api/service';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';

const userStore = useUserStore();
const equityCount = ref(0);
const activeServiceCount = ref(0);
const loading = ref(false);

const loggedIn = computed(() => userStore.isLoggedIn);
const displayName = computed(() => userStore.userInfo.realName || '尊贵客户');
const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 6) return '夜深了，';
  if (h < 12) return '早上好，';
  if (h < 14) return '中午好，';
  if (h < 18) return '下午好，';
  return '晚上好，';
});

async function loadOverview() {
  if (!loggedIn.value) return;
  loading.value = true;
  try {
    // 权益总数：size=1 只为拿 total
    const eq = await getEquities({ current: 1, size: 1 });
    equityCount.value = eq.total ?? 0;
  } catch (e) {
    equityCount.value = 0;
  }
  try {
    // 进行中服务：取近期会话按状态过滤（1待分配~5服务中算进行中）
    const sv = await getServices({ current: 1, size: 50 });
    activeServiceCount.value = (sv.records ?? []).filter(
      (s) => [1, 2, 3, 4, 5].includes(s.sessionStatus),
    ).length;
  } catch (e) {
    activeServiceCount.value = 0;
  } finally {
    loading.value = false;
  }
}

function goLogin() {
  uni.reLaunch({ url: '/pages/login/index' });
}
function goActivate() {
  if (!loggedIn.value) return goLogin();
  uni.navigateTo({ url: '/pages/equity/activate' });
}
function goEquityList() {
  if (!loggedIn.value) return goLogin();
  uni.navigateTo({ url: '/pages/equity/list' });
}
function goServiceList() {
  if (!loggedIn.value) return goLogin();
  uni.switchTab({ url: '/pages/service/index' });
}
function goPark() {
  uni.switchTab({ url: '/pages/park/index' });
}

// 仅用 onShow（避免 onMounted+onShow 双请求陷阱）
onShow(loadOverview);
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.home {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 140rpx;
}

/* 顶部 */
.header {
  background: $gradient-brand;
  padding: 40rpx $spacing-lg 70rpx;
}
.welcome {
  margin-bottom: $spacing-md;
  .greeting,
  .name {
    color: #fff;
    font-size: 36rpx;
  }
  .name {
    font-weight: bold;
    margin-left: $spacing-xs;
  }
}
.banner {
  height: 200rpx;
  border-radius: $radius-md;
  overflow: hidden;
  background: $bg-card;
}
.banner-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  .banner-logo {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    background: $brand-primary-light;
    color: $brand-primary;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    font-weight: bold;
  }
  .banner-text {
    margin-top: 12rpx;
    color: $brand-primary;
    font-size: 26rpx;
    font-weight: bold;
  }
  .banner-sub {
    margin-top: 4rpx;
    color: $text-secondary;
    font-size: 22rpx;
  }
}

/* 通用卡片 */
.card {
  background: $bg-card;
  margin: -40rpx $spacing-lg 0;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  position: relative;
}

/* 概览卡 */
.overview {
  display: flex;
  align-items: center;
  padding: 36rpx 0;
}
.ov-skeleton {
  flex: 1;
  margin: 0 $spacing-md;
  box-shadow: none;
}
.ov-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.ov-num {
  font-size: 44rpx;
  font-weight: bold;
  color: $brand-primary;
}
.ov-label {
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: $spacing-xs;
}
.ov-divider {
  width: 2rpx;
  height: 60rpx;
  background: $border-base;
}
.login-prompt {
  flex-direction: row;
  justify-content: space-between;
  padding: 36rpx $spacing-md;
  .login-tip {
    font-size: 28rpx;
    color: $text-regular;
  }
  .login-btn {
    font-size: 28rpx;
    color: $brand-primary;
    font-weight: bold;
  }
}

/* 宫格 */
.grid {
  display: flex;
  flex-wrap: wrap;
  padding: $spacing-md 0;
}
.grid-item {
  width: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0;
}
.grid-label {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  margin-top: 14rpx;
}
.grid-sub {
  font-size: 20rpx;
  color: $text-placeholder;
  margin-top: 4rpx;
}

/* 引导卡 */
.guide {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx $spacing-md;
  .guide-title {
    font-size: 30rpx;
    font-weight: bold;
    color: $text-primary;
  }
  .guide-desc {
    font-size: 24rpx;
    color: $text-secondary;
    margin-top: 12rpx;
  }
}
</style>
