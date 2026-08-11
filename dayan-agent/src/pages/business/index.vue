<template>
  <view class="page dy-safe-bottom">
    <!-- 渐变 header -->
    <view class="header">
      <text class="header-title">展业工具</text>
      <text class="header-sub">查询机构、采购权益、报名活动</text>
    </view>

    <!-- 找机构 -->
    <view class="card dy-clickable" @click="onSection('park')">
      <DyIconBlock text="机" color="blue" size="lg" />
      <view class="card-body">
        <view class="card-title">找机构</view>
        <view class="card-desc">旅居养老 / 活力长居 / 照护长居</view>
      </view>
      <text class="card-arrow">›</text>
    </view>

    <!-- 大雁商城 -->
    <view class="card dy-clickable" @click="onSection('mall')">
      <DyIconBlock text="商" color="green" size="lg" />
      <view class="card-body">
        <view class="card-title">大雁商城</view>
        <view class="card-desc">权益商品采购 · 个人专属</view>
      </view>
      <text class="card-arrow">›</text>
    </view>

    <!-- 场景活动 -->
    <view class="card dy-clickable" @click="onSection('activity')">
      <DyIconBlock text="活" color="orange" size="lg" />
      <view class="card-body">
        <view class="card-title">场景活动</view>
        <view class="card-desc">参观体验 / 健康讲座 / 节日活动</view>
      </view>
      <view class="card-tag">即将上线</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';

function onSection(type: string) {
  if (type === 'park') {
    uni.navigateTo({ url: '/pages/business/park/categories' });
    return;
  }
  if (type === 'mall') {
    uni.navigateTo({ url: '/pages/business/mall/index' });
    return;
  }
  uni.showToast({ title: '场景活动（开发中）', icon: 'none' });
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  padding: $spacing-md $spacing-md 0;
  min-height: 100vh;
  background: $bg-page;
}

/* 渐变 header */
.header {
  background: $gradient-blue;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  margin-bottom: $spacing-md;
}
.header-title {
  display: block;
  font-size: 38rpx;
  font-weight: bold;
  color: #fff;
}
.header-sub {
  display: block;
  margin-top: $spacing-sm;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

/* 功能卡片 */
.card {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  display: flex;
  align-items: center;
  box-shadow: $shadow-card;
}
.card-body {
  margin-left: $spacing-md;
  flex: 1;
}
.card-title {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}
.card-desc {
  margin-top: $spacing-xs;
  font-size: 24rpx;
  color: $text-secondary;
}
.card-tag {
  font-size: 22rpx;
  color: $brand-warning;
  background: $brand-warning-light;
  padding: 6rpx 18rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
}
.card-arrow {
  font-size: 40rpx;
  color: $text-placeholder;
  flex-shrink: 0;
}
</style>
