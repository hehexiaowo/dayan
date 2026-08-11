<template>
  <view class="page dy-safe-bottom">
    <template v-if="loading">
      <DySkeleton :rows="8" card />
    </template>

    <template v-else-if="article">
      <!-- 文章信息 -->
      <view class="info-card">
        <view v-if="getBadges(article).length" class="badge-row">
          <text
            v-for="badge in getBadges(article)"
            :key="badge.cls"
            class="badge"
            :class="badge.cls"
          >{{ badge.text }}</text>
        </view>
        <text class="title">{{ article.title }}</text>
        <text v-if="article.subtitle" class="subtitle">{{ article.subtitle }}</text>
        <view class="meta-row">
          <view v-if="article.authorName" class="author-chip">
            <text class="author-name">{{ article.authorName }}</text>
          </view>
          <text v-if="article.publishTime" class="meta-date">{{ formatDate(article.publishTime) }}</text>
          <text v-if="article.viewCount != null" class="meta-views">{{ formatCount(article.viewCount) }} 阅读</text>
          <text v-if="article.collectCount" class="meta-views">{{ article.collectCount }} 收藏</text>
        </view>
        <!-- 标签 -->
        <view v-if="tagList.length" class="tag-row">
          <text v-for="tag in tagList" :key="tag" class="tag">#{{ tag }}</text>
        </view>
      </view>

      <!-- 正文 — 按 contentType 分形式渲染 -->
      <view class="body-card">
        <!-- 视频 -->
        <template v-if="article.contentType === 2">
          <video
            v-if="mediaUrl"
            :src="mediaUrl"
            controls
            class="video-player"
            object-fit="contain"
          />
          <text v-if="mediaDesc" class="media-desc">{{ mediaDesc }}</text>
          <text v-if="!mediaUrl" class="body-text">视频加载失败</text>
        </template>

        <!-- 图片集 -->
        <template v-else-if="article.contentType === 3">
          <view class="gallery">
            <image
              v-for="(img, i) in galleryImages"
              :key="i"
              :src="img"
              mode="aspectFill"
              class="gallery-img dy-clickable"
              @click="previewImage(i)"
            />
          </view>
          <text v-if="article.summary" class="gallery-caption">{{ article.summary }}</text>
        </template>

        <!-- 文件 -->
        <template v-else-if="article.contentType === 6">
          <view class="file-card dy-clickable" @click="openFile">
            <text class="file-icon">📄</text>
            <view class="file-info">
              <text class="file-name">{{ fileInfo.name }}</text>
              <text v-if="fileInfo.size" class="file-size">{{ fileInfo.size }}</text>
            </view>
            <text class="file-action">下载</text>
          </view>
          <text v-if="article.summary" class="body-text" style="margin-top: 20rpx;">{{ article.summary }}</text>
        </template>

        <!-- 图文（默认 / type=1） -->
        <template v-else>
          <rich-text v-if="isHtmlBody" :nodes="article.contentBody || ''" />
          <text v-else class="body-text">{{ article.contentBody || article.summary || '暂无内容' }}</text>
        </template>
      </view>

      <!-- 转载来源 -->
      <view v-if="article.sourceUrl" class="source-bar dy-clickable" @click="openSource">
        <text class="source-label">{{ article.sourceType === 2 ? '转载自' : '原文链接' }}</text>
        <text class="source-url one-line">{{ article.sourceUrl }}</text>
        <text class="source-arrow">↗</text>
      </view>

      <!-- 底部操作栏 -->
      <view class="bottom-bar">
        <view class="action-btn fav-btn dy-clickable" @click="toggleFavorite">
          <text class="action-icon" :class="{ favorited: isFavorited }">{{ isFavorited ? '♥' : '♡' }}</text>
          <text class="action-text">{{ isFavorited ? '已收藏' : '收藏' }}</text>
        </view>
        <view class="action-btn share-btn dy-clickable" @click="onShare">
          <text class="action-icon">↗</text>
          <text class="action-text">分享给客户</text>
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
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getContentDetail } from '@/api/content';
import { addFavoriteApi, removeFavoriteApi, getFavoritedCodesApi, TARGET_TYPE } from '@/api/favorite';
import { formatFileUrl } from '@/utils/file';
import type { ContentArticle } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const article = ref<ContentArticle | null>(null);
const loading = ref(true);
const contentCode = ref('');
const isFavorited = ref(false);

