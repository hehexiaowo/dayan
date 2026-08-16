<template>
  <view class="page dy-safe-bottom">
    <!-- ===== 内容列表（仿公众号历史消息：头条大图 + 左文右缩略图） ===== -->
    <view class="list">
      <!-- 骨架屏 -->
      <template v-if="loading && !items.length">
        <DySkeleton :rows="3" card />
        <DySkeleton v-for="i in 2" :key="i" :rows="2" avatar card />
      </template>

      <!-- 加载失败 -->
      <DyEmpty
        v-else-if="loadError && !items.length"
        text="加载失败，请检查网络后重试"
        icon="!"
        color="gray"
        action-text="重新加载"
        @action="loadList"
      />

      <!-- 空状态 -->
      <DyEmpty v-else-if="!items.length" :text="'暂无' + board.title + '内容'" :icon="board.icon" color="blue" />

      <template v-else>
        <!-- ===== 头条大卡：封面标题叠加 + 白底 meta 行 ===== -->
        <view class="hero-card dy-clickable" @click="onItemClick(hero)">
          <view class="hero-cover" :class="'hero-cover-' + board.key">
            <text v-if="hero.badge" class="hero-badge" :class="badgeClass(hero.badge)">{{ hero.badge }}</text>
            <view v-if="isVideo" class="hero-play">
              <text class="hero-play-triangle">▶</text>
            </view>
            <view class="hero-mask">
              <text class="hero-title">{{ hero.title }}</text>
            </view>
          </view>
          <view class="hero-info">
            <text v-if="hero.summary" class="hero-summary">{{ hero.summary }}</text>
            <view class="hero-meta">
              <text v-if="hero.author" class="hero-meta-author">{{ hero.author }}</text>
              <text v-if="contentMeta(hero)" class="hero-meta-item">{{ contentMeta(hero) }}</text>
              <text v-if="hero.viewCount != null" class="hero-meta-item">{{ formatViews(hero.viewCount) }} {{ viewsLabel }}</text>
            </view>
          </view>
        </view>

        <!-- ===== 列表条目：左文右缩略图（公众号样式） ===== -->
        <view
          v-for="item in restItems"
          :key="item.id"
          class="content-card dy-clickable"
          @click="onItemClick(item)"
        >
          <!-- 左侧文字 -->
          <view class="content-body">
            <text class="content-title">{{ item.title }}</text>
            <text v-if="item.summary" class="content-summary">{{ item.summary }}</text>
            <view class="content-meta">
              <text v-if="item.author" class="meta-author">{{ item.author }}</text>
              <text v-if="contentMeta(item)" class="meta-item">{{ contentMeta(item) }}</text>
              <text v-if="item.viewCount != null" class="meta-item">{{ formatViews(item.viewCount) }} {{ viewsLabel }}</text>
            </view>
          </view>
          <!-- 右侧缩略图 -->
          <view class="content-thumb" :class="'thumb-' + board.key">
            <text class="thumb-char">{{ coverChar(item) }}</text>
            <text v-if="item.badge" class="thumb-badge" :class="badgeClass(item.badge)">{{ item.badge }}</text>
          </view>
        </view>

        <!-- 列表底部提示 -->
        <view class="list-end">
          <text class="list-end-text">更多内容持续上线中</text>
        </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onShow, onPullDownRefresh } from '@dcloudio/uni-app';
import { getLearningContents } from '@/api/learning';
import { LearningCategory } from '@/types';
import type { LearningContent } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

/**
 * 学习中心板块列表页（渠道课程 / 外部课程 / 雁鸣中国共用）。
 *
 * - 布局仿微信公众号历史消息：头条大卡（封面 + 标题叠加 + 白底 meta 行），
 *   其余条目左文右缩略图；无图片字段，封面用板块色渐变 + 标题首字代替；
 * - 板块名由导航栏标题承载（onLoad setNavigationBarTitle），页内不再放说明卡；
 * - 渲染差异按板块：渠道=视频样式（播放钮/播放量），雁鸣=日期，
 *   外部=时长；大雁课程（course_info）在独立课程列表页。
 */

interface BoardMeta {
  key: 'channel' | 'external' | 'yanming';
  title: string;
  icon: string;
  category: LearningCategory;
}

const BOARD_MAP: Record<string, BoardMeta> = {
  '1': { key: 'channel', title: '渠道课程', icon: '渠', category: LearningCategory.CHANNEL },
  '2': { key: 'external', title: '外部课程', icon: '外', category: LearningCategory.EXTERNAL },
  '3': { key: 'yanming', title: '雁鸣中国', icon: '鸣', category: LearningCategory.YANMING },
};

const board = ref<BoardMeta>(BOARD_MAP['1']);
const items = ref<LearningContent[]>([]);
const loading = ref(false);
const loadError = ref(false);

/** 头条（排序即权重：sortOrder/publishTime 倒序的第一条） */
const hero = computed<LearningContent>(() => items.value[0]);
const restItems = computed<LearningContent[]>(() => items.value.slice(1));

/** 渠道板块按视频样式渲染（存量内容以视频课为主） */
const isVideo = computed(() => board.value.key === 'channel');

