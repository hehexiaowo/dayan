<template>
  <view class="page">
    <StepProgress :current="3" />

    <view v-if="loading" class="state-box">
      <view class="dot-loading"><view class="dot" /><view class="dot" /><view class="dot" /></view>
      <text class="state-text">正在搭建大纲骨架…</text>
    </view>

    <template v-else-if="project">
      <view class="head-card dy-card">
        <text class="head-title">{{ project.selectedTitle }}</text>
        <text class="head-sub">{{ purposeLabel(project.purpose) }} · 共 {{ nodes.length }} 个章节节点</text>
      </view>

      <view v-for="(n, i) in nodes" :key="i" class="node-card dy-card">
        <view class="node-head">
          <text class="node-idx">{{ i + 1 }}</text>
          <text class="node-section">{{ n.section }}</text>
          <text class="node-edit dy-clickable" @click="n.editing = !n.editing">{{ n.editing ? '完成' : '编辑' }}</text>
        </view>
        <template v-if="!n.editing">
          <view class="node-block">
            <text class="node-label">核心论点</text>
            <text v-for="(c, j) in splitLines(n.corePointsText)" :key="'c' + j" class="node-line">· {{ c }}</text>
          </view>
          <view class="node-block">
            <text class="node-label">论据（须来自素材）</text>
            <text v-for="(a, j) in splitLines(n.argumentsText)" :key="'a' + j" class="node-line arg">· {{ a }}</text>
          </view>
          <view v-if="n.imagePromptZh" class="node-img">
            <view class="img-flag" />
            <text class="node-img-text">配图：{{ n.imagePromptZh }}（{{ n.imageSize || '1280*720' }}）</text>
          </view>
        </template>
        <template v-else>
          <text class="node-label">章节标题</text>
          <input class="dy-input" v-model="n.section" />
          <text class="node-label">核心论点（每行一条）</text>
          <textarea class="dy-textarea" v-model="n.corePointsText" auto-height maxlength="500" />
          <text class="node-label">论据（每行一条）</text>
          <textarea class="dy-textarea" v-model="n.argumentsText" auto-height maxlength="800" />
        </template>
      </view>

      <view class="regen-box dy-card">
        <textarea class="dy-textarea" v-model="feedback" maxlength="200" placeholder="对结构不满意？写点调整方向（可选），如：开头更犀利/加一段风险数据" />
        <view class="regen-btn dy-clickable" :class="{ disabled: regenerating }" @click="regen">
          <text class="regen-btn-text">{{ regenerating ? '重新生成中…' : '重新生成大纲' }}</text>
        </view>
      </view>

      <view class="footer-bar">
        <view class="btn-plain dy-clickable" @click="goBack"><text class="btn-plain-text">上一步</text></view>
        <view class="btn-primary dy-clickable" @click="confirm">
          <text class="btn-primary-text">{{ confirming ? '确认中…' : '确认大纲，生成正文' }}</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getAiProject, genAiOutline, regenAiOutline, confirmAiOutline } from '@/api/toolAiartist'
import type { AiProject, AiOutline } from '@/types/toolAiartist'
import { purposeLabel } from '@/types/toolAiartist'
import StepProgress from '@/components/StepProgress/StepProgress.vue'

/**
 * 第 3 步：大纲确认。节点卡展示论据溯源，可逐节点编辑、反馈重生成。
 */
interface EditableNode {
  id?: string
  section: string
  corePointsText: string
  argumentsText: string
  imagePromptZh?: string
  imageSize?: string
  imageInsertion?: AiOutline['nodes'][number]['imageInsertion']
  viralTags?: string[]
  editing?: boolean
}

const projectId = ref(0)
const project = ref<AiProject | null>(null)
const nodes = ref<EditableNode[]>([])
const feedback = ref('')
const loading = ref(false)
const regenerating = ref(false)
const confirming = ref(false)

onLoad(async (options: { id?: string }) => {
  projectId.value = Number(options?.id ?? 0)
  if (!projectId.value) { uni.showToast({ title: '参数错误', icon: 'none' }); return }
  loading.value = true
  try {
    project.value = await getAiProject(projectId.value)
    if (!project.value.outline) {
      project.value = await genAiOutline(projectId.value)
    }
    syncNodes()
  } catch { /* 已提示 */ } finally { loading.value = false }
})

function syncNodes() {
  nodes.value = (project.value?.outline?.nodes ?? []).map((n) => ({
    id: n.id,
    section: n.section,
    corePointsText: (n.corePoints ?? []).join('\n'),
    argumentsText: (n.arguments ?? []).join('\n'),
    imagePromptZh: n.imageInsertion?.imagePromptZh,
    imageSize: n.imageInsertion?.size,
    imageInsertion: n.imageInsertion,
    viralTags: n.viralTags,
    editing: false
  }))
}

