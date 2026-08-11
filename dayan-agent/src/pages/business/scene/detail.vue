<template>
  <view class="page">
    <!-- 骨架屏 -->
    <view v-if="loading">
      <DySkeleton :rows="6" card />
    </view>

    <!-- 加载失败 -->
    <DyEmpty
      v-else-if="loadError"
      text="加载失败"
      icon="败"
      color="orange"
      action-text="重试"
      @action="loadDetail"
    />

    <!-- 详情内容 -->
    <view v-else-if="scene" class="detail-wrap">
      <!-- 封面图 -->
      <view class="hero">
        <image
          v-if="coverUrl"
          class="hero-img"
          :src="coverUrl"
          mode="aspectFill"
        />
        <view v-else class="hero-placeholder">
          <text class="hero-placeholder-text">{{ scene.sceneName?.charAt(0) || '场' }}</text>
        </view>
      </view>

      <!-- 基本信息卡片 -->
      <view class="info-card">
        <text class="scene-title">{{ scene.sceneName }}</text>
        <view class="scene-tags">
          <text class="tag-type">{{ sceneTypeText(scene.sceneType) }}</text>
          <text v-if="scene.isFree === 1" class="tag-free">免费</text>
        </view>
        <view class="price-row">
          <text class="price">
            {{ scene.isFree === 1 ? '免费' : `¥${scene.salePrice || 0}` }}
          </text>
          <text v-if="scene.isFree !== 1" class="price-unit">{{ scene.priceUnit || '元/人' }}</text>
        </view>
        <view class="meta-list">
          <view v-if="scene.address" class="meta-row">
            <text class="meta-icon">📍</text>
            <text class="meta-text">{{ scene.address }}</text>
          </view>
          <view v-if="scene.durationHours" class="meta-row">
            <text class="meta-icon">⏰</text>
            <text class="meta-text">活动时长约 {{ scene.durationHours }} 小时</text>
          </view>
          <view v-if="scene.maxPerson" class="meta-row">
            <text class="meta-icon">👥</text>
            <text class="meta-text">单场上限 {{ scene.maxPerson }} 人</text>
          </view>
        </view>
      </view>

      <!-- 场景亮点 -->
      <view v-if="highlights.length > 0" class="section-card">
        <view class="section-title">场景亮点</view>
        <view class="highlight-list">
          <view v-for="(item, idx) in highlights" :key="idx" class="highlight-row">
            <text class="highlight-dot">•</text>
            <text class="highlight-text">{{ item }}</text>
          </view>
        </view>
      </view>

      <!-- 活动介绍 -->
      <view v-if="scene.sceneDescription" class="section-card">
        <view class="section-title">活动介绍</view>
        <text class="desc-text">{{ scene.sceneDescription }}</text>
      </view>

      <!-- 近期日程 -->
      <view class="section-card">
        <view class="section-title">
          近期日程
          <text v-if="schedules.length > 0" class="title-count">（{{ schedules.length }} 场）</text>
        </view>

        <view v-if="scheduleLoading" class="schedule-loading">
          <DySkeleton :rows="2" />
        </view>

        <DyEmpty
          v-else-if="schedules.length === 0"
          text="暂无可预约日程"
          icon="日"
          color="gray"
        />

        <view v-else class="schedule-list">
          <view
            v-for="sch in schedules"
            :key="sch.id"
            class="schedule-card"
            :class="{ 'schedule-full': isFull(sch) }"
          >
            <view class="schedule-date">
              <text class="date-main">{{ formatDate(sch.scheduleDate) }}</text>
              <text class="date-time">{{ formatTime(sch.startTime) }} - {{ formatTime(sch.endTime) }}</text>
            </view>
            <view class="schedule-info">
              <text class="schedule-price">
                {{ sch.priceOverride != null ? (sch.priceOverride === 0 ? '免费' : `¥${sch.priceOverride}`) : (scene.isFree === 1 ? '免费' : `¥${scene.salePrice || 0}`) }}
              </text>
              <text class="schedule-capacity">
                {{ sch.currentPerson || 0 }}/{{ sch.maxPerson }} 人
                <text v-if="!isFull(sch)" class="capacity-tag">可预约</text>
                <text v-else class="capacity-tag capacity-full">已满</text>
              </text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getSceneDetail, getSceneSchedules } from '@/api/scene';
import { SCENE_TYPE_MAP } from '@/types';
import type { SceneActivity, SceneScheduleItem } from '@/types';
import { formatFileUrl } from '@/utils/file';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const sceneCode = ref('');
const scene = ref<SceneActivity | null>(null);
const loading = ref(false);
const loadError = ref(false);

const schedules = ref<SceneScheduleItem[]>([]);
const scheduleLoading = ref(false);

const coverUrl = computed(() => formatFileUrl(scene.value?.coverImage));

/** 解析亮点字段（后端 highlight 存 JSON 数组字符串，或纯文本） */
const highlights = computed<string[]>(() => {
  const raw = scene.value?.highlight;
  if (!raw) return [];
  // 尝试 JSON 解析
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed.filter((x) => typeof x === 'string' && x.trim());
    }
  } catch {
    // 非 JSON：按换行符切分
  }
  return raw
    .split(/[\n\r,;；，]/)
    .map((s) => s.trim())
    .filter(Boolean);
});

