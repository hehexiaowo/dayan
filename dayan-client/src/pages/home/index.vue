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
    <view v-if="loggedIn" class="overview card" @click="goEquityList">
      <view class="ov-item">
        <text class="ov-num">{{ equityCount }}</text>
        <text class="ov-label">我的权益（张）</text>
      </view>
      <view class="ov-divider"></view>
      <view class="ov-item" @click.stop="goServiceList">
        <text class="ov-num">{{ activeServiceCount }}</text>
        <text class="ov-label">进行中服务</text>
      </view>
    </view>

    <!-- 未登录提示 -->
    <view v-else class="overview card login-prompt" @click="goLogin">
      <text class="login-tip">请登录后查看您的权益与服务</text>
      <text class="login-btn">去登录 ></text>
    </view>

    <!-- 主功能宫格 -->
    <view class="grid card">
      <view class="grid-item primary" @click="goActivate">
        <view class="grid-icon icon-activate">★</view>
        <text class="grid-label">激活权益</text>
        <text class="grid-sub">输入激活码</text>
      </view>
      <view class="grid-item" @click="goEquityList">
        <view class="grid-icon icon-equity">益</view>
        <text class="grid-label">我的权益</text>
      </view>
      <view class="grid-item" @click="goServiceList">
        <view class="grid-icon icon-service">务</view>
        <text class="grid-label">我的服务</text>
      </view>
      <view class="grid-item" @click="goPark">
        <view class="grid-icon icon-park">构</view>
        <text class="grid-label">找机构</text>
      </view>
    </view>

    <!-- 权益引导（已登录且无权益时） -->
    <view v-if="loggedIn && equityCount === 0 && !loading" class="guide card" @click="goActivate">
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
.home {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 30rpx;
}

/* 顶部 */
.header {
  background: linear-gradient(135deg, #67C23A 0%, #4eaf2a 100%);
  padding: 40rpx 30rpx 70rpx;
}
.welcome {
  margin-bottom: 30rpx;
  .greeting,
  .name {
    color: #fff;
    font-size: 36rpx;
  }
  .name {
    font-weight: bold;
    margin-left: 8rpx;
  }
}
.banner {
  height: 200rpx;
  border-radius: 16rpx;
  overflow: hidden;
  background: #fff;
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
    background: rgba(103, 194, 58, 0.15);
    color: #67C23A;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    font-weight: bold;
  }
  .banner-text {
    margin-top: 12rpx;
    color: #67C23A;
    font-size: 26rpx;
    font-weight: bold;
  }
  .banner-sub {
    margin-top: 4rpx;
    color: #909399;
    font-size: 22rpx;
  }
}

/* 通用卡片 */
.card {
  background: #fff;
  margin: -40rpx 24rpx 0;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  position: relative;
}

/* 概览卡 */
.overview {
  display: flex;
  align-items: center;
  padding: 36rpx 0;
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
  color: #67C23A;
}
.ov-label {
  font-size: 24rpx;
  color: #909399;
  margin-top: 8rpx;
}
.ov-divider {
  width: 1px;
  height: 60rpx;
  background: #ebeef5;
}
.login-prompt {
  flex-direction: row;
  justify-content: space-between;
  padding: 36rpx 30rpx;
  .login-tip {
    font-size: 28rpx;
    color: #606266;
  }
  .login-btn {
    font-size: 28rpx;
    color: #67C23A;
    font-weight: bold;
  }
}

/* 宫格 */
.grid {
  display: flex;
  flex-wrap: wrap;
  padding: 30rpx 0;
}
.grid-item {
  width: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0;
}
.grid-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 34rpx;
  font-weight: bold;
  margin-bottom: 14rpx;
}
.icon-activate {
  background: linear-gradient(135deg, #67C23A, #4eaf2a);
  box-shadow: 0 6rpx 16rpx rgba(103, 194, 58, 0.4);
  font-size: 40rpx;
}
.icon-equity {
  background: #409eff;
}
.icon-service {
  background: #e6a23c;
}
.icon-park {
  background: #909399;
}
.grid-label {
  font-size: 28rpx;
  color: #303133;
  font-weight: 500;
}
.grid-sub {
  font-size: 20rpx;
  color: #c0c4cc;
  margin-top: 4rpx;
}

/* 引导卡 */
.guide {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 30rpx;
  .guide-title {
    font-size: 30rpx;
    font-weight: bold;
    color: #303133;
  }
  .guide-desc {
    font-size: 24rpx;
    color: #909399;
    margin-top: 12rpx;
  }
}
</style>
