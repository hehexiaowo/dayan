<template>
  <view class="mine">
    <!-- 个人信息卡 -->
    <view class="profile">
      <view class="avatar">
        <image v-if="userStore.userInfo.avatar" class="avatar-img" :src="userStore.userInfo.avatar" mode="aspectFill" />
        <view v-else class="avatar-ph">{{ avatarText }}</view>
      </view>
      <view class="profile-info">
        <text class="real-name">{{ displayName }}</text>
        <text class="mobile">{{ maskedMobile }}</text>
      </view>
      <view v-if="userStore.channelCode" class="channel-tag">{{ userStore.channelCode }}</view>
    </view>

    <!-- 统计概览（真实数据） -->
    <view class="stats">
      <view class="stat-item" @click="goEquity">
        <text class="stat-num">{{ equityCount }}</text>
        <text class="stat-label">我的权益</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item" @click="goService">
        <text class="stat-num">{{ serviceCount }}</text>
        <text class="stat-label">服务会话</text>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="menu-group">
      <view class="menu-item" @click="goEquity">
        <text class="menu-icon" style="color: #67C23A">益</text>
        <text class="menu-text">我的权益</text>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-item" @click="goService">
        <text class="menu-icon" style="color: #e6a23c">务</text>
        <text class="menu-text">我的服务</text>
        <text class="menu-arrow">></text>
      </view>
    </view>

    <view class="menu-group">
      <view class="menu-item" @click="onAbout">
        <text class="menu-icon" style="color: #909399">关</text>
        <text class="menu-text">关于我们</text>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-item menu-danger" @click="onLogout">
        <text class="menu-icon" style="color: #f56c6c">退</text>
        <text class="menu-text danger">退出登录</text>
        <text class="menu-arrow">></text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import { logoutApi } from '@/api/auth';
import { getEquities } from '@/api/equity';
import { getServices } from '@/api/service';

const userStore = useUserStore();
const equityCount = ref(0);
const serviceCount = ref(0);

const displayName = computed(() => userStore.userInfo.realName || '尊贵客户');
const avatarText = computed(() => (displayName.value || '客').charAt(0));
const maskedMobile = computed(() => {
  // accountCode 存的是 clientCode，手机号未单独存；优先用 channelCode 标签展示渠道
  const code = userStore.userInfo.accountCode || '';
  if (!code) return '未绑定手机';
  const s = String(code);
  // clientCode 形如 CL0000000001，不脱敏直接展示编码
  return s;
});

async function loadStats() {
  if (!userStore.isLoggedIn) return;
  try {
    const eq = await getEquities({ current: 1, size: 1 });
    equityCount.value = eq.total ?? 0;
  } catch (e) {
    equityCount.value = 0;
  }
  try {
    const sv = await getServices({ current: 1, size: 1 });
    serviceCount.value = sv.total ?? 0;
  } catch (e) {
    serviceCount.value = 0;
  }
}

function goEquity() {
  uni.navigateTo({ url: '/pages/equity/list' });
}
function goService() {
  uni.switchTab({ url: '/pages/service/index' });
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
        } catch (e) {
          // 忽略登出接口失败，本地清登录态即可
        }
        userStore.logout();
        uni.reLaunch({ url: '/pages/login/index' });
      }
    },
  });
}

onShow(loadStats);
</script>

<style lang="scss" scoped>
.mine {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 40rpx;
}

/* 个人信息卡 */
.profile {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #67C23A 0%, #4eaf2a 100%);
  padding: 50rpx 30rpx 60rpx;
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
  margin-left: 24rpx;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.real-name {
  color: #fff;
  font-size: 36rpx;
  font-weight: bold;
}
.mobile {
  color: rgba(255, 255, 255, 0.85);
  font-size: 26rpx;
  margin-top: 8rpx;
}
.channel-tag {
  color: #fff;
  font-size: 22rpx;
  border: 1px solid rgba(255, 255, 255, 0.6);
  padding: 4rpx 16rpx;
  border-radius: 16rpx;
}

/* 统计 */
.stats {
  display: flex;
  align-items: center;
  background: #fff;
  margin: -30rpx 24rpx 0;
  border-radius: 16rpx;
  padding: 30rpx 0;
  position: relative;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-num {
  font-size: 40rpx;
  font-weight: bold;
  color: #303133;
}
.stat-label {
  font-size: 24rpx;
  color: #909399;
  margin-top: 6rpx;
}
.stat-divider {
  width: 1px;
  height: 60rpx;
  background: #ebeef5;
}

/* 菜单 */
.menu-group {
  background: #fff;
  margin: 20rpx 24rpx 0;
  border-radius: 16rpx;
  overflow: hidden;
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  border-bottom: 1px solid #f5f5f5;
  &:last-child {
    border-bottom: none;
  }
}
.menu-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #f5f6f8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: bold;
  margin-right: 20rpx;
}
.menu-text {
  flex: 1;
  font-size: 28rpx;
  color: #303133;
  &.danger {
    color: #f56c6c;
  }
}
.menu-arrow {
  color: #c0c4cc;
  font-size: 28rpx;
}
.menu-danger {
  &:active {
    background: #fef0f0;
  }
}
</style>
