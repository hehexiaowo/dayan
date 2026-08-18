<template>
  <view class="page">
    <template v-if="project">
      <view v-if="project.warnings?.length" class="warn-box">
        <text v-for="(w, i) in project.warnings" :key="i" class="warn-text">⚠ {{ w }}</text>
      </view>

      <!-- 配图区（正文含占位符或视频封面） -->
      <view v-if="hasImages" class="dy-section-title">AI 配图</view>
      <view v-if="hasImages && !images.length" class="start-box dy-card">
        <text class="start-text">按大纲配图位规划生成插图（封面 + 正文图），失败自动降级为出图描述。</text>
        <view class="start-btn dy-clickable" :class="{ disabled: generating }" @click="start">
          <text class="start-btn-text">{{ generating ? stageText : '开始生成配图' }}</text>
        </view>
      </view>
      <view v-for="img in images" :key="img.placeholder" class="img-card dy-card">
        <view class="img-head">
          <text class="img-ph">{{ img.placeholder === '[AI_IMAGE_COVER]' ? '封面图' : '正文插图' }}</text>
          <text class="img-state" :class="img.status">{{ stateLabel(img.status) }}</text>
        </view>
        <image v-if="img.status === 'done' && img.url" class="img-preview" :src="img.url" mode="widthFix" />
        <view v-if="img.status !== 'done'" class="img-prompt">
          <text class="img-prompt-text">{{ img.promptZh || img.prompt || '（无描述）' }}</text>
          <text v-if="img.error" class="img-error">{{ img.error }}</text>
          <text v-if="img.prompt" class="img-copy dy-clickable" @click="copyPrompt(img)">复制英文出图 prompt</text>
        </view>
      </view>

      <!-- 图文成品预览 -->
      <view class="dy-section-title">图文成品预览</view>
      <view class="preview-box dy-card">
        <view v-if="!previewHtml" class="preview-empty dy-clickable" @click="loadPreview">
          <text class="preview-empty-text">{{ loadingPreview ? '组装中…' : '点击加载成品预览' }}</text>
        </view>
        <!-- #ifdef H5 -->
        <view v-else class="preview-html" v-html="previewHtml" />
        <!-- #endif -->
      </view>

      <view class="footer-bar">
        <view class="btn-plain dy-clickable" @click="goBack"><text class="btn-plain-text">返回改正文</text></view>
        <view class="btn-primary dy-clickable" @click="save">
          <text class="btn-primary-text">{{ saving ? '保存中…' : '保存到内容中心' }}</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getAiProject, getAiPreview, saveAiProject } from '@/api/toolAiCreator'
import { postSseStream } from '@/utils/sse'
import { copyText } from '@/utils/clipboard'
import type { AiProject, AiProjectImage } from '@/types/toolAiCreator'

/**
 * 第 5-6 步：配图生成（SSE 逐张进度）+ 图文成品预览 + 保存内容中心。
 */
const projectId = ref(0)
const project = ref<AiProject | null>(null)
const images = ref<AiProjectImage[]>([])
const generating = ref(false)
const stageText = ref('')
const previewHtml = ref('')
const loadingPreview = ref(false)
const saving = ref(false)

