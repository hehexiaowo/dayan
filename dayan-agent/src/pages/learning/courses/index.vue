<template>
  <view class="page dy-safe-bottom">
    <!-- 类型筛选 chips -->
    <view class="filter-bar">
      <view
        v-for="opt in typeFilters"
        :key="opt.value"
        class="chip dy-clickable"
        :class="{ active: activeType === opt.value }"
        @click="onFilterChange(opt.value)"
      >
        <text class="chip-text">{{ opt.label }}</text>
      </view>
    </view>

    <!-- 课程列表 -->
    <view class="list">
      <template v-if="loading && !courses.length">
        <DySkeleton v-for="i in 3" :key="i" :rows="2" avatar card />
      </template>

      <DyEmpty
        v-else-if="loadError && !courses.length"
        text="加载失败，请检查网络后重试"
        icon="!"
        color="gray"
        action-text="重新加载"
        @action="loadList"
      />

      <DyEmpty v-else-if="!courses.length" text="暂无相关课程" icon="课" color="blue" />

      <view
        v-else
        v-for="course in courses"
        :key="course.courseCode"
        class="course-card dy-clickable"
        @click="goDetail(course.courseCode)"
      >
        <view class="cover">
          <image
            v-if="course.coverImage"
            :src="formatFileUrl(course.coverImage)"
            mode="aspectFill"
            class="cover-img"
          />
          <view v-else class="cover-placeholder">
            <text class="placeholder-text">{{ course.courseName.charAt(0) }}</text>
          </view>
        </view>
        <view class="info">
          <view class="name-row">
            <text class="name">{{ course.courseName }}</text>
            <text v-if="course.isRecommend === 1" class="rec-badge">荐</text>
          </view>
          <text class="meta">{{ courseTypeLabel(course.courseType) }}<template v-if="course.lecturerName"> · {{ course.lecturerName }}</template></text>
          <text class="meta">{{ course.totalClass || 0 }} 课时<template v-if="course.totalDuration"> · {{ durationText(course.totalDuration) }}</template></text>
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
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getCourses } from '@/api/course';
import { formatFileUrl } from '@/utils/file';
import { COURSE_TYPE_LABELS, type Course } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const typeFilters = [
  { label: '全部', value: 0 },
  { label: '线上录播', value: 1 },
  { label: '线上直播', value: 2 },
  { label: '线下课程', value: 3 },
  { label: '混合课程', value: 4 },
];

const activeType = ref(0);
const courses = ref<Course[]>([]);
const loading = ref(false);
const loadError = ref(false);

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
    courses.value = await getCourses(activeType.value || undefined);
  } catch (e) {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function onFilterChange(value: number) {
  if (activeType.value === value) return;
  activeType.value = value;
  loadList();
}

function goDetail(courseCode: string) {
  uni.navigateTo({ url: `/pages/learning/courses/detail?code=${courseCode}` });
}

onShow(() => {
  loadList();
});
</script>

<style lang="scss" scoped>
.page {
  padding: $spacing-md $spacing-md 60rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* 筛选 chips */
.filter-bar {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}
.chip {
  padding: 8rpx 24rpx;
  border-radius: 999rpx;
  background: $bg-card;
  border: 2rpx solid $border-base;
}
.chip.active {
  background: $gradient-blue;
  border-color: transparent;
}
.chip-text {
  font-size: 24rpx;
  color: $text-secondary;
}
.chip.active .chip-text {
  color: #fff;
}

/* 课程卡片 */
.course-card {
  display: flex;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
}
.cover {
  width: 200rpx;
  height: 150rpx;
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
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $brand-primary-light;
}
.placeholder-text {
  font-size: 56rpx;
  font-weight: bold;
  color: $brand-primary;
  opacity: 0.4;
}
.info {
  flex: 1;
  margin-left: $spacing-md;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
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
.meta {
  font-size: 24rpx;
  color: $text-secondary;
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
