<template>
  <view class="page dy-safe-bottom">
    <template v-if="loading">
      <DySkeleton :rows="8" card />
    </template>

    <template v-else-if="course">
      <!-- 头图 -->
      <view class="hero">
        <image
          v-if="course.coverImage"
          :src="formatFileUrl(course.coverImage)"
          mode="aspectFill"
          class="hero-img"
        />
        <view v-else class="hero-placeholder">
          <text class="hero-placeholder-text">{{ course.courseName.charAt(0) }}</text>
        </view>
        <view v-if="course.isRecommend === 1" class="hero-rec">推荐</view>
      </view>

      <!-- 基本信息 -->
      <view class="info-card">
        <view class="price-row">
          <text v-if="course.isFree === 1" class="price free">免费</text>
          <template v-else>
            <text class="price">¥{{ formatPrice(course.salePrice) }}</text>
            <text v-if="course.originalPrice && course.originalPrice > (course.salePrice || 0)" class="original">
              ¥{{ formatPrice(course.originalPrice) }}
            </text>
          </template>
          <text class="sales">已售 {{ course.salesCount || 0 }}<template v-if="course.ratingAvg"> · 评分 {{ course.ratingAvg }}</template></text>
        </view>
        <text class="name">{{ course.courseName }}</text>
        <view class="meta-grid">
          <view class="meta-item"><text class="meta-label">类型</text><text class="meta-value">{{ courseTypeLabel(course.courseType) }}</text></view>
          <view class="meta-item"><text class="meta-label">讲师</text><text class="meta-value">{{ course.lecturerName || course.lecturer?.lecturerName || '-' }}</text></view>
          <view class="meta-item"><text class="meta-label">课时</text><text class="meta-value">{{ course.totalClass || 0 }} 课时</text></view>
          <view class="meta-item"><text class="meta-label">时长</text><text class="meta-value">{{ durationText(course.totalDuration) || '-' }}</text></view>
          <view v-if="course.validDays" class="meta-item"><text class="meta-label">有效期</text><text class="meta-value">{{ course.validDays }} 天</text></view>
          <view v-if="course.courseStartDate" class="meta-item"><text class="meta-label">开课</text><text class="meta-value">{{ course.courseStartDate }}</text></view>
        </view>
      </view>

      <!-- 课程简介 -->
      <view v-if="course.courseDescription" class="section-card">
        <text class="section-title">课程简介</text>
        <text class="section-text">{{ course.courseDescription }}</text>
      </view>

      <!-- 目标人群 -->
      <view v-if="course.targetAudience" class="section-card">
        <text class="section-title">适合人群</text>
        <view class="audience-row">
          <text class="audience-chip">{{ course.targetAudience }}</text>
        </view>
      </view>

      <!-- 学习目标 -->
      <view v-if="objectiveLines.length" class="section-card">
        <text class="section-title">学习目标</text>
        <view v-for="(line, i) in objectiveLines" :key="i" class="objective-row">
          <text class="objective-index">{{ i + 1 }}</text>
          <text class="objective-text">{{ line }}</text>
        </view>
      </view>

      <!-- 课程大纲 -->
      <view v-if="outlineChapters.length" class="section-card">
        <text class="section-title">课程大纲</text>
        <view v-for="(ch, ci) in outlineChapters" :key="ci" class="chapter">
          <view class="chapter-head">
            <text class="chapter-title">{{ ch.title }}</text>
            <text class="chapter-count">{{ ch.lessons.length }} 课</text>
          </view>
          <view v-for="(ls, li) in ch.lessons" :key="li" class="lesson-row">
            <text class="lesson-title">{{ ls.title }}</text>
            <text v-if="ls.duration" class="lesson-duration">{{ ls.duration }} 分钟</text>
          </view>
        </view>
      </view>

      <!-- 讲师卡片 -->
      <view v-if="course.lecturer" class="section-card lecturer-card">
        <text class="section-title">讲师介绍</text>
        <view class="lecturer-row">
          <image
            v-if="course.lecturer.avatar"
            :src="formatFileUrl(course.lecturer.avatar)"
            mode="aspectFill"
            class="lecturer-avatar"
          />
          <view v-else class="lecturer-avatar lecturer-avatar-placeholder">
            <text>{{ (course.lecturer.lecturerName || '师').charAt(0) }}</text>
          </view>
          <view class="lecturer-info">
            <view class="lecturer-name-row">
              <text class="lecturer-name">{{ course.lecturer.lecturerName }}</text>
              <text v-if="course.lecturer.title" class="lecturer-title">{{ course.lecturer.title }}</text>
            </view>
            <text v-if="course.lecturer.organization" class="lecturer-org">{{ course.lecturer.organization }}</text>
          </view>
        </view>
        <text v-if="course.lecturer.introduction" class="lecturer-intro">{{ course.lecturer.introduction }}</text>
      </view>
    </template>

    <DyEmpty
      v-else-if="!loading"
      text="课程不存在或已下架"
      icon="!"
      color="gray"
      action-text="返回列表"
      @action="goBack"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getCourseDetail } from '@/api/course';
import { formatFileUrl } from '@/utils/file';
import { COURSE_TYPE_LABELS, type Course, type CourseOutlineChapter } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const courseCode = ref('');
const course = ref<Course | null>(null);
const loading = ref(false);

function courseTypeLabel(t?: number): string {
  return (t != null && COURSE_TYPE_LABELS[t]) || '课程';
}

