<template>
  <view class="page">
    <!-- 头部 -->
    <view class="hero">
      <view class="logo-circle">
        <text class="logo-text">雁栖康养</text>
      </view>
      <text class="app-name">雁栖康养</text>
      <text class="app-version">客户端 v{{ version }}</text>
    </view>

    <!-- 简介 -->
    <view class="section-title">关于我们</view>
    <view class="card">
      <text class="intro-text">
        雁栖康养客户端，为家属提供一站式养老服务体验。在这里，您可以便捷地查询全国养老机构、预订旅游短居、管理专属权益、跟进服务进度，让长辈的晚年生活更安心、更舒心。
      </text>
    </view>

    <!-- 功能特色 -->
    <view class="section-title">功能特色</view>
    <view class="card">
      <view v-for="(f, i) in features" :key="i" class="feature-row" :class="{ last: i === features.length - 1 }">
        <view class="feature-dot" :style="{ background: f.color }">
          <text class="feature-icon">{{ f.icon }}</text>
        </view>
        <view class="feature-info">
          <text class="feature-title">{{ f.title }}</text>
          <text class="feature-desc">{{ f.desc }}</text>
        </view>
      </view>
    </view>

    <!-- 协议入口 -->
    <view class="section-title">法律信息</view>
    <view class="card">
      <view class="link-row" @click="onAgreement('用户协议')">
        <text class="link-text">用户协议</text>
        <text class="link-arrow">›</text>
      </view>
      <view class="link-row last" @click="onAgreement('隐私政策')">
        <text class="link-text">隐私政策</text>
        <text class="link-arrow">›</text>
      </view>
    </view>

    <view class="copyright">
      <text class="copyright-text">© 2026 雁栖康养</text>
      <text class="copyright-sub">用心守护每一位长辈</text>
    </view>
  </view>
</template>

<script setup lang="ts">
const version = '1.0.0';

const features = [
  { icon: '构', color: 'linear-gradient(135deg,#409eff,#5f8afe)', title: '养老网络', desc: '活力长居 / 照护长居 / 旅游短居，地图下钻精准找机构' },
  { icon: '单', color: 'linear-gradient(135deg,#36cfc9,#5cdbd3)', title: '旅游短居预订', desc: '在线选房型、定日期，便捷预订心仪机构' },
  { icon: '益', color: 'linear-gradient(135deg,#67c23a,#85ce61)', title: '权益管理', desc: '专属权益随时查看，激活使用一目了然' },
  { icon: '服', color: 'linear-gradient(135deg,#ff9900,#ffb84d)', title: '服务跟进', desc: '服务进度透明可视，全程安心可控' },
];

function onAgreement(name: string) {
  uni.showToast({ title: `${name}内容即将上线`, icon: 'none' });
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 80rpx;
}

/* hero */
.hero {
  background: $gradient-brand;
  padding: $spacing-xl $spacing-lg;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.logo-circle {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  border: 4rpx solid rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-text {
  font-size: 44rpx;
  font-weight: bold;
  color: #fff;
}
.app-name {
  margin-top: $spacing-md;
  font-size: 38rpx;
  font-weight: bold;
  color: #fff;
}
.app-version {
  margin-top: $spacing-xs;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
}

.section-title {
  padding: $spacing-lg $spacing-lg $spacing-sm;
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}

.card {
  background: $bg-card;
  margin: 0 $spacing-lg;
  border-radius: $radius-md;
  padding: $spacing-md $spacing-lg;
  box-shadow: $shadow-card;
}

/* 简介 */
.intro-text {
  font-size: 28rpx;
  color: $text-regular;
  line-height: 1.7;
}

/* 功能特色 */
.feature-row {
  display: flex;
  align-items: flex-start;
  padding: 22rpx 0;
  border-bottom: 1rpx solid $border-light;

  &.last {
    border-bottom: none;
  }
}
.feature-dot {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: $spacing-md;
  flex-shrink: 0;
}
.feature-icon {
  font-size: 28rpx;
  font-weight: bold;
  color: #fff;
}
.feature-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.feature-title {
  font-size: 28rpx;
  font-weight: 500;
  color: $text-primary;
}
.feature-desc {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: $text-secondary;
  line-height: 1.5;
}

/* 协议入口 */
.link-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0;
  border-bottom: 1rpx solid $border-light;

  &.last {
    border-bottom: none;
  }

  &:active {
    background: $bg-page;
  }
}
.link-text {
  font-size: 28rpx;
  color: $text-primary;
}
.link-arrow {
  font-size: 32rpx;
  color: $text-placeholder;
}

.copyright {
  margin-top: $spacing-xl;
  text-align: center;
}
.copyright-text {
  display: block;
  font-size: 24rpx;
  color: $text-placeholder;
}
.copyright-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: $text-placeholder;
}
</style>
