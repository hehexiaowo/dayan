<template>
  <view class="page">
    <!-- 步骤进度 -->
    <ArticleRewriteSteps :current="5" />

    <!-- 加载中 -->
    <DyLoadingState v-if="loading" :text="loadingText" hint="AI 正在生成配图，请耐心等待..." />

    <template v-if="imageData">
      <!-- 方案切换标签 -->
      <view v-if="imageData.results && imageData.results.length > 1" class="plan-tabs">
        <text
          v-for="item in imageData.results"
          :key="item.planId"
          class="plan-tab dy-clickable"
          :class="{ on: currentPlanId === item.planId }"
          @click="currentPlanId = item.planId"
        >{{ getPlanName(item.planId) }}</text>
      </view>

      <!-- 主图选择 -->
      <view class="image-card dy-card">
        <view class="dy-section-title">主图（封面图）</view>
        <view class="main-image-list">
          <view
            v-for="(img, index) in currentImageResult?.mainImage?.candidates"
            :key="img.imageId"
            class="main-image-item dy-clickable"
            :class="{ selected: img.selected }"
            @click="selectMainImage(index)"
          >
            <image :src="img.url" mode="aspectFill" class="main-image" />
            <view v-if="img.selected" class="selected-tag">
              <text class="selected-tag-text">已选</text>
            </view>
          </view>
        </view>
        <view class="custom-upload">
          <text class="custom-upload-text">或上传自定义主图</text>
          <view class="btn-secondary btn-disabled" @click="onUploadCustom">
            <text class="btn-secondary-text">上传图片</text>
          </view>
        </view>
      </view>

      <!-- 文章内图 -->
      <view class="image-card dy-card" v-if="currentImageResult?.bodyImages?.length">
        <view class="dy-section-title">文章内图</view>
        <view class="body-image-list">
          <view
            v-for="img in currentImageResult.bodyImages"
            :key="img.imageId"
            class="body-image-item"
          >
            <image :src="img.url" mode="aspectFill" class="body-image" />
            <text class="body-image-position">第{{ img.position }}段后</text>
          </view>
        </view>
      </view>

      <!-- 跳过提示 -->
      <view class="skip-hint">
        <text class="skip-text">暂不配图？</text>
        <text class="skip-link dy-clickable" @click="onSkip">跳过此步骤</text>
      </view>

      <!-- 操作按钮 -->
      <view class="action-bar">
        <view class="btn-primary dy-clickable" @click="onNext">
          <text class="btn-primary-text">下一步：完成</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getRewriteDetail, generateImages, saveMainImage } from '@/api/toolArticleRewrite'
import type { ArticleRewriteRecord, ImageResult, ImageResultItem, SummaryAnalysis } from '@/types/toolArticleRewrite'
import { getCurrentPlanResult, getErrMsg } from '@/types/toolArticleRewrite'
import ArticleRewriteSteps from '@/components/ArticleRewriteSteps/ArticleRewriteSteps.vue'
import DyLoadingState from '@/components/DyLoadingState/DyLoadingState.vue'

const record = ref<ArticleRewriteRecord | null>(null)
const currentPlanId = ref('')
const loading = ref(false)
const loadingText = ref('加载中...')

// 解析 imageResult 字符串为对象
const imageData = computed<ImageResult | null>(() => {
  if (!record.value?.imageResult) return null
  if (typeof record.value.imageResult === 'string') {
    try { return JSON.parse(record.value.imageResult) } catch { return null }
  }
  return record.value.imageResult as ImageResult
})

// 解析 summaryAnalysis 字符串为对象
const summaryData = computed<SummaryAnalysis | null>(() => {
  if (!record.value?.summaryAnalysis) return null
  if (typeof record.value.summaryAnalysis === 'string') {
    try { return JSON.parse(record.value.summaryAnalysis) } catch { return null }
  }
  return record.value.summaryAnalysis as SummaryAnalysis
})

const currentImageResult = computed<ImageResultItem | undefined>(() => {
  return getCurrentPlanResult(imageData.value?.results, currentPlanId.value)
})

