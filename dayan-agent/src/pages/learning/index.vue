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
      <view
        v-for="item in currentItems"
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
          <text v-if="item.badge" class="cover-badge" :class="'cb-' + item.badgeType">{{ item.badge }}</text>
        </view>
        <!-- 右侧内容 -->
        <view class="course-body">
          <text class="course-title">{{ item.title }}</text>
          <text v-if="item.summary" class="course-summary">{{ item.summary }}</text>
          <view class="course-meta">
            <text class="meta-author">{{ item.author }}</text>
            <text class="meta-sep">·</text>
            <text class="meta-info">{{ item.meta }}</text>
            <text class="meta-sep">·</text>
            <text class="meta-views">{{ formatViews(item.views) }} {{ item.viewsLabel }}</text>
          </view>
        </view>
      </view>

      <!-- 列表底部提示 -->
      <view class="list-end">
        <text class="list-end-text">更多内容持续上线中</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

interface CourseItem {
  id: number;
  title: string;
  summary?: string;
  author: string;
  meta: string;
  views: number;
  viewsLabel: string;
  badge?: string;
  badgeType?: string;
}

const tabs = [
  { key: 'video', label: '视频课程' },
  { key: 'article', label: '图文课程' },
  { key: 'yanming', label: '雁鸣中国' },
] as const;

const activeTab = ref<(typeof tabs)[number]['key']>('video');

/** 视频课程（Mock） */
const videoCourses: CourseItem[] = [
  {
    id: 1,
    title: '泰康幸福年金产品全解析',
    summary: '从保障责任到收益演示，30分钟讲透一款主力年金险',
    author: '王芳 · 资深讲师',
    meta: '28:30',
    views: 12450,
    viewsLabel: '播放',
    badge: '热',
    badgeType: 'hot',
  },
  {
    id: 2,
    title: '客户异议处理实战技巧',
    summary: '5步化解「再考虑考虑」「回去和家人商量」',
    author: '李军 · 销售总监',
    meta: '15:20',
    views: 8730,
    viewsLabel: '播放',
  },
  {
    id: 3,
    title: '养老社区参观体验式营销',
    summary: '如何把一次参观变成一场成交',
    author: '张敏 · 金牌代理',
    meta: '22:10',
    views: 6210,
    viewsLabel: '播放',
    badge: '新',
    badgeType: 'new',
  },
  {
    id: 4,
    title: '高净值客户资产配置与年金逻辑',
    summary: '用底层资产思维打开大单入口',
    author: '陈伟 · 财富顾问',
    meta: '35:40',
    views: 4380,
    viewsLabel: '播放',
  },
];

/** 图文课程（Mock） */
const articleCourses: CourseItem[] = [
  {
    id: 11,
    title: '2026 养老保险税优政策全解读',
    summary: '个人养老金账户抵扣、递延纳税实操指南',
    author: '政策研究组',
    meta: '约 15 分钟',
    views: 21300,
    viewsLabel: '阅读',
    badge: '热',
    badgeType: 'hot',
  },
  {
    id: 12,
    title: '获客话术：从寒暄到需求挖掘的 20 个模板',
    summary: '场景化话术卡片，开口不再难',
    author: '销售训练营',
    meta: '约 10 分钟',
    views: 18500,
    viewsLabel: '阅读',
  },
  {
    id: 13,
    title: 'CCRC 持续照料社区模式科普',
    summary: '独立生活—协助生活—专业护理一站式的底层逻辑',
    author: '行业研究院',
    meta: '约 8 分钟',
    views: 9820,
    viewsLabel: '阅读',
    badge: '新',
    badgeType: 'new',
  },
  {
    id: 14,
    title: '转介绍技巧：让老客户主动为你背书',
    summary: '3 个关键时机 + 1 套信任递进模型',
    author: '资深导师团',
    meta: '约 12 分钟',
    views: 15400,
    viewsLabel: '阅读',
  },
];

/** 雁鸣中国（Mock） */
const yanmingNews: CourseItem[] = [
  {
    id: 21,
    title: '大雁养老与平安养老达成战略合作',
    summary: '共建康养生态，覆盖 30 省 200+ 城市',
    author: '大雁要闻',
    meta: '08-10',
    views: 5670,
    viewsLabel: '阅读',
    badge: '要闻',
    badgeType: 'top',
  },
  {
    id: 22,
    title: '第七届中国养老产业峰会精华回顾',
    summary: '10 位行业领袖观点：银发经济的下一个十年',
    author: '行业动态',
    meta: '08-06',
    views: 3920,
    viewsLabel: '阅读',
    badge: '动态',
    badgeType: 'info',
  },
  {
    id: 23,
    title: '月度之星：代理人单月签单 30 万的秘诀',
    summary: '从 0 到金牌，她只用了这三个动作',
    author: '大雁人物',
    meta: '08-01',
    views: 8100,
    viewsLabel: '阅读',
    badge: '人物',
    badgeType: 'hot',
  },
  {
    id: 24,
    title: '银发经济蓝皮书：2026 养老消费趋势',
    summary: '中高收入长者愿为什么付费？四组数据说清楚',
    author: '趋势洞察',
    meta: '07-28',
    views: 6730,
    viewsLabel: '阅读',
    badge: '洞察',
    badgeType: 'new',
  },
];

/** 当前 Tab 对应的数据 */
const currentItems = computed<CourseItem[]>(() => {
  if (activeTab.value === 'video') return videoCourses;
  if (activeTab.value === 'article') return articleCourses;
  return yanmingNews;
});

/** 封面块图标字 */
const coverIcon = computed(() => {
  if (activeTab.value === 'video') return '视';
  if (activeTab.value === 'article') return '文';
  return '鸣';
});

/** 阅读量 / 播放量格式化：>=10000 显示 x.x 万 */
function formatViews(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万';
  return String(n);
}

function onCourseClick(item: CourseItem) {
  uni.showToast({ title: '《' + item.title.slice(0, 8) + '…》即将上线', icon: 'none' });
}
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
}

.cb-hot { background: rgba(255, 255, 255, 0.95); color: $brand-error; }
.cb-new { background: rgba(255, 255, 255, 0.95); color: $brand-primary; }
.cb-top { background: rgba(255, 255, 255, 0.95); color: $brand-error; }
.cb-info { background: rgba(255, 255, 255, 0.95); color: $brand-info; }

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
