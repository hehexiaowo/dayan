<template>
  <view class="page dy-safe-bottom">
    <!-- Sticky: 搜索栏 + 分类导航 -->
    <view class="sticky-header">
      <view class="search-bar">
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
    </view>

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
          <!-- ====== 图文 (type=1 / default) ====== -->
          <template v-if="!article.contentType || article.contentType === 1">
            <view v-if="getBadges(article).length" class="badge-row">
              <text
                v-for="badge in getBadges(article)"
                :key="badge.cls"
                class="badge"
                :class="badge.cls"
              >{{ badge.text }}</text>
            </view>
            <view class="text-card-body">
              <view class="text-area">
                <text class="card-title">{{ article.title }}</text>
                <text v-if="article.summary" class="card-summary one-line">{{ article.summary }}</text>
              </view>
              <image
                v-if="formatFileUrl(article.coverImage)"
                :src="formatFileUrl(article.coverImage)"
                mode="aspectFill"
                class="text-thumb"
              />
            </view>
          </template>

          <!-- ====== 视频 (type=2) ====== -->
          <template v-else-if="article.contentType === 2">
            <view class="video-banner-wrap">
              <image
                v-if="formatFileUrl(article.coverImage)"
                :src="formatFileUrl(article.coverImage)"
                mode="aspectFill"
                class="video-banner"
              />
              <view v-else class="video-banner video-placeholder" />
              <view class="video-overlay" />
              <view class="play-circle">
                <text class="play-triangle">▶</text>
              </view>
              <text class="type-tag">视频</text>
            </view>
            <text class="card-title">{{ article.title }}</text>
          </template>

          <!-- ====== 图集 (type=3) ====== -->
          <template v-else-if="article.contentType === 3">
            <view class="gallery-strip">
              <view
                v-for="(img, i) in getGalleryThumbs(article)"
                :key="i"
                class="thumb-item"
              >
                <image :src="img" mode="aspectFill" class="thumb-img" />
                <text v-if="i === 2 && getGalleryCount(article) > 3" class="thumb-more">+{{ getGalleryCount(article) - 3 }}</text>
              </view>
              <!-- 不足3张时补占位 -->
              <view v-for="i in (3 - getGalleryThumbs(article).length)" :key="'ph'+i" class="thumb-item thumb-placeholder">
                <text class="thumb-ph-icon">图</text>
              </view>
            </view>
            <text class="card-title">{{ article.title }}</text>
          </template>

          <!-- ====== 文件 (type=6) ====== -->
          <template v-else-if="article.contentType === 6">
            <view v-if="getBadges(article).length" class="badge-row">
              <text
                v-for="badge in getBadges(article)"
                :key="badge.cls"
                class="badge"
                :class="badge.cls"
              >{{ badge.text }}</text>
            </view>
            <view class="file-row">
              <view class="file-icon-box">
                <text class="file-icon-text">📄</text>
                <text v-if="getFileInfo(article).ext" class="file-ext">{{ getFileInfo(article).ext }}</text>
              </view>
              <view class="file-meta-col">
                <text class="file-name-text">{{ getFileInfo(article).name }}</text>
                <text class="file-sub-text">{{ getFileInfo(article).size }}</text>
              </view>
            </view>
            <text class="card-title">{{ article.title }}</text>
          </template>

          <!-- ====== 公共底栏 ====== -->
          <view class="card-footer">
            <text v-if="article.authorName" class="footer-author">{{ article.authorName }}</text>
            <text v-if="article.viewCount != null" class="footer-views">
              {{ formatCount(article.viewCount) }} {{ article.contentType === 2 ? '播放' : article.contentType === 6 ? '下载' : '阅读' }}
            </text>
            <text v-if="article.publishTime" class="footer-date">{{ formatDate(article.publishTime) }}</text>
            <view class="fav-btn dy-clickable" @click.stop="toggleFavorite(article)">
              <text class="fav-icon" :class="{ favorited: article.isFavorited }">{{ article.isFavorited ? '♥' : '♡' }}</text>
              <text v-if="article.collectCount" class="fav-count">{{ article.collectCount }}</text>
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

/** ====== 内容解析工具（图集/文件 content_body 存 JSON） ====== */

/** 图集缩略图：取 content_body JSON 前 3 张 */
function getGalleryThumbs(article: ContentArticle): string[] {
  if (article.contentType !== 3 || !article.contentBody) return [];
  try {
    const parsed = JSON.parse(article.contentBody);
    if (Array.isArray(parsed)) {
      return parsed.slice(0, 3).map((s: unknown) => formatFileUrl(String(s)));
    }
  } catch { /* ignore */ }
  return article.coverImage ? [formatFileUrl(article.coverImage)] : [];
}

/** 图集图片总数 */
function getGalleryCount(article: ContentArticle): number {
  if (article.contentType !== 3 || !article.contentBody) return 0;
  try {
    const parsed = JSON.parse(article.contentBody);
    return Array.isArray(parsed) ? parsed.length : 0;
  } catch { /* ignore */ }
  return 0;
}