interface Badge {
  text: string;
  cls: string;
}

const isHtmlBody = computed(() => {
  const body = article.value?.contentBody;
  return !!body && body.trim().startsWith('<');
});

/** 视频地址（type=2，content_body 存 URL 或 JSON {url,desc}） */
const mediaUrl = computed(() => {
  if (article.value?.contentType !== 2) return '';
  const body = article.value.contentBody || '';
  try {
    const parsed = JSON.parse(body);
    return formatFileUrl(parsed.url || '');
  } catch {
    return formatFileUrl(body.trim());
  }
});

const mediaDesc = computed(() => {
  if (article.value?.contentType !== 2) return '';
  const body = article.value.contentBody || '';
  try {
    const parsed = JSON.parse(body);
    return parsed.desc || '';
  } catch {
    return '';
  }
});

/** 图片集（type=3，content_body 存 JSON 数组） */
const galleryImages = computed<string[]>(() => {
  if (article.value?.contentType !== 3) return [];
  try {
    const parsed = JSON.parse(article.value.contentBody || '[]');
    return Array.isArray(parsed) ? parsed.map((s: unknown) => formatFileUrl(String(s))) : [];
  } catch {
    return [];
  }
});

/** 文件信息（type=6，content_body 存 JSON {fileName,fileUrl,fileSize}） */
const fileInfo = computed(() => {
  if (article.value?.contentType !== 6) return { name: '', url: '', size: '' };
  try {
    const parsed = JSON.parse(article.value.contentBody || '{}');
    return {
      name: parsed.fileName || '未知文件',
      url: formatFileUrl(parsed.fileUrl || ''),
      size: parsed.fileSize || '',
    };
  } catch {
    return { name: '附件', url: '', size: '' };
  }
});

const tagList = computed<string[]>(() => {
  const tags = article.value?.tags;
  if (!tags) return [];
  try {
    const parsed = JSON.parse(tags);
    return Array.isArray(parsed) ? parsed.map(String) : [];
  } catch {
    // 非 JSON，按逗号分隔
    return tags.split(/[,，]/).map((t) => t.trim()).filter(Boolean);
  }
});

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

function getBadges(a: ContentArticle): Badge[] {
  const badges: Badge[] = [];
  if (a.isTop === 1) badges.push({ text: '置顶', cls: 'badge-top' });
  if (a.isRecommend === 1) badges.push({ text: '热', cls: 'badge-hot' });
  if (isNew(a.publishTime)) badges.push({ text: '新', cls: 'badge-new' });
  return badges;
}

function isNew(publishTime?: string): boolean {
  if (!publishTime) return false;
  const diff = Date.now() - new Date(publishTime.replace(/-/g, '/')).getTime();
  return diff < 7 * 24 * 60 * 60 * 1000;
}

async function loadDetail() {
  loading.value = true;
  try {
    article.value = await getContentDetail(contentCode.value);
    // 查收藏状态
    const codes = await getFavoritedCodesApi(TARGET_TYPE.CONTENT);
    isFavorited.value = codes.includes(contentCode.value);
  } catch {
    article.value = null;
  } finally {
    loading.value = false;
  }
}

async function toggleFavorite() {
  const was = isFavorited.value;
  isFavorited.value = !was; // 乐观
  try {
    if (was) {
      await removeFavoriteApi(TARGET_TYPE.CONTENT, contentCode.value);
    } else {
      await addFavoriteApi(TARGET_TYPE.CONTENT, contentCode.value);
    }
  } catch {
    isFavorited.value = was; // 回滚
  }
}

function onShare() {
  uni.showToast({ title: '分享功能开发中', icon: 'none' });
}

/** 图片预览（全屏左右滑动） */
function previewImage(index: number) {
  uni.previewImage({ urls: galleryImages.value, current: galleryImages.value[index] });
}

