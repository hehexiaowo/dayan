<template>
  <view class="page">
    <!-- 步骤进度 -->
    <ArticleRewriteSteps :current="3" />

    <!-- 加载中 -->
    <DyLoadingState v-if="loading" :text="loadingText" hint="AI 正在生成内容，请耐心等待..." />

    <!-- 转写结果 -->
    <template v-if="rewriteData">
      <!-- 方案切换标签 -->
      <view v-if="rewriteData.results && rewriteData.results.length > 1" class="plan-tabs">
        <text
          v-for="item in rewriteData.results"
          :key="item.planId"
          class="plan-tab dy-clickable"
          :class="{ on: currentPlanId === item.planId }"
          @click="currentPlanId = item.planId"
        >{{ getPlanName(item.planId) }}</text>
      </view>

      <!-- 转写内容预览 -->
      <view class="rewrite-card dy-card">
        <view class="rewrite-title">{{ currentResult?.title }}</view>
        <view class="rewrite-meta">
          <text class="meta-item">字数：{{ currentResult?.wordCount }}</text>
          <text class="meta-item">摘要：{{ currentResult?.summary }}</text>
        </view>
        <view class="rewrite-body">
          <text class="body-text">{{ currentResult?.body }}</text>
        </view>
        <view class="rewrite-keywords" v-if="currentResult?.keywords?.length">
          <text class="keywords-label">关键词：</text>
          <text v-for="(kw, i) in currentResult.keywords" :key="i" class="keyword-tag">{{ kw }}</text>
        </view>
        <view class="rewrite-adaptation" v-if="currentResult?.channelAdaptation">
          <text class="adaptation-label">渠道适配：</text>
          <text class="adaptation-value">{{ currentResult.channelAdaptation }}</text>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="action-bar">
        <view class="btn-secondary dy-clickable" @click="onRegenerate">
          <text class="btn-secondary-text">重新生成</text>
        </view>
        <view class="btn-primary dy-clickable" @click="onNext">
          <text class="btn-primary-text">下一步：审查</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getRewriteDetail, regenerateRewrite, rewrite } from '@/api/toolArticleRewrite'
import type { ArticleRewriteRecord, RewriteResult, RewriteResultItem, SummaryAnalysis } from '@/types/toolArticleRewrite'
import { getCurrentPlanResult, getErrMsg } from '@/types/toolArticleRewrite'
import ArticleRewriteSteps from '@/components/ArticleRewriteSteps/ArticleRewriteSteps.vue'
import DyLoadingState from '@/components/DyLoadingState/DyLoadingState.vue'

const record = ref<ArticleRewriteRecord | null>(null)
const currentPlanId = ref('')
const loading = ref(false)
const loadingText = ref('加载中...')

// 解析 rewriteResult 字符串为对象
const rewriteData = computed<RewriteResult | null>(() => {
  if (!record.value?.rewriteResult) return null
  if (typeof record.value.rewriteResult === 'string') {
    try { return JSON.parse(record.value.rewriteResult) } catch { return null }
  }
  return record.value.rewriteResult as RewriteResult
})

// 解析 summaryAnalysis 字符串为对象
const summaryData = computed<SummaryAnalysis | null>(() => {
  if (!record.value?.summaryAnalysis) return null
  if (typeof record.value.summaryAnalysis === 'string') {
    try { return JSON.parse(record.value.summaryAnalysis) } catch { return null }
  }
  return record.value.summaryAnalysis as SummaryAnalysis
})

const currentResult = computed<RewriteResultItem | undefined>(() => {
  return getCurrentPlanResult(rewriteData.value?.results, currentPlanId.value)
})

onLoad(async (options) => {
  if (!options?.id) return

  const id = Number(options.id)
  loading.value = true
  loadingText.value = '正在加载...'
  try {
    record.value = await getRewriteDetail(id)

    // 如果方案已选但转写未执行，自动转写
    if (record.value.status === 'PLANNED') {
      loadingText.value = 'AI 正在生成转写内容...'
      record.value = await rewrite(id)
    }

    currentPlanId.value = rewriteData.value?.currentPlanId || ''
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

async function onRegenerate() {
  if (!record.value) return

  uni.showModal({
    title: '重新生成',
    content: '确定要重新生成转写内容吗？',
    success: async (res) => {
      if (!res.confirm) return
      loading.value = true
      loadingText.value = 'AI 正在重新生成内容...'
      try {
        record.value = await regenerateRewrite(record.value!.id)
        // 解析 rewriteResult 获取 currentPlanId
        let parsed: any = null
        if (record.value.rewriteResult) {
          if (typeof record.value.rewriteResult === 'string') {
            try { parsed = JSON.parse(record.value.rewriteResult) } catch { parsed = null }
          } else {
            parsed = record.value.rewriteResult
          }
        }
        currentPlanId.value = parsed?.currentPlanId || ''
        uni.showToast({ title: '重新生成成功', icon: 'success' })
      } catch (e: any) {
        uni.showToast({ title: getErrMsg(e, '重新生成失败'), icon: 'none' })
      } finally {
        loading.value = false
      }
    },
  })
}

function onNext() {
  if (!record.value) return
  uni.navigateTo({
    url: `/pages/acquisition/tools/article-rewrite/step-audit?id=${record.value.id}`,
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

.rewrite-card {
  padding: $spacing-md;
}

.rewrite-title {
  font-size: 34rpx;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: $spacing-md;
  line-height: 1.4;
}

.rewrite-meta {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 24rpx;
  color: $text-secondary;
}

.rewrite-body {
  margin-bottom: $spacing-md;
}

.body-text {
  font-size: 28rpx;
  color: $text-primary;
  line-height: 1.8;
  white-space: pre-wrap;
}

.rewrite-keywords {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.keywords-label {
  font-size: 24rpx;
  color: $text-secondary;
}

.keyword-tag {
  padding: 4rpx 16rpx;
  background: rgba($brand-primary, 0.1);
  color: $brand-primary;
  border-radius: $radius-sm;
  font-size: 24rpx;
}

.rewrite-adaptation {
  background: rgba($brand-warning, 0.05);
  padding: $spacing-md;
  border-radius: $radius-md;
}

.adaptation-label {
  font-size: 24rpx;
  color: $brand-warning;
  font-weight: 600;
}

.adaptation-value {
  font-size: 26rpx;
  color: $text-primary;
  line-height: 1.6;
}

.action-bar {
  display: flex;
  gap: $spacing-md;
  margin-top: $spacing-lg;
}

.btn-primary,
.btn-secondary {
  flex: 1;
  height: $control-height;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-md;
}

.btn-primary {
  background: $gradient-blue;
}

.btn-primary-text {
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
}

.btn-secondary {
  background: #fff;
  border: 2rpx solid $brand-primary;
}

.btn-secondary-text {
  color: $brand-primary;
  font-size: 30rpx;
}
</style>
