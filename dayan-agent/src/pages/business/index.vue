<template>
  <view class="page">
    <view class="header">
      <text class="header-title">展业工具</text>
      <text class="header-sub">查询机构、采购权益、报名活动</text>
    </view>

    <!-- 找机构 -->
    <view class="card" @click="onSection('park')">
      <view class="card-icon" style="background: #409eff">机</view>
      <view class="card-body">
        <view class="card-title">找机构</view>
        <view class="card-desc">旅居养老 / 活力长居 / 照护长居</view>
      </view>
    </view>

    <!-- 大雁商城 -->
    <view class="card" @click="onSection('mall')">
      <view class="card-icon" style="background: #19be6b">商</view>
      <view class="card-body">
        <view class="card-title">大雁商城</view>
        <view class="card-desc">权益 / 场景 / 课程 / 旅居商品</view>
      </view>
      <view class="card-tag">即将上线</view>
    </view>

    <!-- 场景活动 -->
    <view class="card" @click="onSection('activity')">
      <view class="card-icon" style="background: #ff9900">活</view>
      <view class="card-body">
        <view class="card-title">场景活动</view>
        <view class="card-desc">参观体验 / 健康讲座 / 节日活动</view>
      </view>
      <view class="card-tag">即将上线</view>
    </view>
  </view>
</template>

<script setup lang="ts">
function onSection(type: string) {
  if (type === 'park') {
    uni.navigateTo({ url: '/pages/business/park/categories' });
    return;
  }
  const tips: Record<string, string> = {
    mall: '大雁商城（Inc 2 上线）',
    activity: '场景活动（Inc 3 上线）',
  };
  uni.showToast({ title: tips[type] || '开发中', icon: 'none' });
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 60rpx;
  min-height: 100vh;
  background: #f5f7fa;
}
.header {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.header-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #303133;
}
.header-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #909399;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
}
.card-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 36rpx;
  font-weight: bold;
  flex-shrink: 0;
}
.card-body {
  margin-left: 24rpx;
  flex: 1;
}
.card-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}
.card-desc {
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #909399;
}
.card-tag {
  font-size: 22rpx;
  color: #ff9900;
  background: #fff7e6;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
}
</style>
