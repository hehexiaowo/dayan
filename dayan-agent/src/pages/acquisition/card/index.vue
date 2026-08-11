<template>
  <view class="page">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <input
        v-model="keyword"
        class="dy-input search-input"
        placeholder="搜索名片名称 / 姓名 / 手机号"
        placeholder-class="input-placeholder"
        confirm-type="search"
        @confirm="loadList"
      />
    </view>

    <!-- 内容区 -->
    <view class="list-wrap">
      <!-- 骨架屏 -->
      <view v-if="loading && cards.length === 0" class="skeleton-wrap">
        <DySkeleton v-for="i in 2" :key="i" :rows="3" card />
      </view>

      <!-- 空状态 -->
      <DyEmpty
        v-else-if="cards.length === 0 && !loadError"
        text="还没有名片"
        icon="名"
        color="blue"
        action-text="创建名片"
        @action="onCreate"
      />

      <!-- 加载失败 -->
      <DyEmpty
        v-else-if="loadError"
        text="加载失败"
        icon="败"
        color="orange"
        action-text="重试"
        @action="loadList"
      />

      <!-- 名片列表 -->
      <view v-else>
        <view
          v-for="card in cards"
          :key="card.id"
          class="card-item dy-clickable"
          @click="onPreview(card)"
        >
          <!-- 左侧头像 -->
          <view class="card-avatar">
            <DyIconBlock
              :text="card.displayName?.charAt(0) || '?'"
              color="blue"
              size="md"
              shape="circle"
            />
          </view>

          <!-- 右侧信息 -->
          <view class="card-body">
            <view class="card-header">
              <text class="card-title-text">{{ card.displayName }}</text>
              <view v-if="card.status === 0" class="badge-off">停用</view>
            </view>
            <text v-if="card.title" class="card-subtitle">{{ card.title }}</text>
            <view class="card-info-row">
              <text v-if="card.phone" class="card-info-text">📱 {{ card.phone }}</text>
              <text v-if="card.company" class="card-info-text company">🏢 {{ card.company }}</text>
            </view>
            <view v-if="cardTags(card).length" class="card-tags">
              <text v-for="tag in cardTags(card)" :key="tag" class="mini-tag">{{ tag }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 悬浮按钮 -->
    <view class="fab dy-clickable" @click="onCreate">
      <text class="fab-icon">+</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getCards } from '@/api/card';
import type { BusinessCard } from '@/types';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const keyword = ref('');
const cards = ref<BusinessCard[]>([]);
const loading = ref(false);
const loadError = ref(false);

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    const res = await getCards({ keyword: keyword.value.trim() || undefined });
    cards.value = res.records || [];
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

/** 解析名片标签（逗号分隔 → 数组，最多展示 3 个） */
function cardTags(card: BusinessCard): string[] {
  if (!card.tags) return [];
  return card.tags
    .split(/[，,]/)
    .map((s) => s.trim())
    .filter(Boolean)
    .slice(0, 3);
}

function onCreate() {
  uni.navigateTo({ url: '/pages/acquisition/card/edit' });
}

function onPreview(card: BusinessCard) {
  uni.navigateTo({ url: '/pages/acquisition/card/preview?id=' + card.id });
}

// 每次进入页面统一刷新
onShow(() => {
  loadList();
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 120rpx;
}

/* 搜索栏 */
.search-bar {
  padding: $spacing-sm $spacing-lg;
  background: $bg-card;
}

.search-input {
  background: $bg-page;
  border-radius: $radius-md;
  padding: 16rpx 24rpx;
  font-size: 26rpx;
}

.input-placeholder {
  color: $text-placeholder;
  font-size: 26rpx;
}

/* 列表 */
.list-wrap {
  padding: $spacing-sm $spacing-lg;
}

.skeleton-wrap {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

/* 名片卡片项 */
.card-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md $spacing-lg;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}

.card-avatar {
  flex-shrink: 0;
  padding-top: 4rpx;
}

.card-body {
  flex: 1;
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.card-title-text {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}

.badge-off {
  font-size: 20rpx;
  color: $text-secondary;
  background: $bg-page;
  padding: 2rpx 12rpx;
  border-radius: $radius-sm;
}

.card-subtitle {
  display: block;
  margin-top: 4rpx;
  font-size: 26rpx;
  color: $text-secondary;
}

.card-info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4rpx $spacing-md;
  margin-top: $spacing-sm;
}

.card-info-text {
  font-size: 24rpx;
  color: $text-regular;

  &.company {
    color: $text-secondary;
  }
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  margin-top: $spacing-sm;
}

.mini-tag {
  font-size: 20rpx;
  color: $brand-primary;
  background: $brand-primary-light;
  padding: 4rpx 16rpx;
  border-radius: $radius-sm;
}

/* 悬浮按钮 */
.fab {
  position: fixed;
  right: 40rpx;
  bottom: 60rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: $gradient-blue;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(64, 158, 255, 0.4);
  z-index: 10;
}

.fab-icon {
  font-size: 52rpx;
  color: #fff;
  font-weight: 300;
  line-height: 1;
}
</style>
