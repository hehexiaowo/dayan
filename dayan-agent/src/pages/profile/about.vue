<template>
  <view class="page">
    <!-- hero（蓝色品牌渐变） -->
    <view class="hero">
      <view class="logo-circle">
        <text class="logo-text">养老宝典</text>
      </view>
      <text class="app-name">养老宝典</text>
      <text class="version">v1.0.0</text>
    </view>

    <!-- 关于我们 -->
    <view class="section">
      <view class="section-title">关于我们</view>
      <view class="section-body">
        <text class="intro">养老宝典客户端，为养老行业从业者提供一站式展业工具。在这里，您可以查询全国养老机构、采购权益商品、分享内容获客、跟进客户线索，让养老服务事业更高效、更专业。</text>
      </view>
    </view>

    <!-- 功能特色 -->
    <view class="section">
      <view class="section-title">功能特色</view>
      <view class="feature-list">
        <view v-for="f in features" :key="f.title" class="feature-item">
          <view class="feature-dot" :style="{ background: f.color }">
            <text class="feature-dot-text">{{ f.icon }}</text>
          </view>
          <view class="feature-text">
            <text class="feature-title">{{ f.title }}</text>
            <text class="feature-desc">{{ f.desc }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 版权 -->
    <view class="copyright">
      <text class="copyright-text">© 2026 养老宝典</text>
      <text class="copyright-slogan">专业守护，用心服务</text>
    </view>
  </view>
</template>

<script setup lang="ts">
const features = [
  { icon: '构', title: '机构查询', desc: '三网络地图下钻，全国养老机构精准查找', color: 'linear-gradient(135deg, #409eff, #5f8afe)' },
  { icon: '权', title: '权益采购', desc: '专属权益商品采购，库存与订单一目了然', color: 'linear-gradient(135deg, #19be6b, #2bcc7a)' },
  { icon: '获', title: '分享获客', desc: '内容/海报/名片一键分享，客户线索自动沉淀', color: 'linear-gradient(135deg, #ff9900, #ffb84d)' },
  { icon: '学', title: '学习提升', desc: '专业知识课程，持续提升展业能力', color: 'linear-gradient(135deg, #36cfc9, #5cdbd3)' },
];
</script>

<style lang="scss" scoped>

.page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 60rpx;
}

/* hero */
.hero {
  background: $gradient-blue;
  padding: 80rpx $spacing-lg 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.logo-circle {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
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
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
}
.version {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
}

/* section */
.section {
  margin: $spacing-md $spacing-lg 0;
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
}
.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
  margin-bottom: $spacing-sm;
}
.intro {
  font-size: 28rpx;
  color: $text-regular;
  line-height: 1.7;
}

/* 功能特色 */
.feature-list {
  margin-top: $spacing-sm;
}
.feature-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid $border-light;
}
.feature-item:last-child {
  border-bottom: none;
}
.feature-dot {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.feature-dot-text {
  font-size: 30rpx;
  font-weight: bold;
  color: #fff;
}
.feature-text {
  margin-left: $spacing-md;
  display: flex;
  flex-direction: column;
}
.feature-title {
  font-size: 28rpx;
  font-weight: bold;
  color: $text-primary;
}
.feature-desc {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: $text-secondary;
}

/* 版权 */
.copyright {
  margin-top: $spacing-lg;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.copyright-text {
  font-size: 24rpx;
  color: $text-placeholder;
}
.copyright-slogan {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $text-placeholder;
}
</style>