/** 文件信息：从 content_body JSON 解析 */
function getFileInfo(article: ContentArticle): { name: string; size: string; ext: string } {
  const fallback = { name: '未知文件', size: '', ext: '' };
  if (article.contentType !== 6 || !article.contentBody) return fallback;
  try {
    const parsed = JSON.parse(article.contentBody);
    const name = parsed.fileName || '附件';
    return {
      name,
      size: parsed.fileSize || '',
      ext: name.includes('.') ? name.split('.').pop()!.toUpperCase() : '',
    };
  } catch { /* ignore */ }
  return fallback;
}

/** ====== 徽标 / 格式化 ====== */

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
  padding: 0 $spacing-md 140rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* ====== Sticky 头部 ====== */
.sticky-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: $bg-page;
  padding: $spacing-sm 0;
}
.search-bar {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-sm $spacing-md;
  box-shadow: $shadow-card;
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
  margin-top: $spacing-sm;
  padding: 4rpx 0;
}
.cat-pill {
  display: inline-flex;
  align-items: center;
  height: 56rpx;
  padding: 0 24rpx;
  margin-right: $spacing-sm;
  border-radius: 28rpx;
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

/* ====== 文章列表 ====== */
.list {
  margin-top: $spacing-sm;
}
.article-card {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
  transition: transform 0.15s ease, background 0.15s ease;
}
.article-card:active {
  transform: scale(0.99);
  background: darken(#fff, 2%);
}

/* 徽标 */
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
.badge-top { background: $brand-error-light; color: $brand-error; }
.badge-hot { background: $brand-warning-light; color: $brand-warning; }
.badge-new { background: $brand-primary-light; color: $brand-primary; }

/* 卡片标题（通用） */
.card-title {
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
  margin-top: 10rpx;
}

/* ====== 图文卡片 ====== */
.text-card-body {
  display: flex;
  gap: $spacing-md;
  align-items: flex-start;
}
.text-area {
  flex: 1;
  min-width: 0;
}
.text-area .card-title {
  margin-top: 0;
}
.card-summary {
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
.card-summary.one-line {
  -webkit-line-clamp: 1;
}
.text-thumb {
  width: 120rpx;
  height: 120rpx;
  border-radius: $radius-sm;
  flex-shrink: 0;
  background: $bg-page;
}

/* ====== 视频卡片 ====== */
.video-banner-wrap {
  position: relative;
  width: 100%;
  border-radius: $radius-md;
  overflow: hidden;
}
.video-banner {
  width: 100%;
  height: 360rpx;
  display: block;
}
.video-placeholder {
  background: linear-gradient(135deg, $brand-primary-dark, $brand-primary);
}
.video-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.25);
}
.play-circle {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}
.play-triangle {
  font-size: 32rpx;
  color: #fff;
  margin-left: 6rpx;
}
.type-tag {
  position: absolute;
  bottom: 16rpx;
  left: 16rpx;
  font-size: 20rpx;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}

/* ====== 图集卡片 ====== */
.gallery-strip {
  display: flex;
  gap: $spacing-sm;
}
.thumb-item {
  position: relative;
  flex: 1;
  height: 200rpx;
  border-radius: $radius-sm;
  overflow: hidden;
  background: $bg-page;
}
.thumb-img {
  width: 100%;
  height: 100%;
}
.thumb-more {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
}
.thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx dashed $border-base;
}
.thumb-ph-icon {
  font-size: 28rpx;
  color: $text-placeholder;
}

/* ====== 文件卡片 ====== */
.file-row {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-sm 0;
}
.file-icon-box {
  position: relative;
  width: 72rpx;
  height: 72rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $brand-primary-light;
  border-radius: $radius-sm;
}
.file-icon-text {
  font-size: 36rpx;
}
.file-ext {
  position: absolute;
  bottom: -4rpx;
  left: 50%;
  transform: translateX(-50%);
  font-size: 16rpx;
  line-height: 1;
  padding: 2rpx 8rpx;
  background: $brand-primary;
  color: #fff;
  border-radius: 4rpx;
}
.file-meta-col {
  flex: 1;
  min-width: 0;
}
.file-name-text {
  display: block;
  font-size: 26rpx;
  color: $text-regular;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-sub-text {
  font-size: 22rpx;
  color: $text-placeholder;
}

/* ====== 公共底栏 ====== */
.card-footer {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
  flex-wrap: wrap;
}
.footer-author {
  font-size: 22rpx;
  color: $brand-primary;
}
.footer-views {
  font-size: 22rpx;
  color: $text-placeholder;
}
.footer-date {
  font-size: 22rpx;
  color: $text-placeholder;
}

/* 收藏 */
.fav-btn {
  display: flex;
  align-items: center;
  gap: 4rpx;
  margin-left: auto;
  padding: 8rpx 8rpx;
  min-height: 44rpx;
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
