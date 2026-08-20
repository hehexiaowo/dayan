<template>
  <view class="page dy-safe-bottom">
    <!-- 骨架屏 -->
    <template v-if="loading && !records.length">
      <DySkeleton v-for="i in 3" :key="i" :rows="2" card />
    </template>

    <!-- 加载失败 -->
    <DyEmpty
      v-else-if="loadError && !records.length"
      text="加载失败，请检查网络后重试"
      icon="!"
      color="gray"
      action-text="重新加载"
      @action="loadRecords"
    />

    <!-- 空状态 -->
    <DyEmpty
      v-else-if="!records.length"
      text="还没有学习记录，去看看课程吧"
      icon="学"
      color="blue"
      action-text="去学习中心"
      @action="goLearningCenter"
    />

    <!-- 学习记录列表 -->
    <template v-else>
      <view
        v-for="record in records"
        :key="record.id"
        class="record-card dy-clickable"
        @click="goDetail(record.courseCode)"
      >
        <view class="record-head">
          <text class="record-name">{{ record.courseCode }}</text>
          <text class="record-status" :class="statusClass(record.status)">
            {{ statusLabel(record.status) }}
          </text>
        </view>

        <!-- 进度条 -->
        <view class="progress-wrap">
          <view class="progress-bar">
            <view class="progress-fill" :style="{ width: (record.learnProgress || 0) + '%' }" />
          </view>
          <text class="progress-text">{{ record.learnProgress || 0 }}%</text>
        </view>

        <view class="record-meta">
          <text v-if="record.totalLearnTime" class="meta-item">
            累计 {{ formatLearnTime(record.totalLearnTime) }}
          </text>
          <text v-if="record.lastLearnTime" class="meta-item">
            {{ formatDate(record.lastLearnTime) }}
          </text>
        </view>
      </view>

      <!-- 底部提示 -->
      <view class="list-end">
        <text class="list-end-text">共 {{ records.length }} 条学习记录</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
import { getMyLearnRecords } from '@/api/course';
import { LEARN_STATUS_LABELS, type CourseRecordLearn } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

/**
 * 我的学习页面：展示当前 agent 的全部学习记录。
 *
 * - 按最近学习时间倒序；
 * - 显示课程编码、进度条、累计时长、最近学习时间；
 * - 点击跳转课程详情。
 */

const records = ref<CourseRecordLearn[]>([]);
const loading = ref(false);
const loadError = ref(false);

function statusLabel(status?: number): string {
  return (status != null && LEARN_STATUS_LABELS[status]) || '学习中';
}

function statusClass(status?: number): string {
  switch (status) {
    case 2: return 'status-completed';
    case 0: return 'status-refunded';
    case 3: return 'status-expired';
    default: return 'status-learning';
  }
}

function formatLearnTime(minutes: number): string {
  if (minutes < 60) return `${minutes} 分钟`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m ? `${h} 小时 ${m} 分钟` : `${h} 小时`;
}

function formatDate(dt?: string): string {
  if (!dt) return '';
  return dt.replace('T', ' ').slice(0, 16);
}

async function loadRecords() {
  loading.value = true;
  loadError.value = false;
  try {
    records.value = await getMyLearnRecords();
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function goDetail(courseCode: string) {
  uni.navigateTo({ url: `/pages/course/dayan/detail?code=${courseCode}` });
}

function goLearningCenter() {
  uni.switchTab({ url: '/pages/course/index' });
}

onShow(() => {
  loadRecords();
});

onPullDownRefresh(async () => {
  try {
    await loadRecords();
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

.record-card {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
  transition: transform 0.15s ease;
}

.record-card:active {
  transform: scale(0.99);
}

.record-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-sm;
}

.record-name {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-status {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
  margin-left: $spacing-sm;
}

.status-learning {
  color: $brand-primary;
  background: $brand-primary-light;
}

.status-completed {
  color: $brand-success;
  background: rgba($brand-success, 0.1);
}

.status-refunded {
  color: $text-placeholder;
  background: $bg-page;
}

.status-expired {
  color: $brand-warning;
  background: rgba($brand-warning, 0.1);
}

/* 进度条 */
.progress-wrap {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}

.progress-bar {
  flex: 1;
  height: 12rpx;
  background: $bg-page;
  border-radius: 999rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: $gradient-blue;
  border-radius: 999rpx;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 22rpx;
  color: $brand-primary;
  font-weight: 500;
  flex-shrink: 0;
  min-width: 60rpx;
  text-align: right;
}

.record-meta {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.meta-item {
  font-size: 22rpx;
  color: $text-placeholder;
}

/* 底部 */
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
