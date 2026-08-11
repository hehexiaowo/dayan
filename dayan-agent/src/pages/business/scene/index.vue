<template>
  <view class="page">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-input-wrap">
        <text class="search-icon">搜</text>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索场景名称"
          placeholder-class="search-placeholder"
          confirm-type="search"
          @confirm="loadList"
        />
        <text v-if="keyword" class="search-clear" @click="keyword = ''">×</text>
      </view>
    </view>

    <!-- 列表区 -->
    <view class="list-wrap">
      <!-- 骨架屏 -->
      <view v-if="loading && scenes.length === 0">
        <DySkeleton v-for="i in 3" :key="i" :rows="3" card />
      </view>

      <!-- 空状态 -->
      <DyEmpty
        v-else-if="scenes.length === 0 && !loadError"
        text="暂无场景活动"
        icon="场"
        color="orange"
      />

      <!-- 加载失败 -->
      <DyEmpty
        v-else-if="loadError"
        text="加载失败"
        icon="败"
        color="orange"
        action-text="重试"
        @action="loadList"
      />

      <!-- 场景列表 -->
      <view v-else>
        <view
          v-for="scene in filteredScenes"
          :key="scene.sceneCode"
          class="scene-card dy-clickable"
          @click="goDetail(scene.sceneCode)"
        >
          <!-- 封面图 -->
          <view class="card-cover">
            <image
              v-if="coverUrl(scene.coverImage)"
              class="cover-img"
              :src="coverUrl(scene.coverImage)!"
              mode="aspectFill"
            />
            <view v-else class="cover-placeholder">
              <text class="cover-placeholder-text">{{ scene.sceneName?.charAt(0) || '场' }}</text>
            </view>
          </view>

          <!-- 信息区 -->
          <view class="card-body">
            <text class="card-title">{{ scene.sceneName }}</text>
            <view class="card-tags">
              <text class="tag-type">{{ sceneTypeText(scene.sceneType) }}</text>
              <text v-if="scene.isFree === 1" class="tag-free">免费</text>
            </view>
            <view class="card-info">
              <text v-if="scene.address" class="info-text">📍 {{ scene.address }}</text>
              <text v-else class="info-text info-muted">地址待补充</text>
            </view>
            <view class="card-bottom">
              <text class="card-price">
                {{ scene.isFree === 1 ? '免费' : `¥${scene.salePrice || 0}` }}
                <text v-if="scene.isFree !== 1" class="price-unit">{{ scene.priceUnit || '元/人' }}</text>
              </text>
              <text v-if="scene.durationHours" class="card-duration">⏰ {{ scene.durationHours }}小时</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getSceneList } from '@/api/scene';
import { SCENE_TYPE_MAP } from '@/types';
import type { SceneActivity } from '@/types';
import { formatFileUrl } from '@/utils/file';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const keyword = ref('');
const scenes = ref<SceneActivity[]>([]);
const loading = ref(false);
const loadError = ref(false);

const filteredScenes = computed(() => {
  const kw = keyword.value.trim();
  if (!kw) return scenes.value;
  return scenes.value.filter((s) => s.sceneName?.includes(kw));
});

function sceneTypeText(type?: number): string {
  if (type == null) return '其他';
  return SCENE_TYPE_MAP[type] || '其他';
}

function coverUrl(cover?: string): string | undefined {
  const url = formatFileUrl(cover);
  return url || undefined;
}

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    const res = await getSceneList({ size: 50 });
    scenes.value = res?.records || [];
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function goDetail(sceneCode: string) {
  uni.navigateTo({ url: '/pages/business/scene/detail?code=' + sceneCode });
}

onShow(() => {
  loadList();
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

/* 搜索栏 */
.search-bar {
  padding: $spacing-sm $spacing-lg;
  background: $bg-card;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  background: $bg-page;
  border-radius: $radius-md;
  padding: 12rpx 24rpx;
}

.search-icon {
  font-size: 26rpx;
  color: $text-placeholder;
  margin-right: $spacing-sm;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: $text-primary;
}

.search-placeholder {
  color: $text-placeholder;
  font-size: 28rpx;
}

.search-clear {
  font-size: 36rpx;
  color: $text-placeholder;
  padding: 0 8rpx;
}

/* 列表 */
.list-wrap {
  padding: $spacing-sm $spacing-lg;
}

/* 场景卡片 */
.scene-card {
  background: $bg-card;
  border-radius: $radius-md;
  margin-bottom: $spacing-md;
  overflow: hidden;
  box-shadow: $shadow-card;
}

.card-cover {
  width: 100%;
  height: 280rpx;
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
  background: $gradient-orange;
}

.cover-placeholder-text {
  font-size: 80rpx;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.5);
}

.card-body {
  padding: $spacing-md $spacing-lg;
}

.card-title {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
  display: block;
}

.card-tags {
  display: flex;
  gap: $spacing-xs;
  margin-top: $spacing-xs;
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

.card-info {
  margin-top: $spacing-sm;
}

.info-text {
  font-size: 24rpx;
  color: $text-regular;
}

.info-muted {
  color: $text-placeholder;
}

.card-bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: $spacing-sm;
}

.card-price {
  font-size: 34rpx;
  font-weight: bold;
  color: $brand-error;
}

.price-unit {
  font-size: 22rpx;
  font-weight: normal;
  color: $text-secondary;
}

.card-duration {
  font-size: 24rpx;
  color: $text-secondary;
}
</style>
