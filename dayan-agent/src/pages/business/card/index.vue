<template>
  <view class="page">
    <!-- 头部 -->
    <view class="header">
      <text class="header-title">展业贺卡</text>
      <text class="header-subtitle">节日贺卡 · 一键制作</text>
    </view>

    <!-- 占位内容 -->
    <view class="placeholder">
      <DyIconBlock text="卡" color="orange" size="lg" shape="circle" />
      <text class="placeholder-title">展业贺卡</text>
      <text class="placeholder-desc">贺卡制作功能建设中，敬请期待</text>
    </view>

    <!-- 底部提示 -->
    <view class="bottom-tip">
      <text class="tip-text">更多展业工具持续上线中</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
</script>

<style lang="scss" scoped>

.page {
  min-height: 100vh;
  background: $bg-page;
}

/* 头部 */
.header {
  background: $gradient-blue;
  padding: $spacing-xl $spacing-lg;
  display: flex;
  flex-direction: column;
}
.header-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
}
.header-subtitle {
  margin-top: $spacing-xs;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* 占位内容 */
.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx $spacing-lg 0;
}
.placeholder-title {
  margin-top: $spacing-lg;
  font-size: 36rpx;
  font-weight: bold;
  color: $text-primary;
}
.placeholder-desc {
  margin-top: $spacing-sm;
  font-size: 26rpx;
  color: $text-secondary;
}

/* 底部提示 */
.bottom-tip {
  padding: $spacing-xl 0;
  display: flex;
  justify-content: center;
}
.tip-text {
  font-size: 24rpx;
  color: $text-placeholder;
}
</style>
