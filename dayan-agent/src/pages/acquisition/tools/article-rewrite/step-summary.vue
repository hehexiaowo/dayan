<template>
  <view class="page">
    <!-- 步骤进度 -->
    <ArticleRewriteSteps :current="1" />

    <!-- 加载中 -->
    <DyLoadingState v-if="loading" :text="loadingText" hint="AI 正在分析文章，请耐心等待..." />

    <!-- ① 内容简述 + 相关性标签 -->
    <view v-if="summaryData" class="summary-card dy-card">
      <view class="card-head">
        <view class="dy-section-title">内容简述</view>
        <text class="regen-link" @click="onRegenerateSummary">重新生成</text>
      </view>
      <view class="summary-item">
        <text class="summary-label">标题</text>
        <text class="summary-value">{{ contentData?.originalTitle }}</text>
      </view>
      <view class="summary-item">
        <text class="summary-label">来源</text>
        <text class="summary-value">{{ contentData?.originalSource }}</text>
      </view>
      <view class="summary-item">
        <text class="summary-label">核心总结</text>
        <text class="summary-value">{{ summaryData.contentSummary }}</text>
      </view>

      <!-- 相关性标签选择 -->
      <view v-if="candidateTags.length" class="tag-section">
        <view class="tag-title">相关性标签</view>
        <view class="tag-hint">请勾选与文章相关的领域标签，再进行价值判断</view>
        <view class="tag-list">
          <view
            v-for="tag in candidateTags"
            :key="tag"
            class="tag-chip"
            :class="{ on: selectedTags.includes(tag) }"
            @click="toggleTag(tag)"
          >
            <text class="tag-chip-text">{{ tag }}</text>
          </view>
        </view>
      </view>

      <!-- 价值判断 -->
      <view class="btn-action" :class="{ 'dy-btn-disabled': !selectedTags.length || loading }" @click="onJudge">
        <text class="btn-action-text">进行价值判断</text>
      </view>
    </view>

    <!-- ② 价值判断结果 -->
    <view v-if="summaryData?.viralValue" class="value-card dy-card">
      <view class="dy-section-title">价值判断</view>
      <view class="value-item">
        <text class="value-label">爆点价值</text>
        <text class="value-tag" :class="'tag-' + (summaryData.viralValue.level || 'low')">
          {{ valueLabel(summaryData.viralValue.level) }}
        </text>
        <text class="value-reason">{{ summaryData.viralValue.reason || '' }}</text>
      </view>
      <view class="value-item">
        <text class="value-label">相关性</text>
        <text class="value-tag" :class="'tag-' + (summaryData.relevance?.level || 'none')">
          {{ relevanceLabel(summaryData.relevance?.level) }}
        </text>
        <text class="value-reason">{{ summaryData.relevance?.detail || '未判断' }}</text>
      </view>

      <!-- 生成转写建议（跳转策略页确认） -->
      <view class="btn-action" :class="{ 'dy-btn-disabled': loading }" @click="onPlans">
        <text class="btn-action-text">生成转写建议</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getRewriteDetail, generateSummary, judgeValue, generatePlans } from '@/api/toolArticleRewrite'
import type { ArticleRewriteRecord, ContentFetch, SummaryAnalysis } from '@/types/toolArticleRewrite'
import { getErrMsg } from '@/types/toolArticleRewrite'
import ArticleRewriteSteps from '@/components/ArticleRewriteSteps/ArticleRewriteSteps.vue'
import DyLoadingState from '@/components/DyLoadingState/DyLoadingState.vue'

const record = ref<ArticleRewriteRecord | null>(null)
const loading = ref(false)
const loadingText = ref('加载中...')
/** 选定的相关性标签 */
const selectedTags = ref<string[]>([])

// 解析 contentFetch 字符串为对象
const contentData = computed<ContentFetch | null>(() => {
  if (!record.value?.contentFetch) return null
  if (typeof record.value.contentFetch === 'string') {
    try { return JSON.parse(record.value.contentFetch) } catch { return null }
  }
  return record.value.contentFetch as ContentFetch
})

// 解析 summaryAnalysis 字符串为对象
const summaryData = computed<SummaryAnalysis | null>(() => {
  if (!record.value?.summaryAnalysis) return null
  if (typeof record.value.summaryAnalysis === 'string') {
    try { return JSON.parse(record.value.summaryAnalysis) } catch { return null }
  }
  return record.value.summaryAnalysis as SummaryAnalysis
})

const candidateTags = computed<string[]>(() => summaryData.value?.candidateTags || [])

onLoad(async (options) => {
  if (!options?.id) return

  const id = Number(options.id)
  loading.value = true
  loadingText.value = '正在加载...'
  try {
    record.value = await getRewriteDetail(id)

    // 尚未生成内容简述 → 自动生成
    if (!summaryData.value?.contentSummary) {
      loadingText.value = 'AI 正在分析文章内容...'
      record.value = await generateSummary(id)
    } else if (record.value.status !== 'CONTENT_FETCHED') {
      // 已有总结结果并进入后续步骤，提示是否重新生成
      loading.value = false
      const resume = await askRegenerate()
      if (resume) {
        loading.value = true
        loadingText.value = 'AI 正在重新分析文章内容...'
        try {
          record.value = await generateSummary(id)
        } catch (e: any) {
          uni.showToast({ title: getErrMsg(e, '重新生成失败'), icon: 'none' })
        } finally {
          loading.value = false
        }
      }
    }

    initLocalState()
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '加载失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
})

/** 弹窗询问是否重新生成总结 */
function askRegenerate(): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title: '重新生成判断',
      content: '已存在内容判断结果，是否重新生成？',
      confirmText: '重新生成',
      cancelText: '使用现有',
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    })
  })
}

