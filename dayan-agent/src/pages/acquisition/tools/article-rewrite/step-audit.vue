<template>
  <view class="page">
    <!-- 步骤进度 -->
    <ArticleRewriteSteps :current="4" />

    <!-- 加载中 -->
    <DyLoadingState v-if="loading" :text="loadingText" hint="AI 正在审核内容，请耐心等待..." />

    <template v-if="auditData">
      <!-- 方案切换标签 -->
      <view v-if="auditData.results && auditData.results.length > 1" class="plan-tabs">
        <text
          v-for="item in auditData.results"
          :key="item.planId"
          class="plan-tab dy-clickable"
          :class="{ on: currentPlanId === item.planId }"
          @click="switchPlan(item.planId)"
        >{{ getPlanName(item.planId) }}</text>
      </view>

      <!-- 审核结果列表 -->
      <view class="audit-card dy-card">
        <view class="dy-section-title">审核结果</view>

        <view v-if="!currentAudit?.items?.length" class="empty-audit">
          <text class="empty-text">未发现需要修改的内容</text>
        </view>

        <view
          v-for="(item, index) in currentAudit?.items"
          :key="index"
          class="audit-item dy-clickable"
          :class="[
            item.severity === 'error' ? 'severity-error' : 'severity-warning',
            { selected: selectedIndexes.includes(index), fixed: item.fixed }
          ]"
          @click="toggleSelect(index)"
        >
          <view class="audit-head">
            <view class="audit-title">
              <text class="audit-dimension">{{ dimensionLabel(item.dimension) }}</text>
              <text class="audit-item-name">{{ item.item }}</text>
            </view>
            <text class="audit-severity" :class="'severity-' + item.severity">
              {{ item.severity === 'error' ? '必须修改' : '建议修改' }}
            </text>
          </view>
          <view class="audit-body">
            <view class="audit-desc">
              <text class="desc-label">问题描述：</text>
              <text class="desc-value">{{ item.description }}</text>
            </view>
            <view class="audit-original" v-if="item.originalText">
              <text class="original-label">原文：</text>
              <text class="original-value">{{ item.originalText }}</text>
            </view>
            <view class="audit-suggestion">
              <text class="suggestion-label">修改建议：</text>
              <text class="suggestion-value">{{ item.suggestion }}</text>
            </view>
            <view class="audit-fixed" v-if="item.fixedText">
              <text class="fixed-label">建议替换为：</text>
              <text class="fixed-value">{{ item.fixedText }}</text>
            </view>
          </view>
          <view v-if="item.fixed" class="audit-fixed-tag">
            <text class="fixed-tag-text">已修复</text>
          </view>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="action-bar">
        <view
          class="btn-secondary dy-clickable"
          :class="{ 'dy-btn-disabled': !selectedIndexes.length }"
          @click="onFix"
        >
          <text class="btn-secondary-text">一键修复（{{ selectedIndexes.length }}项）</text>
        </view>
        <view class="btn-primary dy-clickable" @click="onNext">
          <text class="btn-primary-text">下一步</text>
        </view>
      </view>

      <!-- 重新检查 -->
      <view class="recheck-bar">
        <text class="recheck-link dy-clickable" @click="onReAudit">重新检查</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getRewriteDetail, audit, fixAudit } from '@/api/toolArticleRewrite'
import type { ArticleRewriteRecord, AuditResult, AuditResultItem, SummaryAnalysis } from '@/types/toolArticleRewrite'
import { getCurrentPlanResult, getErrMsg } from '@/types/toolArticleRewrite'
import ArticleRewriteSteps from '@/components/ArticleRewriteSteps/ArticleRewriteSteps.vue'
import DyLoadingState from '@/components/DyLoadingState/DyLoadingState.vue'

const record = ref<ArticleRewriteRecord | null>(null)
const currentPlanId = ref('')
const selectedIndexes = ref<number[]>([])
const loading = ref(false)
const loadingText = ref('加载中...')