function durationText(minutes?: number): string {
  if (!minutes) return '';
  if (minutes < 60) return `${minutes} 分钟`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m ? `${h}小时${m}分` : `${h}小时`;
}

function formatPrice(price?: number): string {
  if (price == null) return '-';
  return Number(price).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

/** 学习目标按行拆分（中文分号/换行分隔） */
const objectiveLines = computed(() =>
  (course.value?.learningObjectives || '')
    .split(/[；;\n]/)
    .map((s) => s.trim())
    .filter(Boolean)
);

/** 大纲 JSON 容错解析：空/坏 JSON 返回 [] */
const outlineChapters = computed<CourseOutlineChapter[]>(() => {
  const raw = course.value?.courseOutline;
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (ch) => ch && typeof ch.title === 'string' && Array.isArray(ch.lessons)
    ) as CourseOutlineChapter[];
  } catch {
    return [];
  }
});

async function loadDetail() {
  if (!courseCode.value) {
    // 无课程编码（深链参数缺失）：直接落空态，避免卡死 loading
    loading.value = false;
    course.value = null;
    return;
  }
  loading.value = true;
  try {
    course.value = await getCourseDetail(courseCode.value);
  } catch (e) {
    course.value = null;
  } finally {
    loading.value = false;
  }
}

function goBack() {
  uni.navigateBack({
    fail: () => uni.redirectTo({ url: '/pages/learning/courses/index' }),
  });
}

onLoad((query) => {
  courseCode.value = (query?.code as string) || '';
  loadDetail();
});
</script>

<style lang="scss" scoped>
.page {
  padding: $spacing-md $spacing-md 60rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* 头图 */
.hero {
  position: relative;
  height: 320rpx;
  border-radius: $radius-md;
  overflow: hidden;
}
.hero-img {
  width: 100%;
  height: 100%;
}
.hero-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $gradient-blue;
}
.hero-placeholder-text {
  font-size: 96rpx;
  font-weight: bold;
  color: #fff;
  opacity: 0.5;
}
.hero-rec {
  position: absolute;
  top: $spacing-sm;
  right: $spacing-sm;
  font-size: 22rpx;
  color: #fff;
  background: $brand-warning;
  border-radius: 999rpx;
  padding: 4rpx 16rpx;
}

/* 信息卡 */
.info-card {
  margin-top: $spacing-md;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  box-shadow: $shadow-card;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
}
.price {
  font-size: 40rpx;
  font-weight: bold;
  color: $brand-warning;
}
.price.free {
  color: $brand-success;
}
.original {
  font-size: 24rpx;
  color: $text-placeholder;
  text-decoration: line-through;
}
.sales {
  margin-left: auto;
  font-size: 22rpx;
  color: $text-placeholder;
}
.name {
  display: block;
  margin-top: 8rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: $text-primary;
}
.meta-grid {
  margin-top: $spacing-md;
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm $spacing-lg;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.meta-label {
  font-size: 24rpx;
  color: $text-placeholder;
}
.meta-value {
  font-size: 24rpx;
  color: $text-secondary;
}

/* 内容卡片 */
.section-card {
  margin-top: $spacing-md;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  box-shadow: $shadow-card;
}
.section-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: $spacing-sm;
}
.section-text {
  font-size: 26rpx;
  color: $text-secondary;
  line-height: 1.6;
}

/* 人群 chip */
.audience-row {
  display: flex;
  flex-wrap: wrap;
}
.audience-chip {
  font-size: 24rpx;
  color: $brand-primary;
  background: $brand-primary-light;
  border-radius: 999rpx;
  padding: 6rpx 20rpx;
}

/* 学习目标 */
.objective-row {
  display: flex;
  align-items: flex-start;
  gap: $spacing-sm;
  margin-top: 10rpx;
}
.objective-index {
  flex-shrink: 0;
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: $brand-primary-light;
  color: $brand-primary;
  font-size: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.objective-text {
  font-size: 26rpx;
  color: $text-secondary;
  line-height: 36rpx;
}

/* 大纲 */
.chapter {
  margin-top: $spacing-sm;
}
.chapter-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 0;
}
.chapter-title {
  font-size: 26rpx;
  font-weight: 500;
  color: $text-primary;
}
.chapter-count {
  font-size: 22rpx;
  color: $text-placeholder;
}
.lesson-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx $spacing-sm;
  border-top: 2rpx solid $border-light;
}
.lesson-title {
  font-size: 24rpx;
  color: $text-secondary;
}
.lesson-duration {
  font-size: 22rpx;
  color: $text-placeholder;
}

/* 讲师 */
.lecturer-row {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}
.lecturer-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  flex-shrink: 0;
}
.lecturer-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: $brand-primary-light;
  color: $brand-primary;
  font-size: 40rpx;
  font-weight: bold;
}
.lecturer-info {
  flex: 1;
  min-width: 0;
}
.lecturer-name-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}
.lecturer-name {
  font-size: 28rpx;
  font-weight: 500;
  color: $text-primary;
}
.lecturer-title {
  font-size: 22rpx;
  color: $brand-primary;
}
.lecturer-org {
  display: block;
  margin-top: 4rpx;
  font-size: 24rpx;
  color: $text-placeholder;
}
.lecturer-intro {
  display: block;
  margin-top: $spacing-sm;
  font-size: 24rpx;
  color: $text-secondary;
  line-height: 1.6;
}
</style>