const viewsLabel = computed(() => (isVideo.value ? '播放' : '阅读'));

/** 缩略图首字：标题差异化的视觉锚点 */
function coverChar(item: LearningContent): string {
  return (item.title || '?').charAt(0);
}

/** meta 副信息：渠道/外部=时长，雁鸣=日期 */
function contentMeta(item: LearningContent): string {
  if (board.value.key === 'yanming') {
    return item.publishTime ? formatDate(item.publishTime) : '';
  }
  return item.duration || '';
}

/** 阅读量 / 播放量格式化：>=10000 显示 x.x 万 */
function formatViews(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万';
  return String(n);
}

/** 日期 MM-DD */
function formatDate(dt?: string): string {
  if (!dt) return '';
  const norm = dt.replace('T', ' ');
  return norm.length >= 10 ? norm.slice(5, 10) : norm;
}

/** 角标配色 */
function badgeClass(badge?: string): string {
  if (!badge) return 'cb-default';
  if (['热', '要闻', '人物'].includes(badge)) return 'cb-hot';
  if (['新', '洞察'].includes(badge)) return 'cb-new';
  return 'cb-info';
}

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    items.value = await getLearningContents(board.value.category);
  } catch {
    loadError.value = true;
    items.value = [];
  } finally {
    loading.value = false;
  }
}

function onItemClick(item: LearningContent) {
  if (!item.contentCode) return;
  uni.navigateTo({ url: `/pages/learning/board/detail?code=${item.contentCode}` });
}

// 深链入口：onLoad 仅解析 query，状态只存内存
onLoad((options) => {
  const cat = options?.category;
  if (cat && BOARD_MAP[cat]) {
    board.value = BOARD_MAP[cat];
  }
  uni.setNavigationBarTitle({ title: board.value.title });
});

onShow(() => {
  loadList();
});

onPullDownRefresh(async () => {
  try {
    await loadList();
  } finally {
    uni.stopPullDownRefresh();
  }
});
</script>

<style lang="scss" scoped>

.page {
  padding: $spacing-md $spacing-md 60rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* ===== 头条大卡 ===== */
.list {
  min-height: 400rpx;
}

.hero-card {
  background: $bg-card;
  border-radius: $radius-md;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
  overflow: hidden;
  transition: transform 0.15s ease;
}

.hero-card:active {
  transform: scale(0.99);
}

.hero-cover {
  position: relative;
  height: 320rpx;
  display: flex;
  align-items: flex-end;
}

.hero-cover-channel {
  background: $gradient-green;
}

.hero-cover-external {
  background: $gradient-purple;
}

.hero-cover-yanming {
  background: $gradient-orange;
}

/* 标题叠加遮罩（保证白字对比度，仿公众号头图标题） */
.hero-mask {
  width: 100%;
  padding: $spacing-lg $spacing-md $spacing-sm;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.55));
}

.hero-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #fff;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.hero-badge {
  position: absolute;
  top: $spacing-sm;
  left: $spacing-sm;
  font-size: 20rpx;
  line-height: 1;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.95);
  z-index: 1;
}

.hero-play {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -80%);
  width: 84rpx;
  height: 84rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.3);
  border: 3rpx solid rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-play-triangle {
  font-size: 30rpx;
  color: #fff;
  margin-left: 6rpx;
}

/* 白底 meta 行 */
.hero-info {
  padding: $spacing-sm $spacing-md $spacing-md;
}

.hero-summary {
  display: block;
  font-size: 24rpx;
  color: $text-secondary;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-meta {
  margin-top: 8rpx;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8rpx;
}

.hero-meta-author {
  font-size: 22rpx;
  color: $brand-primary;
}

.hero-meta-item {
  font-size: 22rpx;
  color: $text-placeholder;
}

/* ===== 列表条目：左文右缩略图 ===== */
.content-card {
  display: flex;
  gap: $spacing-md;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
  transition: transform 0.15s ease;
}

.content-card:active {
  transform: scale(0.99);
}

.content-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.content-title {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.content-summary {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $text-secondary;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-meta {
  margin-top: auto;
  padding-top: 8rpx;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8rpx;
}

.meta-author {
  font-size: 22rpx;
  color: $brand-primary;
}

.meta-item {
  font-size: 22rpx;
  color: $text-placeholder;
}

/* 右侧缩略图 */
.content-thumb {
  position: relative;
  width: 168rpx;
  height: 168rpx;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.thumb-channel {
  background: $gradient-green;
}

.thumb-external {
  background: $gradient-purple;
}

.thumb-yanming {
  background: $gradient-orange;
}

.thumb-char {
  font-size: 56rpx;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.85);
}

.thumb-badge {
  position: absolute;
  top: 0;
  right: 0;
  font-size: 18rpx;
  line-height: 1;
  padding: 4rpx 8rpx;
  border-radius: 0 $radius-sm 0 $radius-sm;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.95);
}

.cb-hot { color: $brand-error; }
.cb-new { color: $brand-primary; }
.cb-info { color: $brand-info; }
.cb-default { color: $text-secondary; }

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
