<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">AI 创作</text>
      <text class="hero-sub">选择创作分类，六阶段流水线：策略 → 大纲 → 正文 → 审计润色 → 配图 → 成品</text>
    </view>

    <!-- 创作分类选择 -->
    <view class="dy-section-title">选择创作分类</view>
    <DySkeletonList v-if="configLoading" :rows="3" show-avatar />
    <view v-else-if="loadError" class="err-box">
      <text class="err-text">分类加载失败，请检查网络</text>
      <view class="err-btn dy-clickable" @click="loadConfigs"><text class="err-btn-text">重新加载</text></view>
    </view>
    <template v-else>
      <view
        v-for="c in configs"
        :key="c.toolCode"
        class="cat-card dy-card dy-clickable"
        @click="goCategory(c)"
      >
        <DyIconBlock :text="c.icon || '创'" :color="catColor(c.iconColor)" size="lg" shape="circle" />
        <view class="cat-info">
          <text class="cat-name">{{ c.toolName }}</text>
          <text class="cat-desc">{{ c.toolDesc || '六阶段 AI 图文创作' }}</text>
        </view>
        <text class="cat-arrow">›</text>
      </view>
      <DyEmpty v-if="!configs.length" text="暂无可用的创作分类" icon="AI" color="blue" />
    </template>

    <!-- 未完成创作草稿 -->
    <view class="dy-section-title" v-if="drafts.length || loading">未完成创作</view>
    <DySkeletonList v-if="loading" :rows="2" />
    <template v-else>
      <view v-for="d in drafts" :key="d.id" class="draft dy-card dy-clickable" @click="goDraft(d)">
        <view class="draft-top">
          <text class="dy-tag dy-tag-blue">{{ categoryName(d.toolCode) }}</text>
          <text class="draft-phase">{{ AI_PHASE_LABELS[d.status] ?? d.status }}</text>
        </view>
        <text class="draft-title">{{ d.selectedTitle || d.topic || '（未定主题）' }}</text>
        <view class="draft-bottom">
          <text class="draft-time">{{ formatTime(d.updatedAt) }}</text>
          <text class="draft-del dy-clickable" @click.stop="onDelete(d)">删除</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getAiProjects, deleteAiProject, getAiartistConfigs } from '@/api/toolAiartist'
import type { AiProjectListItem, AiartistConfig } from '@/types/toolAiartist'
import { AI_PHASE_LABELS, phaseStep } from '@/types/toolAiartist'
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue'
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue'
import DySkeletonList from '@/components/DySkeletonList/DySkeletonList.vue'

/**
 * AI 创作入口页：创作分类选择（tool_info 的 aiartist 实例）+ 未完成创作草稿续写。
 */
type CatColor = 'blue' | 'green' | 'orange' | 'red' | 'gray'

const COLOR_SET: readonly CatColor[] = ['blue', 'green', 'orange', 'red', 'gray']

function catColor(color?: string): CatColor {
  const hit = COLOR_SET.find((x) => x === color)
  return hit || 'blue'
}

/** 兜底分类列表 — 接口不可用/未配置时保证页面不空白（与 tool_info 预置种子一致） */
const FALLBACK_CONFIGS: AiartistConfig[] = [
  {
    toolCode: 'TL00003',
    toolName: 'AI创作（主题创作）',
    toolDesc: '选择、上传或粘贴文章，进行内容转写与再创作',
    purpose: 'science',
    icon: '主',
    iconColor: 'blue',
  },
  {
    toolCode: 'TL90006',
    toolName: 'AI创作（机构介绍）',
    toolDesc: '选择某个养老机构，进行机构的介绍与亮点总结',
    purpose: 'park',
    icon: '机',
    iconColor: 'green',
  },
  {
    toolCode: 'TL90007',
    toolName: 'AI创作（保险计划）',
    toolDesc: '上传已有的保险计划书，进行计划书的重新组织与表达丰富',
    purpose: 'product',
    icon: '保',
    iconColor: 'orange',
  },
]

const configs = ref<AiartistConfig[]>(FALLBACK_CONFIGS)
const configLoading = ref(false)
const loadError = ref(false)
const drafts = ref<AiProjectListItem[]>([])
const loading = ref(false)

onShow(async () => {
  loadConfigs()
  loading.value = true
  try {
    const res = await getAiProjects({ current: 1, size: 50 })
    drafts.value = res.records.filter((r) => r.status !== 'SAVED')
  } catch {
    // request 层已提示，草稿区静默降级为空
  } finally {
    loading.value = false
  }
})

async function loadConfigs() {
  configLoading.value = true
  loadError.value = false
  try {
    const list = await getAiartistConfigs()
    // 后台已配置则以后台为准；空结果保留兜底
    if (list && list.length > 0) {
      configs.value = list
    }
  } catch {
    // 接口异常：展示错误态，供用户重试
    loadError.value = true
  } finally {
    configLoading.value = false
  }
}

function goCategory(c: AiartistConfig) {
  uni.navigateTo({ url: `/pages/acquisition/tools/aiartist/step-material?toolCode=${c.toolCode}` })
}

function categoryName(toolCode?: string): string {
  if (!toolCode) return 'AI 创作'
  return configs.value.find((c) => c.toolCode === toolCode)?.toolName || toolCode
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
.hero-sub { display: block; font-size: 24rpx; color: rgba(255, 255, 255, 0.85); margin-top: 12rpx; }
.cat-card { display: flex; align-items: center; padding: $spacing-md; margin-bottom: $spacing-sm; }
.cat-info { flex: 1; margin-left: $spacing-md; display: flex; flex-direction: column; min-width: 0; }
.cat-name { font-size: 30rpx; font-weight: 600; color: $text-primary; }
.cat-desc { margin-top: 6rpx; font-size: 24rpx; color: $text-secondary; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cat-arrow { font-size: 40rpx; color: $text-placeholder; margin-left: $spacing-sm; }
.draft { margin-bottom: $spacing-sm; padding: $spacing-md; }
.draft-top { display: flex; justify-content: space-between; align-items: center; }
.draft-phase { font-size: 24rpx; color: $text-secondary; }
.draft-title { display: block; font-size: 30rpx; font-weight: 600; color: $text-primary; margin: 12rpx 0; }
.draft-bottom { display: flex; justify-content: space-between; }
.draft-time { font-size: 22rpx; color: $text-placeholder; }
.draft-del { font-size: 24rpx; color: $brand-error; }
.err-box { display: flex; flex-direction: column; align-items: center; padding: 80rpx 0; gap: 24rpx; }
.err-text { font-size: 26rpx; color: $text-secondary; }
.err-btn { height: $control-height-sm; padding: 0 48rpx; background: $gradient-blue; border-radius: $radius-md; display: flex; align-items: center; justify-content: center; }
.err-btn-text { color: #fff; font-size: 26rpx; font-weight: 600; }
</style>
