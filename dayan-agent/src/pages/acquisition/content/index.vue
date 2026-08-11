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
        <text v-if="keyword" class="search-clear" @click="clearKeyword">×</text>
        <view class="btn-search dy-clickable" @click="onSearch">搜索</view>
      </view>
    </view>

    <!-- 分类导航 -->
    <scroll-view v-if="categories.length" scroll-x class="category-bar" :show-scrollbar="false">
      <view class="cat-pill dy-clickable" :class="{ active: !activeCategory }" @click="switchCategory('')">全部</view>
      <view
        v-for="cat in categories"
        :key="cat.categoryCode"
        class="cat-pill dy-clickable"
        :class="{ active: activeCategory === cat.categoryCode }"
        @click="switchCategory(cat.categoryCode)"
      >
        {{ cat.categoryName }}
      </view>
    </scroll-view>

    <!-- 文章列表 -->
    <view class="list">
      <!-- 首次加载骨架 -->
      <template v-if="loading && !articles.length">
        <DySkeleton v-for="i in 3" :key="i" :rows="3" card />
      </template>

      <DyEmpty
        v-else-if="loadError && !articles.length"
        text="加载失败，请检查网络后重试"
        icon="!"
        color="gray"
        action-text="重新加载"
        @action="loadList(true)"
      />

      <DyEmpty
        v-else-if="!articles.length"
        text="暂无内容"
        icon="文"
        color="orange"
      />

      <template v-else>
        <view
          v-for="article in articles"
          :key="article.contentCode"
          class="article-card dy-clickable"
          @click="goDetail(article.contentCode)"
        >
          <!-- 有封面：左文右图 -->
          <view v-if="formatFileUrl(article.coverImage)" class="card-with-image">
            <view class="card-text-area">
              <view class="badge-row">
                <text class="badge badge-type">{{ getTypeLabel(article.contentType) }}</text>
                <text
                  v-for="badge in getBadges(article)"
                  :key="badge.cls"
                  class="badge"
                  :class="badge.cls"
                >{{ badge.text }}</text>
              </view>
              <text class="article-title">{{ article.title }}</text>
              <text v-if="article.summary" class="article-summary one-line">{{ article.summary }}</text>
              <view class="card-footer">
                <text v-if="article.authorName" class="article-author">{{ article.authorName }}</text>
                <text v-if="article.viewCount != null" class="article-views">{{ formatCount(article.viewCount) }} 阅读</text>
                <text v-if="article.publishTime" class="article-date">{{ formatDate(article.publishTime) }}</text>
                <view class="fav-btn dy-clickable" @click.stop="toggleFavorite(article)">
                  <text class="fav-icon" :class="{ favorited: article.isFavorited }">{{ article.isFavorited ? '♥' : '♡' }}</text>
                  <text v-if="article.collectCount" class="fav-count">{{ article.collectCount }}</text>
                </view>
              </view>
            </view>
            <image
              :src="formatFileUrl(article.coverImage)"
              mode="aspectFill"
              class="card-cover"
            />
          </view>
          <!-- 无封面：纯文字 -->
          <view v-else>
            <view class="badge-row">
              <text class="badge badge-type">{{ getTypeLabel(article.contentType) }}</text>
              <text
                v-for="badge in getBadges(article)"
                :key="badge.cls"
                class="badge"
                :class="badge.cls"
              >{{ badge.text }}</text>
            </view>
            <text class="article-title">{{ article.title }}</text>
            <text v-if="article.summary" class="article-summary">{{ article.summary }}</text>
            <view class="card-footer">
              <text v-if="article.authorName" class="article-author">{{ article.authorName }}</text>
              <text v-if="article.viewCount != null" class="article-views">{{ formatCount(article.viewCount) }} 阅读</text>
              <text v-if="article.publishTime" class="article-date">{{ formatDate(article.publishTime) }}</text>
              <view class="fav-btn dy-clickable" @click.stop="toggleFavorite(article)">
                <text class="fav-icon" :class="{ favorited: article.isFavorited }">{{ article.isFavorited ? '♥' : '♡' }}</text>
                <text v-if="article.collectCount" class="fav-count">{{ article.collectCount }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 加载更多 / 没有更多 -->
        <view v-if="loadingMore" class="list-end">
          <text class="list-end-text">加载中...</text>
        </view>
        <view v-else-if="!hasMore" class="list-end">
          <text class="list-end-text">没有更多了</text>
        </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app';
import { getContentList, getContentCategories } from '@/api/content';
import { addFavoriteApi, removeFavoriteApi, getFavoritedCodesApi, TARGET_TYPE } from '@/api/favorite';
import { formatFileUrl } from '@/utils/file';
import type { ContentArticle, ContentCategoryOption } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const keyword = ref('');
const activeCategory = ref('');
const categories = ref<ContentCategoryOption[]>([]);
const articles = ref<ContentArticle[]>([]);
const favoritedSet = ref<Set<string>>(new Set());
const loading = ref(false);
const loadingMore = ref(false);
const loadError = ref(false);
const current = ref(1);
const size = 10;
const hasMore = ref(true);

interface Badge {
  text: string;
  cls: string;
}

/** 加载分类列表 */
async function loadCategories() {
  try {
    categories.value = await getContentCategories();
  } catch {
    categories.value = [];
  }
}

/** 加载当前代理人已收藏的内容编码集合 */
async function loadFavorited() {
  try {
    favoritedSet.value = new Set(await getFavoritedCodesApi(TARGET_TYPE.CONTENT));
  } catch {
    favoritedSet.value = new Set();
  }
}

/** 加载文章列表（reset=true 回到第 1 页） */
async function loadList(reset = false) {
  if (reset) {
    current.value = 1;
    hasMore.value = true;
    loading.value = true;
  } else {
    loadingMore.value = true;
  }
  loadError.value = false;
  try {
    const res = await getContentList({
      current: current.value,
      size,
      title: keyword.value.trim() || undefined,
      categoryCode: activeCategory.value || undefined,
    });
    const records = (res?.records || []).map((a) => ({
      ...a,
      isFavorited: favoritedSet.value.has(a.contentCode),
    }));
    if (reset) {
      articles.value = records;
    } else {
      articles.value.push(...records);
    }
    hasMore.value = records.length >= size;
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
}

function switchCategory(code: string) {
  if (activeCategory.value === code) return;
  activeCategory.value = code;
  loadList(true);
}

function onSearch() {
  loadList(true);
}

function clearKeyword() {
  keyword.value = '';
  loadList(true);
}

/** 乐观切换收藏 */
async function toggleFavorite(article: ContentArticle) {
  const was = article.isFavorited ?? false;
  article.isFavorited = !was;
  article.collectCount = Math.max(0, (article.collectCount || 0) + (was ? -1 : 1));
  try {
    if (was) {
      await removeFavoriteApi(TARGET_TYPE.CONTENT, article.contentCode);
      favoritedSet.value.delete(article.contentCode);
    } else {
      await addFavoriteApi(TARGET_TYPE.CONTENT, article.contentCode);
      favoritedSet.value.add(article.contentCode);
    }
  } catch {
    // 回滚（request 已 toast 错误提示）
    article.isFavorited = was;
    article.collectCount = Math.max(0, (article.collectCount || 0) + (was ? 1 : -1));
  }
}

function goDetail(contentCode: string) {
  uni.navigateTo({ url: `/pages/acquisition/content/detail?code=${contentCode}` });
}

/** 内容形式标签 */
function getTypeLabel(type?: number): string {
  switch (type) {
    case 2: return '视频';
    case 3: return '图集';
    case 6: return '文件';
    default: return '图文';
  }
}

/** 置顶 > 热 > 新 */
function getBadges(article: ContentArticle): Badge[] {
  const badges: Badge[] = [];
  if (article.isTop === 1) badges.push({ text: '置顶', cls: 'badge-top' });
  if (article.isRecommend === 1) badges.push({ text: '热', cls: 'badge-hot' });
  if (isNew(article.publishTime)) badges.push({ text: '新', cls: 'badge-new' });
  return badges;
}

/** 7 天内算新 */
function isNew(publishTime?: string): boolean {
  if (!publishTime) return false;
  const diff = Date.now() - new Date(publishTime.replace(/-/g, '/')).getTime();
  return diff < 7 * 24 * 60 * 60 * 1000;
}

function formatDate(dt?: string): string {
  if (!dt) return '';
  return dt.length >= 10 ? dt.substring(0, 10) : dt;
}

function formatCount(n?: number): string {
  if (n == null) return '0';
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w';
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
  return String(n);
}

onShow(async () => {
  await Promise.all([loadCategories(), loadFavorited()]);
  loadList(true);
});

onReachBottom(() => {
  if (hasMore.value && !loading.value && !loadingMore.value) {
    current.value++;
    loadList(false);
  }
});

onPullDownRefresh(async () => {
  await loadFavorited();
  await loadList(true);
  uni.stopPullDownRefresh();
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

/* 分类导航 */
.category-bar {
  display: flex;
  white-space: nowrap;
  margin-top: $spacing-md;
  padding: 4rpx 0;
}
.cat-pill {
  display: inline-flex;
  align-items: center;
  height: 60rpx;
  padding: 0 28rpx;
  margin-right: $spacing-sm;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: $text-secondary;
  background: $bg-card;
  flex-shrink: 0;
  transition: all $transition-fast;
}
.cat-pill.active {
  background: $gradient-blue;
  color: #fff;
  font-weight: 500;
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

/* badge */
.badge-row {
  display: flex;
  gap: 8rpx;
  margin-bottom: 10rpx;
}
.badge {
  display: inline-flex;
  align-items: center;
  font-size: 20rpx;
  line-height: 1;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
  font-weight: 500;
}
.badge-top {
  background: $brand-error-light;
  color: $brand-error;
}
.badge-type {
  background: $bg-page;
  color: $text-secondary;
}
.badge-hot {
  background: $brand-warning-light;
  color: $brand-warning;
}
.badge-new {
  background: $brand-primary-light;
  color: $brand-primary;
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
  height: 130rpx;
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
.article-summary.one-line {
  -webkit-line-clamp: 1;
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

/* 收藏角标 */
.fav-btn {
  display: flex;
  align-items: center;
  gap: 4rpx;
  margin-left: auto;
  padding: 4rpx 8rpx;
}
.fav-icon {
  font-size: 32rpx;
  color: $text-placeholder;
  transition: color $transition-fast;
}
.fav-icon.favorited {
  color: $brand-error;
}
.fav-count {
  font-size: 22rpx;
  color: $text-placeholder;
}

/* 列表底部 */
.list-end {
  display: flex;
  justify-content: center;
  padding: $spacing-lg 0;
}
.list-end-text {
  font-size: 24rpx;
  color: $text-placeholder;
}
</style>
