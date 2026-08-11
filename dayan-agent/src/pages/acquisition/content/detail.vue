<template>
  <view class="page dy-safe-bottom">
    <template v-if="loading">
      <DySkeleton :rows="8" card />
    </template>

    <template v-else-if="article">
      <!-- 封面图 -->
      <view v-if="formatFileUrl(article.coverImage)" class="hero">
        <image
          :src="formatFileUrl(article.coverImage)"
          mode="widthFix"
          class="hero-img"
        />
      </view>

      <!-- 文章信息 -->
      <view class="info-card">
        <text class="title">{{ article.title }}</text>
        <text v-if="article.subtitle" class="subtitle">{{ article.subtitle }}</text>
        <view class="meta-row">
          <view v-if="article.authorName" class="author-chip">
            <text class="author-name">{{ article.authorName }}</text>
          </view>
          <text v-if="article.publishTime" class="meta-date">{{ formatDate(article.publishTime) }}</text>
          <text v-if="article.viewCount != null" class="meta-views">{{ article.viewCount }} 阅读</text>
        </view>
      </view>

      <!-- 正文 -->
      <view class="body-card">
        <text class="body-text">{{ article.contentBody || article.summary || '暂无内容' }}</text>
      </view>

      <!-- 底部分享栏 -->
      <view class="bottom-bar">
        <view class="share-btn dy-clickable" @click="onShare">
          <text class="share-icon">↗</text>
          <text class="share-text">分享给客户</text>
        </view>
      </view>
    </template>

    <DyEmpty
      v-else
      text="内容不存在或已下线"
      icon="!"
      color="gray"
    />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getContentDetail } from '@/api/content';
import { formatFileUrl } from '@/utils/file';
import type { ContentArticle } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const article = ref<ContentArticle | null>(null);
const loading = ref(true);
const contentCode = ref('');

function formatDate(dt?: string): string {
  if (!dt) return '';
  return dt.length >= 10 ? dt.substring(0, 10) : dt;
}

async function loadDetail() {
  loading.value = true;
  try {
    article.value = await getContentDetail(contentCode.value);
  } catch (e) {
    article.value = null;
  } finally {
    loading.value = false;
  }
}

function onShare() {
  uni.showToast({ title: '分享功能开发中', icon: 'none' });
}

onLoad((query) => {
  contentCode.value = query?.code || '';
  if (contentCode.value) {
    loadDetail();
  } else {
    loading.value = false;
  }
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page {
  padding-bottom: 140rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* 封面 */
.hero {
  width: 100%;
  background: $bg-card;
}
.hero-img {
  width: 100%;
}

/* 文章信息 */
.info-card {
  background: $bg-card;
  padding: $spacing-lg $spacing-md;
}
.title {
  display: block;
  font-size: 36rpx;
  font-weight: bold;
  color: $text-primary;
  line-height: 1.5;
}
.subtitle {
  display: block;
  font-size: 28rpx;
  color: $text-secondary;
  margin-top: $spacing-sm;
  line-height: 1.5;
}
.meta-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-top: $spacing-md;
  flex-wrap: wrap;
}
.author-chip {
  background: $brand-primary-light;
  border-radius: $radius-sm;
  padding: 4rpx 16rpx;
}
.author-name {
  font-size: 24rpx;
  color: $brand-primary;
}
.meta-date,
.meta-views {
  font-size: 24rpx;
  color: $text-placeholder;
}

/* 正文 */
.body-card {
  background: $bg-card;
  padding: $spacing-lg $spacing-md;
  margin-top: $spacing-sm;
}
.body-text {
  font-size: 30rpx;
  color: $text-primary;
  line-height: 2;
  white-space: pre-wrap;
}

/* 底部分享栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: $spacing-md $spacing-lg;
  padding-bottom: calc(#{$spacing-md} + env(safe-area-inset-bottom));
  background: $bg-card;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
  z-index: 100;
}
.share-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  height: 80rpx;
  background: $gradient-blue;
  border-radius: $radius-md;
}
.share-icon {
  font-size: 32rpx;
  color: #fff;
}
.share-text {
  font-size: 28rpx;
  font-weight: 500;
  color: #fff;
}
</style>
