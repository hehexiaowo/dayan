<template>
  <view class="page dy-safe-bottom">
    <!-- 搜索栏 -->
    <view class="toolbar">
      <view class="search">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索文章标题"
          confirm-type="search"
          @confirm="onSearch"
        />
        <text v-if="keyword" class="search-clear" @click="keyword = ''">×</text>
        <view class="btn-search dy-clickable" @click="onSearch">搜索</view>
      </view>
    </view>

    <!-- 文章列表 -->
    <view class="list">
      <template v-if="loading && !articles.length">
        <DySkeleton v-for="i in 3" :key="i" :rows="3" card />
      </template>

      <DyEmpty
        v-else-if="loadError"
        text="加载失败，请检查网络后重试"
        icon="!"
        color="gray"
        action-text="重新加载"
        @action="loadList"
      />

      <DyEmpty
        v-else-if="!filtered.length"
        text="暂无内容"
        icon="文"
        color="orange"
      />

      <view v-else>
        <view
          v-for="article in filtered"
          :key="article.contentCode"
          class="article-card dy-clickable"
          @click="goDetail(article.contentCode)"
        >
          <!-- 有封面图的卡片：左文右图 -->
          <view v-if="formatFileUrl(article.coverImage)" class="card-with-image">
            <view class="card-text-area">
              <text class="article-title">{{ article.title }}</text>
              <text v-if="article.summary" class="article-summary">{{ article.summary }}</text>
              <view class="card-footer">
                <text v-if="article.authorName" class="article-author">{{ article.authorName }}</text>
                <text v-if="article.viewCount != null" class="article-views">{{ article.viewCount }} 阅读</text>
                <text v-if="article.publishTime" class="article-date">{{ formatDate(article.publishTime) }}</text>
              </view>
            </view>
            <image
              :src="formatFileUrl(article.coverImage)"
              mode="aspectFill"
              class="card-cover"
            />
          </view>
          <!-- 无封面图：纯文字卡片 -->
          <view v-else>
            <text class="article-title">{{ article.title }}</text>
            <text v-if="article.subtitle" class="article-subtitle">{{ article.subtitle }}</text>
            <text v-if="article.summary" class="article-summary">{{ article.summary }}</text>
            <view class="card-footer">
              <text v-if="article.authorName" class="article-author">{{ article.authorName }}</text>
              <text v-if="article.viewCount != null" class="article-views">{{ article.viewCount }} 阅读</text>
              <text v-if="article.publishTime" class="article-date">{{ formatDate(article.publishTime) }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getContentList } from '@/api/content';
import { formatFileUrl } from '@/utils/file';
import type { ContentArticle } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const keyword = ref('');
const articles = ref<ContentArticle[]>([]);
const loading = ref(false);
const loadError = ref(false);

const filtered = computed(() => {
  if (!keyword.value.trim()) return articles.value;
  const kw = keyword.value.trim().toLowerCase();
  return articles.value.filter((a) => a.title.toLowerCase().includes(kw));
});

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    const res = await getContentList({ size: 50 });
    articles.value = res?.records || [];
  } catch (e) {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  // 前端过滤
}

function goDetail(contentCode: string) {
  uni.navigateTo({ url: `/pages/acquisition/content/detail?code=${contentCode}` });
}

function formatDate(dt?: string): string {
  if (!dt) return '';
  return dt.length >= 10 ? dt.substring(0, 10) : dt;
}

onShow(() => {
  loadList();
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page {
  padding: $spacing-md $spacing-md 140rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* 搜索栏 */
.toolbar {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  box-shadow: $shadow-card;
}
.search {
  display: flex;
  align-items: center;
}
.search-input {
  flex: 1;
  height: $control-height-sm;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  padding: 0 20rpx;
  font-size: 28rpx;
  background: $bg-page;
  box-sizing: border-box;
}
.search-clear {
  padding: 0 16rpx;
  font-size: 36rpx;
  color: $text-placeholder;
}
.btn-search {
  margin-left: $spacing-sm;
  height: $control-height-sm;
  line-height: $control-height-sm;
  background: $gradient-blue;
  color: #fff;
  font-size: 26rpx;
  padding: 0 32rpx;
  border-radius: $radius-md;
}

/* 文章列表 */
.list {
  margin-top: $spacing-md;
}
.article-card {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-lg $spacing-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}

/* 有封面的卡片 */
.card-with-image {
  display: flex;
  gap: $spacing-md;
}
.card-text-area {
  flex: 1;
  min-width: 0;
}
.card-cover {
  width: 200rpx;
  height: 140rpx;
  border-radius: $radius-sm;
  flex-shrink: 0;
  background: $bg-page;
}

/* 文字 */
.article-title {
  display: block;
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.article-subtitle {
  display: block;
  font-size: 26rpx;
  color: $text-secondary;
  margin-top: 8rpx;
}
.article-summary {
  display: block;
  font-size: 24rpx;
  color: $text-placeholder;
  margin-top: 8rpx;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.card-footer {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
  flex-wrap: wrap;
}
.article-author {
  font-size: 22rpx;
  color: $brand-primary;
}
.article-views {
  font-size: 22rpx;
  color: $text-placeholder;
}
.article-date {
  font-size: 22rpx;
  color: $text-placeholder;
}
</style>
