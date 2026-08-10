<template>
  <view class="page">
    <view class="menu-card">
      <view class="menu-item dy-clickable" @click="onLogout">
        <DyIconBlock text="退" color="red" size="sm" shape="circle" />
        <text class="menu-label">退出登录</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item">
        <DyIconBlock text="关" color="gray" size="sm" shape="circle" />
        <text class="menu-label">关于大雁养老</text>
        <text class="menu-value">v1.0.0</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';

const userStore = useUserStore();

function onLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确认退出当前账号？',
    confirmColor: '#fa3534',
    success: (res) => {
      if (!res.confirm) return;
      userStore.logout();
      uni.reLaunch({ url: '/pages/login/index' });
    },
  });
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  padding: $spacing-md;
  min-height: 100vh;
  background: $bg-page;
}
.menu-card {
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
  &:last-child { border-bottom: none; }
}
.menu-label {
  margin-left: $spacing-md;
  font-size: 30rpx;
  color: $text-primary;
  flex: 1;
}
.menu-arrow { font-size: 36rpx; color: $text-placeholder; }
.menu-value { font-size: 26rpx; color: $text-secondary; }
</style>
