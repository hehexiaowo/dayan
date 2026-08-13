<template>
  <view class="page">
    <!-- 头部 -->
    <view class="header">
      <text class="header-title">营销海报</text>
      <text class="header-subtitle">精选营销模板，一键分享给客户</text>
    </view>

    <!-- 分类筛选 -->
    <view class="category-bar">
      <view
        v-for="cat in categories"
        :key="cat.code"
        class="category-item dy-clickable"
        :class="{ active: activeCategory === cat.code }"
        @click="activeCategory = cat.code"
      >
        <text>{{ cat.name }}</text>
      </view>
    </view>

    <!-- 海报网格 -->
    <view class="poster-grid">
      <!-- 骨架屏 -->
      <template v-if="loading && !templates.length">
        <view v-for="i in 4" :key="i" class="poster-card skeleton-card">
          <view class="skeleton-cover" />
          <view class="skeleton-line" />
          <view class="skeleton-line short" />
        </view>
      </template>

      <!-- 空状态 -->
      <DyEmpty
        v-else-if="!filteredTemplates.length"
        text="暂无海报模板"
        icon="海"
        color="orange"
      />

      <!-- 海报卡片 -->
      <view
        v-for="tpl in filteredTemplates"
        :key="tpl.templateCode"
        class="poster-card dy-clickable"
        @click="onOpenDetail(tpl)"
      >
        <view class="poster-cover">
          <image
            v-if="tpl.coverImage"
            :src="formatFileUrl(tpl.coverImage)"
            mode="aspectFill"
            class="cover-img"
          />
          <view v-else class="cover-placeholder">
            <text class="cover-placeholder-text">{{ tpl.title?.charAt(0) }}</text>
          </view>
          <view class="poster-badge">{{ tpl.categoryName }}</view>
        </view>
        <view class="poster-info">
          <text class="poster-title">{{ tpl.title }}</text>
          <text v-if="tpl.subtitle" class="poster-subtitle">{{ tpl.subtitle }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getPosterTemplates, POSTER_CATEGORIES } from '@/api/poster';
import type { PosterTemplate } from '@/api/poster';
import { formatFileUrl } from '@/utils/file';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const templates = ref<PosterTemplate[]>([]);
const loading = ref(false);
const activeCategory = ref('');

const categories = POSTER_CATEGORIES;

const filteredTemplates = computed(() => {
  if (!activeCategory.value) return templates.value;
  return templates.value.filter((t) => t.categoryCode === activeCategory.value);
});

async function loadList() {
  loading.value = true;
  try {
    templates.value = await getPosterTemplates();
  } catch {
    templates.value = [];
  } finally {
    loading.value = false;
  }
}

function onOpenDetail(tpl: PosterTemplate) {
  uni.navigateTo({ url: '/pages/acquisition/poster/detail?code=' + tpl.templateCode });
}

onShow(() => loadList());
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

/* 分类筛选 */
.category-bar {
  display: flex;
  padding: $spacing-sm $spacing-md;
  gap: $spacing-sm;
  background: $bg-card;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  white-space: nowrap;
  box-shadow: $shadow-card;
}
.category-item {
  padding: 12rpx 32rpx;
  border-radius: $radius-sm;
  background: $brand-info-light;
  font-size: 26rpx;
  color: $text-secondary;
  flex-shrink: 0;
  transition: all $transition-base;
}
.category-item.active {
  background: $brand-primary;
  color: #fff;
  font-weight: 500;
}

/* 海报网格 */
.poster-grid {
  display: flex;
  flex-wrap: wrap;
  padding: $spacing-md;
  gap: $spacing-md;
}

/* 海报卡片 */
.poster-card {
  width: calc(50% - #{$spacing-md} / 2);
  background: $bg-card;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-card;
}
.poster-cover {
  position: relative;
  width: 100%;
  height: 280rpx;
  overflow: hidden;
}
.cover-img {
  width: 100%;
  height: 100%;
}
.cover-placeholder {
  width: 100%;
  height: 100%;
  background: $gradient-blue;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cover-placeholder-text {
  font-size: 72rpx;
  color: rgba(255, 255, 255, 0.8);
  font-weight: bold;
}
.poster-badge {
  position: absolute;
  top: $spacing-sm;
  left: $spacing-sm;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 20rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}
.poster-info {
  padding: $spacing-sm $spacing-md $spacing-md;
}
.poster-title {
  font-size: 28rpx;
  font-weight: bold;
  color: $text-primary;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.poster-subtitle {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: $text-secondary;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 骨架屏 */
.skeleton-card {
  width: calc(50% - #{$spacing-md} / 2);
}
.skeleton-cover {
  width: 100%;
  height: 280rpx;
  background: $border-light;
}
.skeleton-line {
  height: 24rpx;
  margin: $spacing-sm $spacing-md;
  background: $border-light;
  border-radius: $radius-sm;
}
.skeleton-line.short {
  width: 60%;
}
</style>
