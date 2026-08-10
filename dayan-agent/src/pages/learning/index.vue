<template>
  <view class="page">
    <view class="header">
      <text class="header-title">学习中心</text>
      <text class="header-sub">专业赋能，持续提升</text>
    </view>

    <!-- 分类切换 -->
    <view class="tabs">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- 内容列表 -->
    <view class="list">
      <view class="empty">
        暂无{{ activeTabLabel }}内容
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

interface TabItem {
  key: string;
  label: string;
}

const tabs: TabItem[] = [
  { key: 'article', label: '资讯' },
  { key: 'course', label: '课程' },
  { key: 'script', label: '话术' },
];

const activeTab = ref('article');
const activeTabLabel = computed(() =>
  tabs.find((t) => t.key === activeTab.value)?.label || '',
);
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
.tabs {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: #606266;
  position: relative;
}
.tab-item.active {
  color: #409eff;
  font-weight: bold;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60rpx;
  height: 4rpx;
  background: #409eff;
  border-radius: 2rpx;
}
.empty {
  text-align: center;
  color: #909399;
  font-size: 26rpx;
  padding: 80rpx 0;
}
</style>