const hasImages = computed(() => {
  if (!project.value?.body) return false
  return /\[AI_IMAGE_/.test(project.value.body) || project.value.contentType === 3
})

onLoad(async (options: any) => {
  projectId.value = Number(options?.id ?? 0)
  if (!projectId.value) { uni.showToast({ title: '参数错误', icon: 'none' }); return }
  project.value = await getAiProject(projectId.value)
  images.value = project.value.images ?? []
})

async function start() {
  if (generating.value) return
  generating.value = true
  stageText.value = '提交配图任务…'
  try {
    await postSseStream(`/agent-api/tools/ai-creator/${projectId.value}/images/stream`, {}, {
      onEvent: (name, data) => {
        let parsed: any
        try { parsed = JSON.parse(data) } catch { return }
        if (name === 'stage') {
          stageText.value = parsed.message
        } else if (name === 'image') {
          const idx = images.value.findIndex((i) => i.placeholder === parsed.placeholder)
          if (idx >= 0) {
            images.value[idx] = { ...images.value[idx], status: parsed.state, url: parsed.url, error: parsed.error }
          } else {
            images.value.push({ placeholder: parsed.placeholder, status: parsed.state, url: parsed.url, error: parsed.error })
          }
        } else if (name === 'done') {
          project.value = parsed
          images.value = parsed.images ?? images.value
        } else if (name === 'error') {
          uni.showToast({ title: parsed.message || '配图失败', icon: 'none' })
        }
      }
    })
  } catch {
    uni.showToast({ title: '配图失败，请稍后重试', icon: 'none' })
  } finally { generating.value = false }
}

async function loadPreview() {
  if (loadingPreview.value) return
  loadingPreview.value = true
  try {
    previewHtml.value = await getAiPreview(projectId.value)
  } catch { /* 已提示 */ } finally { loadingPreview.value = false }
}

async function copyPrompt(img: AiProjectImage) {
  if (!img.prompt) return
  await copyText(img.prompt)
  uni.showToast({ title: '已复制出图描述', icon: 'none' })
}

async function save() {
  if (saving.value) return
  saving.value = true
  try {
    await saveAiProject(projectId.value)
    uni.showToast({ title: '已保存到我的内容', icon: 'success' })
    setTimeout(() => uni.redirectTo({ url: '/pages/acquisition/content/mine' }), 800)
  } catch { /* 已提示 */ } finally { saving.value = false }
}

function goBack() { uni.navigateBack() }

function stateLabel(s: string) {
  return { pending: '等待中', generating: '生成中…', done: '已完成', failed: '失败', skipped: '已跳过' }[s] ?? s
}
</script>

<style scoped lang="scss">
.page { padding: $spacing-md $spacing-md 180rpx; background: $bg-page; min-height: 100vh; }
.warn-box { background: rgba(230, 162, 60, .08); border-radius: $radius-md; padding: 16rpx 24rpx; margin-bottom: 24rpx; }
.warn-text { display: block; font-size: 22rpx; color: $brand-warning; }
.start-box { padding: $spacing-md; display: flex; flex-direction: column; gap: 20rpx; align-items: flex-start; }
.start-text { font-size: 24rpx; color: $text-secondary; line-height: 1.7; }
.start-btn { height: $control-height-sm; padding: 0 40rpx; background: $gradient-blue; border-radius: $radius-md; display: flex; align-items: center; }
.start-btn.disabled { opacity: .6; }
.start-btn-text { color: #fff; font-size: 26rpx; }
.img-card { padding: $spacing-md; margin-bottom: 16rpx; }
.img-head { display: flex; justify-content: space-between; margin-bottom: 12rpx; }
.img-ph { font-size: 26rpx; font-weight: 600; color: $text-primary; }
.img-state { font-size: 22rpx; color: $text-secondary; }
.img-state.done { color: $brand-success; }
.img-state.failed { color: $brand-error; }
.img-preview { width: 100%; border-radius: $radius-sm; }
.img-prompt { background: $bg-page; border-radius: $radius-sm; padding: 16rpx; }
.img-prompt-text { display: block; font-size: 22rpx; color: $text-secondary; line-height: 1.7; }
.img-error { display: block; font-size: 20rpx; color: $brand-error; margin-top: 8rpx; }
.img-copy { display: block; font-size: 22rpx; color: $brand-primary; margin-top: 12rpx; }
.preview-box { padding: $spacing-md; min-height: 200rpx; }
.preview-empty { padding: 60rpx 0; display: flex; justify-content: center; }
.preview-empty-text { font-size: 26rpx; color: $brand-primary; }
.preview-html { overflow: hidden; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 50; display: flex; gap: 20rpx; padding: 20rpx $spacing-md calc(20rpx + env(safe-area-inset-bottom)); background: $bg-card; box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, .04); }
.btn-primary { flex: 1.6; height: $control-height; background: $gradient-blue; border-radius: $radius-md; display: flex; align-items: center; justify-content: center; }
.btn-primary-text { color: #fff; font-size: 28rpx; font-weight: 600; }
.btn-plain { flex: 1; height: $control-height; border: 2rpx solid $border-base; border-radius: $radius-md; display: flex; align-items: center; justify-content: center; }
.btn-plain-text { color: $text-regular; font-size: 26rpx; }
</style>
