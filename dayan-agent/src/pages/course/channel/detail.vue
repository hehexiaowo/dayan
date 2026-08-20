<template>
  <view class="page dy-safe-bottom">
    <!-- 骨架屏 -->
    <template v-if="loading">
      <DySkeleton :rows="1" card />
      <DySkeleton :rows="6" card />
    </template>

    <!-- 加载失败 -->
    <DyEmpty
      v-else-if="loadError"
      text="加载失败，请检查网络后重试"
      icon="!"
      color="gray"
      action-text="重新加载"
      @action="loadDetail"
    />

    <template v-else-if="content">
      <!-- ===== 视频播放器（有视频时置顶） ===== -->
      <view v-if="videoUrl" class="video-wrap">
        <video
          :src="videoUrl"
          controls
          class="course-video"
          object-fit="contain"
          :poster="content.coverImage ? formatFileUrl(content.coverImage) : ''"
          @timeupdate="onVideoTimeUpdate"
          @ended="onVideoEnded"
        />
      </view>

      <!-- ===== 文章头部（公众号样式：标题 + meta 行） ===== -->
      <view class="article-head">
        <text class="article-title">{{ content.courseName }}</text>
        <view class="article-meta">
          <text v-if="content.author" class="meta-author">{{ content.author }}</text>
          <text class="meta-item">{{ formatDate(content.publishTime) }}</text>
          <text v-if="content.viewCount != null" class="meta-item">{{ formatViews(content.viewCount) }} 阅读</text>
          <text v-if="content.badge" class="meta-badge">{{ content.badge }}</text>
        </view>
        <view v-if="content.courseDescription" class="article-summary">
          <text class="summary-text">{{ content.courseDescription }}</text>
        </view>
      </view>

      <!-- ===== 正文（HTML 富文本 / 纯文本降级） ===== -->
      <view v-if="content.courseBody" class="article-body">
        <rich-text v-if="isHtml" :nodes="content.courseBody" />
        <template v-else>
          <text v-for="(p, i) in paragraphs" :key="i" class="paragraph">{{ p }}</text>
        </template>
      </view>
      <DyEmpty v-else text="正文整理中" icon="文" color="blue" />

      <!-- ===== 底部提示 ===== -->
      <view class="article-foot">
        <text class="foot-text">内容来自养老宝典学习中心</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onUnload } from '@dcloudio/uni-app';
import { getCourseDetail, reportProgress } from '@/api/course';
import { formatFileUrl } from '@/utils/file';
import type { Course } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

/**
 * 渠道课程内容详情页（course_source 与数据库同频）。
 *
 * - 数据走真实课程详情接口（GET /agent-api/courses/{courseCode}，浏览量累加）；
 * - 正文 courseBody 为纯文本，按空行分段渲染（公众号文章样式）；
 * - onLoad 仅解析深链 query，状态只存内存。
 */

const content = ref<Course | null>(null);
const loading = ref(false);
const loadError = ref(false);

/** 视频地址：videoUrl 可能是 OSS key 或完整 URL */
const videoUrl = computed(() => {
  const raw = content.value?.videoUrl;
  return raw ? formatFileUrl(raw) : '';
});

/** 是否包含 HTML 标签（富文本内容） */
const isHtml = computed(() => {
  const body = content.value?.courseBody || '';
  return /<[a-z][\s\S]*>/i.test(body);
});

/** 正文分段：按空行拆分为段，过滤纯空白段（纯文本降级用） */
const paragraphs = computed<string[]>(() => {
  if (!content.value?.courseBody) return [];
  return content.value.courseBody
    .split(/\n\s*\n/)
    .map((p) => p.trim())
    .filter(Boolean);
});

async function loadDetail() {
  if (!content.value?.courseCode) return;
  loading.value = true;
  loadError.value = false;
  try {
    content.value = await getCourseDetail(content.value.courseCode);
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function formatViews(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万';
  return String(n);
}

function formatDate(dt?: string): string {
  if (!dt) return '';
  const norm = dt.replace('T', ' ');
  return norm.length >= 10 ? norm.slice(0, 10) : norm;
}

// ====== 视频进度追踪 ======

let lastReportTime = 0;
let watchedSeconds = 0;

function onVideoTimeUpdate(e: { detail: { currentTime: number; duration: number } }) {
  const now = Date.now();
  watchedSeconds++;

  if (now - lastReportTime >= 30_000 && content.value) {
    lastReportTime = now;
    const minutes = Math.floor(watchedSeconds / 60);
    if (minutes > 0) {
      reportProgress({
        courseCode: content.value.courseCode,
        learnTimeDelta: minutes,
      }).catch(() => {/* 静默失败 */});
      watchedSeconds = watchedSeconds % 60;
    }
  }
}

function onVideoEnded() {
  if (!content.value) return;
  reportProgress({
    courseCode: content.value.courseCode,
    currentLesson: 1,
    learnTimeDelta: Math.ceil(watchedSeconds / 60),
  }).catch(() => {/* 静默失败 */});
}

onLoad((options) => {
  const code = options?.code;
  if (!code) return;
  content.value = { courseCode: code, courseName: '' };
  loadDetail();
});

onUnload(() => {
  if (content.value && watchedSeconds >= 10) {
    reportProgress({
      courseCode: content.value.courseCode,
      learnTimeDelta: Math.ceil(watchedSeconds / 60),
    }).catch(() => {/* 静默失败 */});
  }
});
</script>

<style lang="scss" scoped>

.page {
  padding: $spacing-lg $spacing-md 60rpx;
  min-height: 100vh;
  background: $bg-card;
}

/* ===== 视频播放器 ===== */
.video-wrap {
  margin: -$spacing-lg (-$spacing-md) $spacing-lg;
  border-radius: 0;
  overflow: hidden;
}
.course-video {
  width: 100%;
  height: 420rpx;
}

/* ===== 文章头部 ===== */
.article-head {
  padding-bottom: $spacing-md;
  border-bottom: 1rpx solid $border-light;
}

.article-title {
  font-size: 40rpx;
  font-weight: bold;
  color: $text-primary;
  line-height: 1.5;
}

.article-meta {
  margin-top: $spacing-sm;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8rpx;
}

.meta-author {
  font-size: 24rpx;
  color: $brand-primary;
}

.meta-item {
  font-size: 24rpx;
  color: $text-placeholder;
}

.meta-badge {
  font-size: 20rpx;
  color: $brand-error;
  background: $brand-error-light;
  border-radius: 999rpx;
  padding: 2rpx 14rpx;
}

.article-summary {
  margin-top: $spacing-md;
  padding: $spacing-md;
  background: $bg-page;
  border-radius: $radius-md;
}

.summary-text {
  font-size: 26rpx;
  color: $text-secondary;
  line-height: 1.7;
}

/* ===== 正文 ===== */
.article-body {
  padding-top: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.paragraph {
  font-size: 30rpx;
  color: $text-regular;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* ===== 底部 ===== */
.article-foot {
  margin-top: $spacing-xl;
  padding-top: $spacing-md;
  border-top: 1rpx solid $border-light;
  display: flex;
  justify-content: center;
}

.foot-text {
  font-size: 22rpx;
  color: $text-placeholder;
}
</style>
