<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">AI 创作</text>
      <text class="hero-sub">六阶段流水线：策略 → 大纲 → 正文 → 审计润色 → 配图 → 成品</text>
      <view class="hero-btn dy-clickable" @click="goNew"><text class="hero-btn-text">＋ 新建创作</text></view>
    </view>

    <view class="dy-section-title" v-if="drafts.length">未完成创作</view>
    <view v-for="d in drafts" :key="d.id" class="draft dy-card dy-clickable" @click="goDraft(d)">
      <view class="draft-top">
        <text class="dy-tag dy-tag-blue">{{ purposeLabel(d.purpose) }}</text>
        <text class="draft-phase">{{ AI_PHASE_LABELS[d.status] ?? d.status }}</text>
      </view>
      <text class="draft-title">{{ d.selectedTitle || d.topic || '（未定主题）' }}</text>
      <view class="draft-bottom">
        <text class="draft-time">{{ formatTime(d.updatedAt) }}</text>
        <text class="draft-del dy-clickable" @click.stop="onDelete(d)">删除</text>
      </view>
    </view>

    <DyEmpty v-if="!loading && !drafts.length" text="还没有进行中的创作" icon="AI" color="blue" action-text="新建创作" @action="goNew" />
    <view v-if="loading" class="loading"><text class="loading-text">加载中…</text></view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getAiProjects, deleteAiProject } from '@/api/aiCreation'
import type { AiProjectListItem } from '@/types/aiCreation'
import { AI_PHASE_LABELS, phaseStep, purposeLabel } from '@/types/aiCreation'
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue'

/**
 * AI 创作入口页：六阶段流水线草稿列表 + 新建入口。
 */
const drafts = ref<AiProjectListItem[]>([])
const loading = ref(false)

onShow(async () => {
  loading.value = true
  try {
    const res = await getAiProjects({ current: 1, size: 50 })
    drafts.value = res.records.filter((r) => r.status !== 'SAVED')
  } finally {
    loading.value = false
  }
})

function goNew() {
  uni.navigateTo({ url: '/pages/acquisition/ai-create/step-material' })
}

function goDraft(d: AiProjectListItem) {
  uni.navigateTo({ url: `${phaseStep(d.status, d.contentType)}?id=${d.id}` })
}

function onDelete(d: AiProjectListItem) {
  uni.showModal({
    title: '删除草稿',
    content: '删除后不可恢复，确定删除？',
    success: async (res) => {
      if (!res.confirm) return
      await deleteAiProject(d.id)
      drafts.value = drafts.value.filter((x) => x.id !== d.id)
      uni.showToast({ title: '已删除', icon: 'none' })
    }
  })
}

function formatTime(dt?: string) {
  if (!dt) return ''
  return dt.length >= 16 ? dt.substring(0, 16).replace('T', ' ') : dt
}
</script>

<style scoped lang="scss">
.page { padding: $spacing-md $spacing-md 60rpx; background: $bg-page; min-height: 100vh; }
.hero { background: $gradient-blue; border-radius: $radius-lg; padding: $spacing-xl $spacing-lg; margin-bottom: $spacing-md; }
.hero-title { display: block; font-size: 38rpx; font-weight: 700; color: #fff; }
.hero-sub { display: block; font-size: 24rpx; color: rgba(255, 255, 255, 0.85); margin: 12rpx 0 24rpx; }
.hero-btn { align-self: flex-start; background: rgba(255, 255, 255, 0.2); border: 1rpx solid rgba(255, 255, 255, 0.6); border-radius: $radius-md; padding: 14rpx 32rpx; }
.hero-btn-text { color: #fff; font-size: 28rpx; font-weight: 600; }
.draft { margin-bottom: $spacing-sm; padding: $spacing-md; }
.draft-top { display: flex; justify-content: space-between; align-items: center; }
.draft-phase { font-size: 24rpx; color: $text-secondary; }
.draft-title { display: block; font-size: 30rpx; font-weight: 600; color: $text-primary; margin: 12rpx 0; }
.draft-bottom { display: flex; justify-content: space-between; }
.draft-time { font-size: 22rpx; color: $text-placeholder; }
.draft-del { font-size: 24rpx; color: $brand-error; }
.loading { padding: 40rpx; text-align: center; }
.loading-text { font-size: 24rpx; color: $text-secondary; }
</style>