function sceneTypeText(type?: number): string {
  if (type == null) return '其他';
  return SCENE_TYPE_MAP[type] || '其他';
}

function isFull(sch: SceneScheduleItem): boolean {
  return (sch.currentPerson || 0) >= (sch.maxPerson || 0);
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '';
  // "2026-08-15" → "08-15"；兼容其他格式只取月-日
  const m = dateStr.match(/(\d{2,4})[-/](\d{1,2})[-/](\d{1,2})/);
  if (m) {
    return `${m[2].padStart(2, '0')}-${m[3].padStart(2, '0')}`;
  }
  return dateStr;
}

/** "09:00:00" → "09:00"；兼容已是简短格式的情况 */
function formatTime(timeStr?: string): string {
  if (!timeStr) return '';
  return timeStr.slice(0, 5);
}

async function loadDetail() {
  if (!sceneCode.value) return;
  loading.value = true;
  loadError.value = false;
  try {
    scene.value = await getSceneDetail(sceneCode.value);
    // 设置导航栏标题
    uni.setNavigationBarTitle({ title: scene.value?.sceneName || '场景详情' });
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

async function loadSchedules() {
  if (!sceneCode.value) return;
  scheduleLoading.value = true;
  try {
    const res = await getSceneSchedules({ sceneCode: sceneCode.value, size: 20 });
    schedules.value = res?.records || [];
  } catch {
    schedules.value = [];
  } finally {
    scheduleLoading.value = false;
  }
}

onLoad((options) => {
  sceneCode.value = (options?.code as string) || '';
  loadDetail();
  loadSchedules();
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 40rpx;
}

.detail-wrap {
  padding-bottom: 40rpx;
}

/* 封面 */
.hero {
  width: 100%;
  height: 420rpx;
  background: $bg-page;
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
  background: $gradient-orange;
}

.hero-placeholder-text {
  font-size: 120rpx;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.5);
}

/* 信息卡片 */
.info-card {
  background: $bg-card;
  margin: -40rpx $spacing-lg 0;
  padding: $spacing-lg;
  border-radius: $radius-md;
  position: relative;
  z-index: 2;
  box-shadow: $shadow-card;
}

.scene-title {
  font-size: 36rpx;
  font-weight: bold;
  color: $text-primary;
  display: block;
}

.scene-tags {
  display: flex;
  gap: $spacing-xs;
  margin-top: $spacing-sm;
}

.tag-type {
  font-size: 22rpx;
  color: $brand-primary;
  background: $brand-primary-light;
  padding: 4rpx 16rpx;
  border-radius: $radius-sm;
}

.tag-free {
  font-size: 22rpx;
  color: $brand-success;
  background: $brand-success-light;
  padding: 4rpx 16rpx;
  border-radius: $radius-sm;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: $spacing-xs;
  margin-top: $spacing-sm;
}

.price {
  font-size: 40rpx;
  font-weight: bold;
  color: $brand-error;
}

.price-unit {
  font-size: 24rpx;
  color: $text-secondary;
}

.meta-list {
  margin-top: $spacing-md;
}

.meta-row {
  display: flex;
  align-items: flex-start;
  margin-top: $spacing-xs;
}

.meta-icon {
  margin-right: $spacing-xs;
  font-size: 26rpx;
}

.meta-text {
  font-size: 26rpx;
  color: $text-regular;
  flex: 1;
}

/* 区块卡片 */
.section-card {
  background: $bg-card;
  margin: $spacing-md $spacing-lg 0;
  padding: $spacing-lg;
  border-radius: $radius-md;
  box-shadow: $shadow-card;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
  margin-bottom: $spacing-md;
  display: flex;
  align-items: baseline;
}

.title-count {
  font-size: 24rpx;
  font-weight: normal;
  color: $text-secondary;
}

/* 亮点 */
.highlight-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.highlight-row {
  display: flex;
  align-items: flex-start;
}

.highlight-dot {
  color: $brand-primary;
  font-size: 30rpx;
  margin-right: $spacing-xs;
  line-height: 1.2;
}

.highlight-text {
  flex: 1;
  font-size: 28rpx;
  color: $text-regular;
  line-height: 1.6;
}

/* 介绍 */
.desc-text {
  font-size: 28rpx;
  color: $text-regular;
  line-height: 1.7;
  white-space: pre-wrap;
}

/* 日程 */
.schedule-loading {
  margin-top: $spacing-sm;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.schedule-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md;
  background: $bg-page;
  border-radius: $radius-sm;
  border-left: 6rpx solid $brand-primary;
}

.schedule-full {
  border-left-color: $text-placeholder;
  opacity: 0.7;
}

.schedule-date {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.date-main {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}

.date-time {
  font-size: 24rpx;
  color: $text-secondary;
}

.schedule-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4rpx;
}

.schedule-price {
  font-size: 30rpx;
  font-weight: bold;
  color: $brand-error;
}

.schedule-capacity {
  font-size: 24rpx;
  color: $text-secondary;
}

.capacity-tag {
  margin-left: $spacing-xs;
  font-size: 22rpx;
  color: $brand-success;
}

.capacity-full {
  color: $text-placeholder;
}
</style>