/** 打开文件 */
function openFile() {
  if (!fileInfo.value.url) {
    uni.showToast({ title: '文件地址无效', icon: 'none' });
    return;
  }
  // #ifdef H5
  window.open(fileInfo.value.url, '_blank');
  // #endif
  // #ifndef H5
  uni.downloadFile({
    url: fileInfo.value.url,
    success: (res) => {
      uni.openDocument({
        filePath: res.tempFilePath,
        fail: () => uni.showToast({ title: '无法打开此文件', icon: 'none' }),
      });
    },
    fail: () => uni.showToast({ title: '下载失败', icon: 'none' }),
  });
  // #endif
}

/** 打开转载原文 */
function openSource() {
  const url = article.value?.sourceUrl;
  if (!url) return;
  // #ifdef H5
  window.open(url, '_blank');
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: '链接已复制', icon: 'none' }),
  });
  // #endif
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

/* 文章信息 */
.info-card {
  background: $bg-card;
  padding: $spacing-lg $spacing-md;
}

/* badge */
.badge-row {
  display: flex;
  gap: 8rpx;
  margin-bottom: $spacing-sm;
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
.badge-hot {
  background: $brand-warning-light;
  color: $brand-warning;
}
.badge-new {
  background: $brand-primary-light;
  color: $brand-primary;
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

/* 标签 */
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
}
.tag {
  font-size: 22rpx;
  color: $brand-primary;
  background: $brand-primary-light;
  padding: 4rpx 14rpx;
  border-radius: $radius-sm;
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

/* 视频播放器 */
.video-player {
  width: 100%;
  border-radius: $radius-md;
  background: #000;
}
.media-desc {
  display: block;
  margin-top: $spacing-sm;
  font-size: 26rpx;
  color: $text-secondary;
  line-height: 1.6;
}

/* 图片集 */
.gallery {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}
.gallery-img {
  width: calc((100% - 12rpx) / 3);
  height: 200rpx;
  border-radius: $radius-sm;
  background: $bg-page;
}
.gallery-caption {
  display: block;
  width: 100%;
  margin-top: $spacing-md;
  font-size: 26rpx;
  color: $text-secondary;
  line-height: 1.6;
}

/* 文件卡片 */
.file-card {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-md;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  background: $bg-page;
}
.file-icon {
  font-size: 48rpx;
}
.file-info {
  flex: 1;
  min-width: 0;
}
.file-name {
  display: block;
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-size {
  font-size: 22rpx;
  color: $text-placeholder;
}
.file-action {
  flex-shrink: 0;
  font-size: 26rpx;
  color: $brand-primary;
  font-weight: 500;
  padding: 8rpx 24rpx;
  border: 2rpx solid $brand-primary;
  border-radius: $radius-sm;
}

/* 转载来源 */
.source-bar {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
  padding: $spacing-md;
  background: $brand-primary-light;
  border-radius: $radius-md;
}
.source-label {
  flex-shrink: 0;
  font-size: 24rpx;
  color: $brand-primary;
  font-weight: 500;
}
.source-url {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  color: $text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.source-arrow {
  flex-shrink: 0;
  font-size: 28rpx;
  color: $brand-primary;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: $spacing-md;
  padding: $spacing-md $spacing-lg;
  padding-bottom: calc(#{$spacing-md} + env(safe-area-inset-bottom));
  background: $bg-card;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
  z-index: 100;
}
.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  height: 80rpx;
  border-radius: $radius-md;
  font-size: 28rpx;
  font-weight: 500;
}
.fav-btn {
  flex: 1;
  background: $brand-info-light;
  color: $text-regular;
}
.share-btn {
  flex: 2;
  background: $gradient-blue;
  color: #fff;
}
.action-icon {
  font-size: 32rpx;
  color: $text-secondary;
}
.action-icon.favorited {
  color: $brand-error;
}
.share-btn .action-icon {
  color: #fff;
}
.action-text {
  font-size: 28rpx;
}
.share-btn .action-text {
  color: #fff;
}
</style>
