<template>
  <view class="page dy-safe-bottom">
    <!-- ===== Banner 渐变区 ===== -->
    <view class="banner">
      <view class="banner-content">
        <text class="banner-title">学习中心</text>
        <text class="banner-sub">专业赋能，持续提升</text>
      </view>
      <view class="banner-icon">
        <text class="banner-icon-text">学</text>
      </view>
    </view>

    <!-- ===== Tab 切换 ===== -->
    <view class="tabs">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item dy-clickable"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <text class="tab-text">{{ tab.label }}</text>
      </view>
    </view>

    <!-- ===== 内容区 ===== -->
    <view class="list">
      <DyEmpty
        :text="emptyText"
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
  emptyText: string;
}

const tabs: TabItem[] = [
  { key: 'video', label: '视频课程', icon: '视', emptyText: '暂无视频课程' },
  { key: 'article', label: '图文课程', icon: '文', emptyText: '暂无图文课程' },
  { key: 'yanming', label: '雁鸣中国', icon: '鸣', emptyText: '暂无内容' },
];

const activeTab = ref('video');

const currentTab = computed(() =>
  tabs.find((t) => t.key === activeTab.value) || tabs[0],
);

const emptyText = computed(() => currentTab.value.emptyText);
const emptyIcon = computed(() => currentTab.value.icon);
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  padding: $spacing-md $spacing-md 60rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* ===== Banner ===== */
.banner {
  background: $gradient-blue;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.banner-content {
  flex: 1;
}

.banner-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
}

.banner-sub {
  display: block;
  margin-top: $spacing-xs;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

.banner-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: 4rpx solid rgba(255, 255, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.banner-icon-text {
  font-size: 44rpx;
  font-weight: bold;
  color: #fff;
}

/* ===== Tab 切换 ===== */
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
  position: relative;
  transition: color $transition-fast;
}

.tab-text {
  font-size: 28rpx;
  color: $text-regular;
}

.tab-item.active .tab-text {
  color: $brand-primary;
  font-weight: bold;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 80rpx;
  height: 6rpx;
  background: $gradient-blue;
  border-radius: 3rpx;
}

/* ===== 内容区 ===== */
.list {
  min-height: 400rpx;
  background: $bg-card;
  border-radius: $radius-md;
  box-shadow: $shadow-card;
  padding: $spacing-lg;
}
</style>