onLoad(async (options) => {
  if (!options?.id) return

  const id = Number(options.id)
  loading.value = true
  loadingText.value = '正在加载...'
  try {
    record.value = await getRewriteDetail(id)

    // 如果还没有生成配图，则生成
    if (record.value.status === 'AUDITED') {
      loadingText.value = 'AI 正在生成配图...'
      record.value = await generateImages(id)
    } else if (record.value.status === 'IMAGED' || record.value.status === 'READY') {
      // 已有配图结果，提示是否重新生成
      uni.showModal({
        title: '重新生成配图',
        content: '已存在配图结果，是否重新生成？',
        confirmText: '重新生成',
        cancelText: '使用现有',
        success: async (res) => {
          if (res.confirm) {
            loading.value = true
            loadingText.value = 'AI 正在重新生成配图...'
            try {
              record.value = await generateImages(id)
            } catch (e: any) {
              uni.showToast({ title: getErrMsg(e, '重新生成失败'), icon: 'none' })
            } finally {
              loading.value = false
            }
          } else {
            loading.value = false
          }
          currentPlanId.value = imageData.value?.currentPlanId || ''
        },
      })
      return
    }

    currentPlanId.value = imageData.value?.currentPlanId || ''
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '加载失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
})

function getPlanName(planId: string) {
  const plan = summaryData.value?.rewritePlans?.find((p) => p.planId === planId)
  return plan?.name || planId
}

async function selectMainImage(index: number) {
  if (!currentImageResult.value?.mainImage?.candidates || !record.value) return

  const img = currentImageResult.value.mainImage.candidates[index]
  if (!img) return

  // 本地先更新选中状态
  currentImageResult.value.mainImage.candidates.forEach((c, i) => {
    c.selected = i === index
  })

  // 调用 API 持久化选择
  try {
    await saveMainImage(record.value.id, currentPlanId.value, img.imageId)
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '保存选择失败'), icon: 'none' })
  }
}

function onUploadCustom() {
  // 功能暂未开放，按钮禁用
  uni.showToast({ title: '自定义上传暂未开放', icon: 'none' })
}

function onSkip() {
  if (!record.value) return
  uni.navigateTo({
    url: `/pages/acquisition/tools/article-rewrite/step-publish?id=${record.value.id}`,
  })
}

function onNext() {
  if (!record.value) return
  uni.navigateTo({
    url: `/pages/acquisition/tools/article-rewrite/step-publish?id=${record.value.id}`,
  })
}
</script>

<style scoped lang="scss">
.page {
  padding: $spacing-md;
  padding-bottom: 40rpx;
  background: $bg-page;
  min-height: 100vh;
}

.plan-tabs {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
  overflow-x: auto;
}

.plan-tab {
  flex-shrink: 0;
  padding: 12rpx 24rpx;
  background: #fff;
  border-radius: $radius-md;
  font-size: 26rpx;
  color: $text-secondary;
  border: 2rpx solid transparent;

  &.on {
    color: $brand-primary;
    border-color: $brand-primary;
    background: rgba($brand-primary, 0.05);
  }
}

.image-card {
  padding: $spacing-md;
  margin-bottom: $spacing-md;
}

.main-image-list {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.main-image-item {
  flex: 1;
  position: relative;
  border-radius: $radius-md;
  overflow: hidden;
  border: 4rpx solid transparent;

  &.selected {
    border-color: $brand-primary;
  }
}

.main-image {
  width: 100%;
  height: 200rpx;
  display: block;
}

.selected-tag {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba($brand-primary, 0.8);
  padding: 8rpx 0;
  text-align: center;
}

.selected-tag-text {
  font-size: 24rpx;
  color: #fff;
}

.custom-upload {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md;
  background: rgba($brand-primary, 0.05);
  border-radius: $radius-md;
}

.custom-upload-text {
  font-size: 26rpx;
  color: $text-secondary;
}

.btn-secondary {
  padding: 12rpx 24rpx;
  background: #fff;
  border-radius: $radius-md;
  border: 2rpx solid $brand-primary;
}

.btn-secondary.btn-disabled {
  opacity: 0.4;
  border-color: $border-base;
}

.btn-secondary.btn-disabled .btn-secondary-text {
  color: $text-placeholder;
}

.btn-secondary-text {
  font-size: 26rpx;
  color: $brand-primary;
}

.body-image-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-md;
}

.body-image-item {
  width: calc(50% - #{$spacing-sm});
  position: relative;
}

.body-image {
  width: 100%;
  height: 200rpx;
  display: block;
  border-radius: $radius-md;
}

.body-image-position {
  position: absolute;
  bottom: 8rpx;
  left: 8rpx;
  font-size: 22rpx;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  padding: 4rpx 12rpx;
  border-radius: $radius-sm;
}

.skip-hint {
  text-align: center;
  padding: $spacing-md 0;
}

.skip-text {
  font-size: 26rpx;
  color: $text-secondary;
}

.skip-link {
  font-size: 26rpx;
  color: $brand-primary;
  margin-left: $spacing-sm;
}

.action-bar {
  margin-top: $spacing-lg;
}

.btn-primary {
  height: $control-height;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $gradient-blue;
  border-radius: $radius-md;
}

.btn-primary-text {
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
}
</style>
