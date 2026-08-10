<template>
  <view class="page">
    <!-- 渐变 header -->
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
      <DyEmpty
        :text="`暂无${activeTabLabel}内容`"
        :icon="emptyIcon"
        color="blue"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

interface TabItem {
  key: string;
  label: string;
  icon: string;
}

const tabs: TabItem[] = [
  { key: 'article', label: '资讯', icon: '资' },
  { key: 'course', label: '课程', icon: '课' },
  { key: 'script', label: '话术', icon: '话' },
];

const activeTab = ref('article');
const activeTabLabel = computed(() =>
  tabs.find((t) => t.key === activeTab.value)?.label || '',
);
const emptyIcon = computed(() =>
  tabs.find((t) => t.key === activeTab.value)?.icon || '空',
);
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  padding: $spacing-md $spacing-md 60rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* 渐变 header */
.header {
  background: $gradient-blue;
  border-radius: $radius-md;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
}
.header-title {
  font-size: 38rpx;
  font-weight: bold;
  color: #fff;
}
.header-sub {
  display: block;
  margin-top: $spacing-xs;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

/* tabs */
.tabs {
  display: flex;
  background: $bg-card;
  border-radius: $radius-md;
  margin-bottom: $spacing-md;
  overflow: hidden;
  box-shadow: $shadow-card;
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: $spacing-md 0;
  font-size: 28rpx;
  color: $text-regular;
  position: relative;
  transition: color $transition-fast;
}
.tab-item.active {
  color: $brand-primary;
  font-weight: bold;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60rpx;
  height: 6rpx;
  background: $gradient-blue;
  border-radius: 3rpx;
}

/* list */
.list {
  min-height: 400rpx;
  background: $bg-card;
  border-radius: $radius-md;
  box-shadow: $shadow-card;
  padding: $spacing-lg;
}
</style>
