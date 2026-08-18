<template>
  <view class="page dy-safe-bottom">
    <!-- ===== 课程列表（头条大卡 + 左文右图条目，公众号样式） ===== -->
    <view class="list">
      <template v-if="loading && !courses.length">
        <DySkeleton :rows="3" card />
        <DySkeleton v-for="i in 2" :key="i" :rows="2" avatar card />
      </template>

      <DyEmpty
        v-else-if="loadError && !courses.length"
        text="加载失败，请检查网络后重试"
        icon="!"
        color="gray"
        action-text="重新加载"
        @action="loadList"
      />

      <DyEmpty v-else-if="!courses.length" text="暂无相关课程" icon="雁" color="blue" />

      <template v-else>
        <!-- ===== 头条大卡：封面标题叠加 + 白底信息行 ===== -->
        <view class="hero-card dy-clickable" @click="goDetail(hero.courseCode)">
          <view class="hero-cover">
            <image
              v-if="hero.coverImage"
              :src="formatFileUrl(hero.coverImage)"
              mode="aspectFill"
              class="hero-img"
            />
            <text v-else class="hero-char">{{ (hero.courseName || '?').charAt(0) }}</text>
            <text v-if="hero.isRecommend === 1" class="hero-badge">荐</text>
            <view class="hero-mask">
              <text class="hero-title">{{ hero.courseName }}</text>
            </view>
          </view>
          <view class="hero-info">
            <view class="hero-meta">
              <text class="type-tag">{{ courseTypeLabel(hero.courseType) }}</text>
              <text v-if="hero.lecturerName" class="meta-lecturer">{{ hero.lecturerName }}</text>
              <text class="meta-ext">{{ hero.totalClass || 0 }} 课时<template v-if="hero.totalDuration"> · {{ durationText(hero.totalDuration) }}</template></text>
            </view>
            <view class="hero-price-row">
              <view class="price-wrap">
                <text v-if="hero.isFree === 1" class="price free">免费</text>
                <template v-else>
                  <text class="price">¥{{ formatPrice(hero.salePrice) }}</text>
                  <text v-if="hero.originalPrice && hero.originalPrice > (hero.salePrice || 0)" class="original">¥{{ formatPrice(hero.originalPrice) }}</text>
                </template>
              </view>
              <text class="sales">已售 {{ hero.salesCount || 0 }}</text>
            </view>
          </view>
        </view>

        <!-- ===== 其余条目：左文右图 ===== -->
        <view
          v-for="course in restCourses"
          :key="course.courseCode"
          class="course-card dy-clickable"
          @click="goDetail(course.courseCode)"
        >
        <!-- 左侧文字 -->
        <view class="info">
          <view class="name-row">
            <text class="name">{{ course.courseName }}</text>
            <text v-if="course.isRecommend === 1" class="rec-badge">荐</text>
          </view>
          <view class="meta-row">
            <text class="type-tag">{{ courseTypeLabel(course.courseType) }}</text>
            <text v-if="course.lecturerName" class="meta-lecturer">{{ course.lecturerName }}</text>
          </view>
          <text class="meta-ext">{{ course.totalClass || 0 }} 课时<template v-if="course.totalDuration"> · {{ durationText(course.totalDuration) }}</template></text>
          <view class="bottom-row">
            <view class="price-wrap">
              <text v-if="course.isFree === 1" class="price free">免费</text>
              <template v-else>
                <text class="price">¥{{ formatPrice(course.salePrice) }}</text>
                <text v-if="course.originalPrice && course.originalPrice > (course.salePrice || 0)" class="original">¥{{ formatPrice(course.originalPrice) }}</text>
              </template>
            </view>
            <text class="sales">已售 {{ course.salesCount || 0 }}</text>
          </view>
        </view>
        <!-- 右侧封面 -->
        <view class="cover">
          <image
            v-if="course.coverImage"
            :src="formatFileUrl(course.coverImage)"
            mode="aspectFill"
            class="cover-img"
          />
          <view v-else class="cover-placeholder">
            <text class="placeholder-text">{{ (course.courseName || '?').charAt(0) }}</text>
          </view>
        </view>
      </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
import { getCourses } from '@/api/course';
import { formatFileUrl } from '@/utils/file';
import { COURSE_TYPE_LABELS, CourseSource, type Course } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

/**
 * 大雁课程列表页（course_source=1 平台自研课程）。
 *
 * - 布局仿公众号历史消息：首门课程为头条大卡（封面 + 标题叠加 + 白底
 *   类型/讲师/价格行），其余条目左文右图；
 * - 封面缺省时用品牌渐变 + 课程名首字占位；
 * - 仅上架课程（后端过滤 courseStatus=2）且固定板块 course_source=1。
 */

const courses = ref<Course[]>([]);
const loading = ref(false);
const loadError = ref(false);

/** 头条课程（列表第一条，后端按推荐/排序返回） */
const hero = computed<Course>(() => courses.value[0]);
const restCourses = computed<Course[]>(() => courses.value.slice(1));

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

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    courses.value = await getCourses(undefined, CourseSource.SELF);
  } catch (e) {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function goDetail(courseCode: string) {
  uni.navigateTo({ url: `/pages/course/dayan/detail?code=${courseCode}` });
}

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
  background: $gradient-blue;
}

.hero-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

/* 无封面占位：品牌渐变 + 课程名首字 */
.hero-char {
  position: absolute;
  top: 44%;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 110rpx;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.9);
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
  color: #fff;
  background: $brand-warning;
  z-index: 1;
}

/* 标题叠加遮罩（保证白字对比度） */
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

/* 白底信息行 */
.hero-info {
  padding: $spacing-sm $spacing-md $spacing-md;
}

.hero-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.hero-price-row {
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1rpx solid $border-light;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
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
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
  transition: transform 0.15s ease;
}
.course-card:active {
  transform: scale(0.99);
}
.cover {
  width: 220rpx;
  height: 165rpx;
  border-radius: $radius-sm;
  overflow: hidden;
  flex-shrink: 0;
  background: $bg-page;
}
.cover-img {
  width: 100%;
  height: 100%;
}
.cover-placeholder {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $gradient-blue;
}
.placeholder-text {
  font-size: 64rpx;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.9);
}
.placeholder-rec {
  position: absolute;
  top: 0;
  right: 0;
  font-size: 20rpx;
  line-height: 1;
  padding: 4rpx 10rpx;
  border-radius: 0 $radius-sm 0 $radius-sm;
  color: #fff;
  background: $brand-warning;
}
.info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  min-width: 0;
}
.name-row {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
}
.name {
  flex: 1;
  font-size: 28rpx;
  font-weight: 500;
  color: $text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rec-badge {
  flex-shrink: 0;
  font-size: 20rpx;
  color: #fff;
  background: $brand-warning;
  border-radius: $radius-sm;
  padding: 2rpx 8rpx;
}
.meta-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}
.type-tag {
  flex-shrink: 0;
  font-size: 20rpx;
  color: $brand-primary;
  background: $brand-primary-light;
  border-radius: 999rpx;
  padding: 2rpx 12rpx;
}
.meta-lecturer {
  font-size: 24rpx;
  color: $text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta-ext {
  font-size: 24rpx;
  color: $text-placeholder;
}
.bottom-row {
  margin-top: auto;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.price-wrap {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}
.price {
  font-size: 30rpx;
  font-weight: bold;
  color: $brand-warning;
}
.price.free {
  color: $brand-success;
}
.original {
  font-size: 22rpx;
  color: $text-placeholder;
  text-decoration: line-through;
}
.sales {
  font-size: 22rpx;
  color: $text-placeholder;
}
</style>
