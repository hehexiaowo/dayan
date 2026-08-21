<template>
  <view class="page">
    <!-- 步骤进度 -->
    <ArticleRewriteSteps :current="2" />

    <!-- 加载中 -->
    <DyLoadingState v-if="loading" :text="loadingText" hint="请稍候..." />

    <!-- 转写方案选择（等量可选卡片） -->
    <view v-if="summaryData?.rewritePlans" class="plans-card dy-card">
      <view class="dy-section-title">选择转写方案</view>
      <view class="plan-tip">AI 已按内容生成以下方案，请选择其一用于转写</view>
      <view
        v-for="(plan, index) in summaryData.rewritePlans"
        :key="plan.planId"
        class="plan-item dy-clickable"
        :class="{ selected: selectedPlanId === plan.planId }"
        @click="choosePlan(plan.planId)"
      >
        <view class="plan-head">
          <view class="plan-index" :class="{ on: selectedPlanId === plan.planId }">
            <text class="plan-index-text">{{ index + 1 }}</text>
          </view>
          <text class="plan-name">{{ plan.name }}</text>
          <view v-if="selectedPlanId === plan.planId" class="plan-badge">
            <text class="plan-badge-text">已选</text>
          </view>
        </view>
        <view class="plan-tags">
          <text class="plan-tag">{{ plan.style }}</text>
          <text class="plan-tag">{{ plan.channel }}</text>
          <text class="plan-tag">{{ plan.wordCount }}</text>
        </view>
        <view class="plan-row">
          <text class="plan-label">切入角度</text>
          <text class="plan-value">{{ plan.angle }}</text>
        </view>
        <view class="plan-reason">
          <text class="reason-label">推荐理由</text>
          <text class="reason-value">{{ plan.reason }}</text>
        </view>
      </view>
    </view>

    <!-- 底部按钮 -->
    <view class="footer-bar">
      <view
        class="btn-primary dy-clickable"
        :class="{ 'dy-btn-disabled': !selectedPlanId || submitting }"
        @click="onSubmit"
      >
        <text class="btn-primary-text">{{ submitting ? '提交中...' : '开始转写' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getRewriteDetail, selectPlan } from '@/api/toolArticleRewrite'
import type { ArticleRewriteRecord, SummaryAnalysis } from '@/types/toolArticleRewrite'
import { getErrMsg } from '@/types/toolArticleRewrite'
import ArticleRewriteSteps from '@/components/ArticleRewriteSteps/ArticleRewriteSteps.vue'
import DyLoadingState from '@/components/DyLoadingState/DyLoadingState.vue'

const record = ref<ArticleRewriteRecord | null>(null)
const loading = ref(false)
const loadingText = ref('加载中...')
const submitting = ref(false)
/** 选定的转写方案（单选） */
const selectedPlanId = ref<string>('')

// 解析 summaryAnalysis 字符串为对象
const summaryData = computed<SummaryAnalysis | null>(() => {
  if (!record.value?.summaryAnalysis) return null
  if (typeof record.value.summaryAnalysis === 'string') {
    try { return JSON.parse(record.value.summaryAnalysis) } catch { return null }
  }
  return record.value.summaryAnalysis as SummaryAnalysis
})

onLoad(async (options) => {
  if (!options?.id) return

  const id = Number(options.id)
  loading.value = true
  loadingText.value = '正在加载...'
  try {
    record.value = await getRewriteDetail(id)

    if (!summaryData.value?.rewritePlans?.length) {
      // 尚未生成转写方案，回判断页
      uni.showToast({ title: '请先生成转写建议', icon: 'none' })
      setTimeout(() => {
        uni.reLaunch({ url: `/pages/acquisition/tools/article-rewrite/step-summary?id=${id}` })
      }, 800)
      return
    }

    // 恢复已选方案
    const selected = summaryData.value?.selectedPlanIds
    if (selected?.length) {
      selectedPlanId.value = selected[0]
    }
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '加载失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
})

/** 单选转写方案 */
function choosePlan(planId: string) {
  selectedPlanId.value = planId
}

async function onSubmit() {
  if (!record.value || !selectedPlanId.value || submitting.value) return

  submitting.value = true
  try {
    await selectPlan(record.value.id, selectedPlanId.value)

    // 跳转到转写页面等待转写完成（不在本页等待）
    uni.navigateTo({
      url: `/pages/acquisition/tools/article-rewrite/step-rewrite?id=${record.value.id}`,
    })
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '提交失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.page {
  padding: $spacing-md;
  padding-bottom: 40rpx;
  background: $bg-page;
  min-height: 100vh;
}

.plans-card {
  padding: $spacing-md;
  margin-bottom: $spacing-md;
}

.plan-tip {
  font-size: 24rpx;
  color: $text-secondary;
  margin-bottom: $spacing-md;
}

.plan-item {
  background: #fff;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-md;
  border: 2rpx solid $border-base;
  transition: all $transition-fast;

  &.selected {
    border-color: $brand-primary;
    background: rgba($brand-primary, 0.04);
    box-shadow: $shadow-card;
  }
}

.plan-head {
  display: flex;
  align-items: center;
  margin-bottom: $spacing-md;
}

.plan-index {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: $brand-info-light;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: $spacing-sm;
  flex-shrink: 0;

  &.on {
    background: $brand-primary;

    .plan-index-text {
      color: #fff;
    }
  }
}

.plan-index-text {
  font-size: 22rpx;
  color: $text-secondary;
  font-weight: 600;
}

.plan-name {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
  min-width: 0;
}

.plan-badge {
  flex-shrink: 0;
  padding: 4rpx 16rpx;
  background: $brand-primary;
  border-radius: $radius-sm;
}

.plan-badge-text {
  font-size: 22rpx;
  color: #fff;
}

.plan-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.plan-tag {
  padding: 4rpx 16rpx;
  background: rgba($brand-primary, 0.08);
  color: $brand-primary;
  border-radius: $radius-sm;
  font-size: 22rpx;
}

.plan-row {
  margin-bottom: $spacing-sm;
}

.plan-label {
  font-size: 24rpx;
  color: $text-secondary;
  margin-right: $spacing-sm;
}

.plan-value {
  font-size: 26rpx;
  color: $text-primary;
}

.plan-reason {
  background: rgba($brand-warning, 0.06);
  border-radius: $radius-sm;
  padding: $spacing-sm;
  margin-top: $spacing-sm;
}

.reason-label {
  font-size: 22rpx;
  color: $brand-warning;
  font-weight: 600;
  margin-right: $spacing-sm;
}

.reason-value {
  font-size: 24rpx;
  color: $text-primary;
  line-height: 1.5;
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

.dy-btn-disabled {
  opacity: 0.5;
  pointer-events: none;
}
</style>
