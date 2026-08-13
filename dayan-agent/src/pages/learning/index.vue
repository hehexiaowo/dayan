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
        @click="onTabChange(tab.key)"
      >
        <text class="tab-text">{{ tab.label }}</text>
      </view>
    </view>

    <!-- ===== 内容区 ===== -->
    <view class="list">
      <!-- 骨架屏 -->
      <template v-if="loading && !items.length">
        <DySkeleton v-for="i in 3" :key="i" :rows="2" avatar card />
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
      <DyEmpty
        v-else-if="!items.length"
        :text="'暂无' + currentTabLabel + '内容'"
        :icon="coverIcon"
        color="blue"
      />

      <!-- 课程卡片 -->
      <template v-else>
        <view
          v-for="item in items"
          :key="item.id"
          class="course-card dy-clickable"
          @click="onCourseClick(item)"
        >
          <!-- 左侧封面块 -->
          <view class="course-cover" :class="'cover-' + activeTab">
            <text class="cover-icon">{{ coverIcon }}</text>
            <view v-if="activeTab === 'video'" class="cover-play">
              <text class="play-triangle">▶</text>
            </view>
            <text v-if="item.badge" class="cover-badge" :class="badgeClass(item.badge)">{{ item.badge }}</text>
          </view>
          <!-- 右侧内容 -->
          <view class="course-body">
            <text class="course-title">{{ item.title }}</text>
            <text v-if="item.summary" class="course-summary">{{ item.summary }}</text>
            <view class="course-meta">
              <text v-if="item.author" class="meta-author">{{ item.author }}</text>
              <template v-if="item.author && courseMeta(item)">
                <text class="meta-sep">·</text>
              </template>
              <text v-if="courseMeta(item)" class="meta-info">{{ courseMeta(item) }}</text>
              <text v-if="item.viewCount != null" class="meta-sep">·</text>
              <text v-if="item.viewCount != null" class="meta-views">{{ formatViews(item.viewCount) }} {{ viewsLabel }}</text>
            </view>
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
import { ref, computed } from 'vue';
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
import { getLearningContents } from '@/api/learning';
import { LearningCategory } from '@/types';
import type { LearningContent } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

interface TabDef {
  key: 'video' | 'article' | 'yanming';
  label: string;
  category: LearningCategory;
}

const tabs: TabDef[] = [
  { key: 'video', label: '视频课程', category: LearningCategory.VIDEO },
  { key: 'article', label: '图文课程', category: LearningCategory.ARTICLE },
  { key: 'yanming', label: '雁鸣中国', category: LearningCategory.YANMING },
];

const activeTab = ref<TabDef['key']>('video');
const items = ref<LearningContent[]>([]);
const loading = ref(false);
const loadError = ref(false);

const currentTab = computed(() => tabs.find((t) => t.key === activeTab.value) || tabs[0]);
const currentTabLabel = computed(() => currentTab.value.label);

/** 封面块图标字 */
const coverIcon = computed(() => {
  if (activeTab.value === 'video') return '视';
  if (activeTab.value === 'article') return '文';
  return '鸣';
});

/** 播放/阅读量单位 */
const viewsLabel = computed(() => (activeTab.value === 'video' ? '播放' : '阅读'));

/** 课程卡片副信息：视频/图文显示时长，雁鸣显示日期 */
function courseMeta(item: LearningContent): string {
  if (activeTab.value === 'yanming') {
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
    items.value = await getLearningContents(currentTab.value.category);
  } catch {
    loadError.value = true;
    items.value = [];
  } finally {
    loading.value = false;
  }
}

function onTabChange(key: TabDef['key']) {
  if (activeTab.value === key) return;
  activeTab.value = key;
  loadList();
}

function onCourseClick(item: LearningContent) {
  uni.showToast({ title: '《' + item.title.slice(0, 8) + '…》详情即将上线', icon: 'none' });
}

// 每次进入页面刷新当前分类
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

/* ===== 课程卡片 ===== */
.list {
  min-height: 400rpx;
}

.course-card {
  display: flex;
  gap: $spacing-md;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
  transition: transform 0.15s ease;
}

.course-card:active {
  transform: scale(0.99);
}

/* 左侧封面块 */
.course-cover {
  position: relative;
  width: 160rpx;
  height: 160rpx;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cover-video {
  background: linear-gradient(135deg, $brand-primary-dark, $brand-primary);
}

.cover-article {
  background: linear-gradient(135deg, #2dbd5a, #28a745);
}

.cover-yanming {
  background: linear-gradient(135deg, #f0a020, #e8951a);
}

.cover-icon {
  font-size: 52rpx;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.85);
}

/* 视频播放按钮 */
.cover-play {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.play-triangle {
  font-size: 22rpx;
  color: #fff;
  margin-left: 4rpx;
}

/* 封面右上角徽标 */
.cover-badge {
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

/* 右侧内容 */
.course-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.course-title {
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

.course-summary {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $text-secondary;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.course-meta {
  margin-top: auto;
  padding-top: 8rpx;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4rpx;
}

.meta-author {
  font-size: 22rpx;
  color: $brand-primary;
}

.meta-info,
.meta-views {
  font-size: 22rpx;
  color: $text-placeholder;
}

.meta-sep {
  font-size: 22rpx;
  color: $text-placeholder;
  padding: 0 2rpx;
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