function splitLines(text: string): string[] {
  return text.split('\n').map((s) => s.trim()).filter(Boolean)
}

function buildOutline(): AiOutline {
  return {
    coverImage: project.value?.outline?.coverImage,
    nodes: nodes.value.map((n) => ({
      id: n.id,
      section: n.section.trim() || '未命名章节',
      corePoints: splitLines(n.corePointsText),
      arguments: splitLines(n.argumentsText),
      viralTags: n.viralTags,
      imageInsertion: n.imageInsertion ?? undefined
    }))
  }
}

async function regen() {
  if (regenerating.value) return
  regenerating.value = true
  try {
    project.value = await regenAiOutline(projectId.value, feedback.value || undefined)
    syncNodes()
    feedback.value = ''
  } catch { /* 已提示 */ } finally { regenerating.value = false }
}

async function confirm() {
  if (confirming.value) return
  confirming.value = true
  try {
    await confirmAiOutline(projectId.value, JSON.stringify(buildOutline()))
    uni.redirectTo({ url: `/pages/acquisition/tools/aiartist/step-body?id=${projectId.value}` })
  } catch { /* 已提示 */ } finally { confirming.value = false }
}

function goBack() { uni.navigateBack() }
</script>

<style scoped lang="scss">
.page { padding: $spacing-md $spacing-md 180rpx; background: $bg-page; min-height: 100vh; }
.state-box { padding: 120rpx 40rpx; display: flex; flex-direction: column; align-items: center; gap: 24rpx; }
.dot-loading { display: flex; gap: 12rpx; }
.dot { width: 14rpx; height: 14rpx; border-radius: 50%; background: $brand-primary; animation: dotBounce 1s infinite ease-in-out; }
.dot:nth-child(2) { animation-delay: .15s; }
.dot:nth-child(3) { animation-delay: .3s; }
@keyframes dotBounce { 0%, 80%, 100% { transform: scale(.6); opacity: .4; } 40% { transform: scale(1); opacity: 1; } }
.state-text { font-size: 26rpx; color: $text-secondary; }
.head-card { padding: $spacing-md; margin-bottom: $spacing-sm; }
.head-title { display: block; font-size: 30rpx; font-weight: 700; color: $text-primary; }
.head-sub { display: block; font-size: 22rpx; color: $text-secondary; margin-top: 8rpx; }
.node-card { padding: $spacing-md; margin-bottom: 16rpx; }
.node-head { display: flex; align-items: center; gap: 16rpx; margin-bottom: 12rpx; }
.node-idx { width: 40rpx; height: 40rpx; border-radius: 50%; background: $brand-primary; color: #fff; font-size: 22rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.node-section { flex: 1; font-size: 28rpx; font-weight: 600; color: $text-primary; }
.node-edit { font-size: 24rpx; color: $brand-primary; }
.node-block { margin-bottom: 12rpx; }
.node-label { display: block; font-size: 22rpx; color: $text-secondary; margin: 8rpx 0; }
.node-line { display: block; font-size: 24rpx; color: $text-regular; line-height: 1.7; }
.node-line.arg { color: $brand-primary-dark; }
.node-img { display: flex; align-items: flex-start; gap: 12rpx; background: rgba(64, 158, 255, .06); border-radius: $radius-sm; padding: 12rpx 16rpx; }
.img-flag { width: 20rpx; height: 20rpx; border-radius: 6rpx; background: $gradient-blue; margin-top: 6rpx; flex-shrink: 0; }
.node-img-text { font-size: 22rpx; color: $text-secondary; line-height: 1.7; }
.regen-box { padding: $spacing-md; margin-bottom: 24rpx; }
.regen-btn { margin-top: 16rpx; height: $control-height-sm; border: 2rpx solid $brand-primary; border-radius: $radius-md; display: flex; align-items: center; justify-content: center; }
.regen-btn.disabled { opacity: .5; }
.regen-btn-text { color: $brand-primary; font-size: 26rpx; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 50; display: flex; gap: 20rpx; padding: 20rpx $spacing-md calc(20rpx + env(safe-area-inset-bottom)); background: $bg-card; box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, .04); }
.btn-primary { flex: 1.6; height: $control-height; background: $gradient-blue; border-radius: $radius-md; display: flex; align-items: center; justify-content: center; }
.btn-primary-text { color: #fff; font-size: 28rpx; font-weight: 600; }
.btn-plain { flex: 1; height: $control-height; border: 2rpx solid $border-base; border-radius: $radius-md; display: flex; align-items: center; justify-content: center; }
.btn-plain-text { color: $text-regular; font-size: 26rpx; }
</style>