/** 恢复本地选择状态 */
function initLocalState() {
  const s = summaryData.value
  if (!s) return
  const cands = s.candidateTags || []
  selectedTags.value = (s.selectedTags || []).filter((t) => !cands.length || cands.includes(t))
}

function valueLabel(level?: string) {
  const map: Record<string, string> = { high: '高', medium: '中', low: '低' }
  return map[level || ''] || level
}

function relevanceLabel(level?: string) {
  const map: Record<string, string> = { strong: '强相关', weak: '弱相关', none: '无关联' }
  return map[level || ''] || level || '未判断'
}

function toggleTag(tag: string) {
  const index = selectedTags.value.indexOf(tag)
  if (index >= 0) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(tag)
  }
}

/** 重新生成内容简述（清空标签选择、价值判断和转写方案） */
async function onRegenerateSummary() {
  if (!record.value || loading.value) return
  uni.showModal({
    title: '重新生成内容简述',
    content: '将清空相关性标签、价值判断和转写方案，确认重新生成？',
    confirmText: '重新生成',
    cancelText: '取消',
    success: async (res) => {
      if (!res.confirm) return
      loading.value = true
      loadingText.value = 'AI 正在重新分析文章内容...'
      try {
        record.value = await generateSummary(record.value!.id)
        initLocalState()
      } catch (e: any) {
        uni.showToast({ title: getErrMsg(e, '重新生成失败'), icon: 'none' })
      } finally {
        loading.value = false
      }
    },
  })
}

/** 进行价值判断 */
async function onJudge() {
  if (!record.value || !selectedTags.value.length || loading.value) return
  loading.value = true
  loadingText.value = 'AI 正在判断内容价值...'
  try {
    record.value = await judgeValue(record.value.id, selectedTags.value)
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '价值判断失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

/** 生成转写建议，跳转策略页确认方案 */
async function onPlans() {
  if (!record.value || loading.value) return
  loading.value = true
  loadingText.value = 'AI 正在策划转写方案...'
  try {
    await generatePlans(record.value.id)
    uni.navigateTo({
      url: `/pages/acquisition/tools/article-rewrite/step-plan?id=${record.value.id}`,
    })
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '生成转写建议失败'), icon: 'none' })
  } finally {
    loading.value = false
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

.summary-card,
.value-card {
  padding: $spacing-md;
  margin-bottom: $spacing-md;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-sm;
}

.regen-link {
  font-size: 26rpx;
  color: $brand-primary;
}

.summary-item {
  margin-bottom: $spacing-md;
}

.summary-label {
  font-size: 24rpx;
  color: $text-secondary;
  margin-bottom: 8rpx;
  display: block;
}

.summary-value {
  font-size: 28rpx;
  color: $text-primary;
  display: block;
  line-height: 1.6;
}

/* 相关性标签 */
.tag-section {
  margin-bottom: $spacing-md;
}

.tag-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 8rpx;
}

.tag-hint {
  font-size: 24rpx;
  color: $text-secondary;
  margin-bottom: $spacing-sm;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.tag-chip {
  padding: 12rpx 28rpx;
  border-radius: $radius-lg;
  background: #fff;
  border: 2rpx solid $border-base;
  transition: all $transition-fast;

  &.on {
    background: rgba($brand-primary, 0.08);
    border-color: $brand-primary;

    .tag-chip-text {
      color: $brand-primary;
    }
  }
}

.tag-chip-text {
  font-size: 26rpx;
  color: $text-primary;
}

/* 次级操作按钮 */
.btn-action {
  height: $control-height;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 2rpx solid $brand-primary;
  border-radius: $radius-md;
  margin-top: $spacing-sm;
}

.btn-action-text {
  color: $brand-primary;
  font-size: 28rpx;
  font-weight: 600;
}

/* 价值判断 */
.value-item {
  margin-bottom: $spacing-md;

  &:last-of-type {
    margin-bottom: 0;
  }
}

.value-label {
  font-size: 24rpx;
  color: $text-secondary;
  margin-bottom: 8rpx;
  display: block;
}

.value-tag {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: $radius-sm;
  font-size: 24rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.tag-high,
.tag-strong {
  background: rgba($brand-success, 0.1);
  color: $brand-success;
}

.tag-medium,
.tag-weak {
  background: rgba($brand-warning, 0.1);
  color: $brand-warning;
}

.tag-low,
.tag-none {
  background: rgba($text-placeholder, 0.1);
  color: $text-placeholder;
}

.value-reason {
  font-size: 24rpx;
  color: $text-secondary;
  line-height: 1.5;
  display: block;
}

.dy-btn-disabled {
  opacity: 0.5;
  pointer-events: none;
}
</style>