// 解析 auditResult 字符串为对象
const auditData = computed<AuditResult | null>(() => {
  if (!record.value?.auditResult) return null
  if (typeof record.value.auditResult === 'string') {
    try { return JSON.parse(record.value.auditResult) } catch { return null }
  }
  return record.value.auditResult as AuditResult
})

// 解析 summaryAnalysis 字符串为对象
const summaryData = computed<SummaryAnalysis | null>(() => {
  if (!record.value?.summaryAnalysis) return null
  if (typeof record.value.summaryAnalysis === 'string') {
    try { return JSON.parse(record.value.summaryAnalysis) } catch { return null }
  }
  return record.value.summaryAnalysis as SummaryAnalysis
})

const currentAudit = computed<AuditResultItem | undefined>(() => {
  return getCurrentPlanResult(auditData.value?.results, currentPlanId.value)
})

onLoad(async (options) => {
  if (!options?.id) return

  const id = Number(options.id)
  loading.value = true
  loadingText.value = '正在加载...'
  try {
    record.value = await getRewriteDetail(id)

    // 转写完成但未审核：执行审核
    if (record.value.status === 'REWRITTEN') {
      loadingText.value = 'AI 正在审核内容...'
      record.value = await audit(id)
    }

    // 手动解析 auditResult
    let parsedAudit: any = null
    if (record.value.auditResult) {
      if (typeof record.value.auditResult === 'string') {
        try { parsedAudit = JSON.parse(record.value.auditResult) } catch { parsedAudit = null }
      } else {
        parsedAudit = record.value.auditResult
      }
    }

    currentPlanId.value = parsedAudit?.currentPlanId || ''

    // 获取当前方案的审核结果
    let currentAuditItems: any[] = []
    if (parsedAudit?.results && currentPlanId.value) {
      const currentAuditResult = parsedAudit.results.find((r: any) => r.planId === currentPlanId.value)
      if (currentAuditResult?.items) {
        currentAuditItems = currentAuditResult.items
      }
    }

    // 自动选中所有必须修改的项
    if (currentAuditItems.length) {
      selectedIndexes.value = currentAuditItems
        .map((item: any, index: number) => (item.severity === 'error' ? index : -1))
        .filter((index: number) => index >= 0)
    }
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

/** 切换方案：重置选中项，自动选中新方案的必须修改项 */
function switchPlan(planId: string) {
  currentPlanId.value = planId
  const items = currentAudit.value?.items || []
  selectedIndexes.value = items
    .map((item, index) => (item.severity === 'error' ? index : -1))
    .filter((index) => index >= 0)
}

function dimensionLabel(dimension: string) {
  const map: Record<string, string> = {
    ai_taste: '降AI味',
    safety: '安全性',
  }
  return map[dimension] || dimension
}

function toggleSelect(index: number) {
  const item = currentAudit.value?.items?.[index]
  if (!item || item.fixed) return
  // 必须修改的项不能取消选择
  if (item.severity === 'error' && selectedIndexes.value.includes(index)) {
    return
  }

  const i = selectedIndexes.value.indexOf(index)
  if (i >= 0) {
    selectedIndexes.value.splice(i, 1)
  } else {
    selectedIndexes.value.push(index)
  }
}

async function onFix() {
  if (!record.value || !selectedIndexes.value.length) return

  loading.value = true
  loadingText.value = 'AI 正在修复内容...'
  try {
    record.value = await fixAudit(record.value.id, selectedIndexes.value)
    selectedIndexes.value = []
    uni.showToast({ title: '修复成功', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '修复失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

/** 重新检查：重新执行审核（覆盖现有审核结果） */
function onReAudit() {
  if (!record.value || loading.value) return

  uni.showModal({
    title: '重新检查',
    content: '将重新对文章进行降AI味检测和安全审查，现有审核结果会被覆盖，确定继续？',
    success: async (res) => {
      if (!res.confirm) return
      loading.value = true
      loadingText.value = 'AI 正在重新审核内容...'
      try {
        record.value = await audit(record.value!.id)
        // 重新解析审核结果
        let parsedAudit: any = null
        if (record.value!.auditResult) {
          if (typeof record.value!.auditResult === 'string') {
            try { parsedAudit = JSON.parse(record.value!.auditResult) } catch { parsedAudit = null }
          } else {
            parsedAudit = record.value!.auditResult
          }
        }
        currentPlanId.value = parsedAudit?.currentPlanId || ''

        // 重新自动选中必须修改的项
        const items = currentAudit.value?.items || []
        selectedIndexes.value = items
          .map((item, index) => (item.severity === 'error' ? index : -1))
          .filter((index) => index >= 0)

        uni.showToast({ title: '检查完成', icon: 'success' })
      } catch (e: any) {
        uni.showToast({ title: getErrMsg(e, '重新检查失败'), icon: 'none' })
      } finally {
        loading.value = false
      }
    },
  })
}

function onNext() {
  if (!record.value) return
  uni.navigateTo({
    url: `/pages/acquisition/tools/article-rewrite/step-image?id=${record.value.id}`,
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

.audit-card {
  padding: $spacing-md;
}

.empty-audit {
  padding: 60rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 28rpx;
  color: $text-secondary;
}

.audit-item {
  background: #fff;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-md;
  border: 2rpx solid $border-base;
  border-left: 6rpx solid transparent;
  transition: all $transition-fast;

  &.severity-error {
    border-left-color: $brand-error;
  }

  &.severity-warning {
    border-left-color: $brand-warning;
  }

  &.selected {
    border-color: $brand-primary;
    background: rgba($brand-primary, 0.04);
    box-shadow: $shadow-card;

    &.severity-error {
      border-left-color: $brand-error;
    }

    &.severity-warning {
      border-left-color: $brand-warning;
    }
  }

  &.fixed {
    background: rgba($brand-success, 0.04);
  }
}

.audit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;
}

.audit-title {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.audit-dimension {
  font-size: 22rpx;
  color: $text-secondary;
  background: $brand-info-light;
  padding: 4rpx 12rpx;
  border-radius: $radius-sm;
  margin-right: $spacing-sm;
  flex-shrink: 0;
}

.audit-item-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
}

.audit-severity {
  flex-shrink: 0;
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: $radius-sm;
  margin-left: $spacing-sm;

  &.severity-error {
    background: rgba($brand-error, 0.1);
    color: $brand-error;
  }

  &.severity-warning {
    background: rgba($brand-warning, 0.1);
    color: $brand-warning;
  }
}

.audit-body {
  padding-left: 0;
}

.audit-desc,
.audit-original,
.audit-suggestion,
.audit-fixed {
  margin-bottom: $spacing-sm;
}

.desc-label,
.original-label,
.suggestion-label,
.fixed-label {
  font-size: 24rpx;
  color: $text-secondary;
}

.desc-value,
.original-value,
.suggestion-value,
.fixed-value {
  font-size: 26rpx;
  color: $text-primary;
  line-height: 1.5;
}

.original-value {
  color: $brand-error;
  text-decoration: line-through;
}

.fixed-value {
  color: $brand-success;
  font-weight: 600;
}

.audit-fixed-tag {
  margin-top: $spacing-sm;
  padding-left: 0;
}

.fixed-tag-text {
  font-size: 22rpx;
  color: $brand-success;
  background: rgba($brand-success, 0.1);
  padding: 4rpx 12rpx;
  border-radius: $radius-sm;
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

.recheck-bar {
  text-align: center;
  padding: $spacing-md 0 $spacing-lg;
}

.recheck-link {
  font-size: 26rpx;
  color: $text-secondary;
  text-decoration: underline;
}
</style>
