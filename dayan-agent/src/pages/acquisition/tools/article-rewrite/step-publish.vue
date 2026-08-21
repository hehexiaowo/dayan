<template>
  <view class="page">
    <!-- 步骤进度 -->
    <ArticleRewriteSteps :current="6" />

    <!-- 加载中 -->
    <DyLoadingState v-if="loading" :text="loadingText" />

    <!-- 内容预览和编辑 -->
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

      <!-- 标题编辑 -->
      <view class="edit-card dy-card">
        <view class="dy-section-title">文章标题</view>
        <input class="dy-input" v-model="editTitle" placeholder="请输入标题" />
      </view>

      <!-- 摘要 -->
      <view class="edit-card dy-card">
        <view class="dy-section-title">摘要</view>
        <textarea class="dy-textarea" v-model="editSummary" placeholder="文章摘要" maxlength="200" />
      </view>

      <!-- 正文编辑 -->
      <view class="edit-card dy-card">
        <view class="dy-section-title">
          正文内容
          <text class="edit-hint">（可直接编辑修改）</text>
        </view>
        <textarea
          class="dy-textarea body-textarea"
          v-model="editBody"
          placeholder="文章正文"
          :maxlength="10000"
        />
        <view class="word-count">
          <text class="word-count-text">{{ editBody.length }} 字</text>
        </view>
      </view>

      <!-- 关键词 -->
      <view class="edit-card dy-card" v-if="currentResult?.keywords?.length">
        <view class="dy-section-title">关键词</view>
        <view class="keywords-list">
          <text v-for="(kw, i) in currentResult.keywords" :key="i" class="keyword-tag">{{ kw }}</text>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="action-bar">
        <view class="btn-secondary dy-clickable" @click="onSaveDraft">
          <text class="btn-secondary-text">保存草稿</text>
        </view>
        <view class="btn-primary dy-clickable" :class="{ 'dy-btn-disabled': saving }" @click="onSave">
          <text class="btn-primary-text">{{ saving ? '保存中...' : '保存到内容库' }}</text>
        </view>
      </view>
    </template>

    <!-- 保存成功 -->
    <view v-if="saved" class="saved-card dy-card">
      <view class="saved-icon">
        <text class="saved-icon-text">✓</text>
      </view>
      <text class="saved-title">保存成功</text>
      <text class="saved-desc">文章已保存到内容库</text>
      <view class="btn-primary dy-clickable" @click="onBackToList">
        <text class="btn-primary-text">返回列表</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getRewriteDetail, saveDraft } from '@/api/toolArticleRewrite'
import request from '@/utils/request'
import type { ArticleRewriteRecord, RewriteResult, RewriteResultItem, SummaryAnalysis } from '@/types/toolArticleRewrite'
import { getCurrentPlanResult, getErrMsg } from '@/types/toolArticleRewrite'
import ArticleRewriteSteps from '@/components/ArticleRewriteSteps/ArticleRewriteSteps.vue'
import DyLoadingState from '@/components/DyLoadingState/DyLoadingState.vue'

const record = ref<ArticleRewriteRecord | null>(null)
const currentPlanId = ref('')
const loading = ref(false)
const saving = ref(false)
const saved = ref(false)

// 编辑字段
const editTitle = ref('')
const editSummary = ref('')
const editBody = ref('')

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

// 监听 currentResult 变化，更新编辑字段
watch(currentResult, (newVal) => {
  if (newVal) {
    editTitle.value = newVal.title || ''
    editSummary.value = newVal.summary || ''
    editBody.value = newVal.body || ''
  }
}, { immediate: true })

onLoad(async (options) => {
  if (!options?.id) return

  const id = Number(options.id)
  loading.value = true
  try {
    record.value = await getRewriteDetail(id)
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

async function onSaveDraft() {
  if (!record.value || saving.value) return

  saving.value = true
  try {
    // 保存编辑内容
    await saveDraft(record.value.id, {
      title: editTitle.value,
      summary: editSummary.value,
      body: editBody.value,
    })
    uni.showToast({ title: '草稿已保存', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '保存失败'), icon: 'none' })
  } finally {
    saving.value = false
  }
}

async function onSave() {
  if (!record.value || saving.value) return

  saving.value = true
  try {
    // 调用保存到内容库的接口
    await request({
      url: `/tools/article-rewrite/${record.value.id}/save-to-content`,
      method: 'POST',
      data: {
        title: editTitle.value,
        summary: editSummary.value,
        body: editBody.value,
      },
    })
    saved.value = true
    uni.showToast({ title: '保存成功', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '保存失败'), icon: 'none' })
  } finally {
    saving.value = false
  }
}

function onBackToList() {
  // 返回到 AI 创作入口页（重新加载）
  uni.reLaunch({
    url: '/pages/acquisition/tools/aiartist/index',
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

.edit-card {
  padding: $spacing-md;
  margin-bottom: $spacing-md;
}

.dy-input {
  background: #fff;
  border-radius: $radius-md;
  padding: 0 $spacing-md;
  height: $control-height;
  font-size: 28rpx;
  margin-top: $spacing-sm;
}

.dy-textarea {
  background: #fff;
  border-radius: $radius-md;
  padding: $spacing-md;
  font-size: 28rpx;
  width: 100%;
  min-height: 200rpx;
  margin-top: $spacing-sm;
}

.body-textarea {
  min-height: 500rpx;
}

.edit-hint {
  font-size: 24rpx;
  color: $text-secondary;
  font-weight: normal;
  margin-left: $spacing-sm;
}

.word-count {
  text-align: right;
  margin-top: $spacing-sm;
}

.word-count-text {
  font-size: 24rpx;
  color: $text-placeholder;
}

.keywords-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
}

.keyword-tag {
  padding: 8rpx 20rpx;
  background: rgba($brand-primary, 0.1);
  color: $brand-primary;
  border-radius: $radius-sm;
  font-size: 26rpx;
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

.dy-btn-disabled {
  opacity: 0.5;
  pointer-events: none;
}

.saved-card {
  padding: $spacing-xl;
  text-align: center;
  margin-top: $spacing-lg;
}

.saved-icon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba($brand-success, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto $spacing-lg;
}

.saved-icon-text {
  font-size: 60rpx;
  color: $brand-success;
}

.saved-title {
  font-size: 36rpx;
  font-weight: 700;
  color: $text-primary;
  display: block;
  margin-bottom: $spacing-sm;
}

.saved-desc {
  font-size: 28rpx;
  color: $text-secondary;
  display: block;
  margin-bottom: $spacing-lg;
}
</style>
