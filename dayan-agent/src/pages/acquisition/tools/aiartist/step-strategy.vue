<template>
  <view class="page">
    <!-- 步骤条 -->
    <StepProgress :current="2" />

    <!-- 生成中 -->
    <view v-if="loading" class="state-box">
      <view class="dot-loading"><view class="dot" /><view class="dot" /><view class="dot" /></view>
      <text class="state-text">{{ stageText }}</text>
    </view>

    <template v-else-if="project">
      <!-- 素材摘要（透明可查） -->
      <view class="dy-section-title" @click="showDigest = !showDigest">
        素材硬数据清单（{{ digestCount }} 条）<text class="toggle">{{ showDigest ? '收起 ▲' : '展开 ▼' }}</text>
      </view>
      <view v-if="showDigest" class="dy-card digest-card">
        <view v-for="(f, i) in project.factDigest?.hardFacts ?? []" :key="i" class="fact-row">
          <text class="fact-text">{{ f.fact }}</text>
          <text class="fact-src">{{ f.source }}</text>
        </view>
        <view v-if="project.factDigest?.missing?.length" class="missing">
          <text class="missing-title">素材缺失提示</text>
          <text v-for="(m, i) in project.factDigest.missing" :key="i" class="missing-text">· {{ m }}</text>
        </view>
      </view>
      <view v-if="project.warnings?.length" class="warn-box">
        <text v-for="(w, i) in project.warnings" :key="i" class="warn-text">⚠ {{ w }}</text>
      </view>

      <!-- 策略面板（可编辑） -->
      <view class="dy-section-title">写作策略（可修改后确认）</view>
      <view class="dy-card panel-card">
        <text class="field-label">受众画像</text>
        <textarea class="dy-textarea" v-model="form.targetAudience" maxlength="500" auto-height />
        <text class="field-label">核心痛点</text>
        <textarea class="dy-textarea" v-model="form.corePainPoint" maxlength="500" auto-height />
        <text class="field-label">爆款逻辑</text>
        <textarea class="dy-textarea" v-model="form.viralLogic" maxlength="500" auto-height />
        <text class="field-label">优势放大器</text>
        <textarea class="dy-textarea" v-model="form.advantageHook" maxlength="800" auto-height />
      </view>

      <!-- 5 标题 -->
      <view class="dy-section-title">选择标题（第 1 步：五选一）</view>
      <view v-for="(t, i) in project.titles ?? []" :key="i" class="title-card dy-card dy-clickable"
        :class="{ picked: pickedTitle === t.title }" @click="pickedTitle = t.title">
        <view class="title-top">
          <text class="dy-tag" :class="t.tag === 'emotion_hook' ? 'dy-tag-orange' : 'dy-tag-blue'">
            {{ AI_TITLE_TAG_LABELS[t.tag ?? ''] ?? t.tag }}
          </text>
          <text class="title-score">传播力 {{ t.viralScore }}</text>
        </view>
        <text class="title-text">{{ t.title }}</text>
        <text v-if="t.reasoning" class="title-reason">{{ t.reasoning }}</text>
      </view>

      <!-- 重生成反馈 -->
      <view class="regen-box dy-card">
        <textarea class="dy-textarea" v-model="titleFeedback" maxlength="200" placeholder="对标题不满意？写点修改方向（可选），如：再犀利一点 / 突出数字" />
        <view class="regen-btn dy-clickable" :class="{ disabled: regenerating }" @click="regenTitles">
          <text class="regen-btn-text">{{ regenerating ? '重新生成中…' : '换一批标题' }}</text>
        </view>
      </view>

      <view class="footer-bar">
        <view class="btn-plain dy-clickable" @click="regenStrategy"><text class="btn-plain-text">重新生成策略</text></view>
        <view class="btn-primary dy-clickable" @click="confirm">
          <text class="btn-primary-text">{{ confirming ? '确认中…' : (contentType === 2 ? '确认并写正文' : '确认并生成大纲') }}</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getAiProject, genAiStrategy, regenAiTitles, confirmAiStrategy } from '@/api/toolAiartist'
import type { AiProject } from '@/types/toolAiartist'
import { AI_TITLE_TAG_LABELS } from '@/types/toolAiartist'
import StepProgress from '@/components/StepProgress/StepProgress.vue'

/**
 * 第 2 步：素材消化 + 策略确认 + 五标题选择。
 * 首进自动生成策略；策略字段可编辑；标题支持反馈重出（策略锁定）。
 */
const projectId = ref(0)
const project = ref<AiProject | null>(null)
const loading = ref(false)
const stageText = ref('正在消化素材、制定策略…')
const showDigest = ref(false)
const form = ref({ targetAudience: '', corePainPoint: '', viralLogic: '', advantageHook: '' })
const pickedTitle = ref('')
const titleFeedback = ref('')
const regenerating = ref(false)
const confirming = ref(false)

const contentType = computed(() => project.value?.contentType ?? 1)
const digestCount = computed(() => project.value?.factDigest?.hardFacts?.length ?? 0)

onLoad(async (options: any) => {
  projectId.value = Number(options?.id ?? 0)
  if (!projectId.value) { uni.showToast({ title: '参数错误', icon: 'none' }); return }
  await load(true)
})

async function load(autoGen: boolean) {
  loading.value = true
  try {
    project.value = await getAiProject(projectId.value)
    syncForm()
    if (!project.value.strategy && autoGen) {
      await generate()
    }
  } finally { loading.value = false }
}

function syncForm() {
  const s = project.value?.strategy
  form.value = {
    targetAudience: s?.targetAudience ?? '',
    corePainPoint: s?.corePainPoint ?? '',
    viralLogic: s?.viralLogic ?? '',
    advantageHook: s?.advantageHook ?? ''
  }
  if (project.value?.selectedTitle) pickedTitle.value = project.value.selectedTitle
}

async function generate() {
  loading.value = true
  stageText.value = '正在消化素材、制定策略…'
  try {
    project.value = await genAiStrategy(projectId.value)
    syncForm()
  } catch { /* 已提示 */ } finally { loading.value = false }
}

function regenStrategy() {
  uni.showModal({
    title: '重新生成策略',
    content: '将清空后续已产出的内容（如有），确定？',
    success: async (res) => { if (res.confirm) await generate() }
  })
}

async function regenTitles() {
  if (regenerating.value) return
  regenerating.value = true
  try {
    project.value = await regenAiTitles(projectId.value, titleFeedback.value || undefined)
    pickedTitle.value = ''
    titleFeedback.value = ''
    uni.showToast({ title: '已换一批', icon: 'none' })
  } catch { /* 已提示 */ } finally { regenerating.value = false }
}

async function confirm() {
  if (confirming.value) return
  if (!pickedTitle.value) { uni.showToast({ title: '请选择一个标题', icon: 'none' }); return }
  confirming.value = true
  try {
    await confirmAiStrategy(projectId.value, { selectedTitle: pickedTitle.value, ...form.value })
    const next = contentType.value === 2
      ? '/pages/acquisition/tools/aiartist/step-body'
      : '/pages/acquisition/tools/aiartist/step-outline'
    uni.redirectTo({ url: `${next}?id=${projectId.value}` })
  } catch { /* 已提示 */ } finally { confirming.value = false }
}
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
.toggle { font-size: 22rpx; color: $brand-primary; margin-left: 12rpx; }
.digest-card { padding: $spacing-md; margin-bottom: $spacing-sm; }
.fact-row { padding: 12rpx 0; border-bottom: 1rpx dashed $border-light; }
.fact-text { display: block; font-size: 26rpx; color: $text-primary; }
.fact-src { display: block; font-size: 20rpx; color: $text-placeholder; margin-top: 4rpx; }
.missing { margin-top: 16rpx; }
.missing-title { display: block; font-size: 24rpx; color: $brand-warning; margin-bottom: 8rpx; }
.missing-text { display: block; font-size: 22rpx; color: $text-secondary; }
.warn-box { background: rgba(230, 162, 60, .08); border-radius: $radius-md; padding: 16rpx 24rpx; margin-bottom: 24rpx; }
.warn-text { display: block; font-size: 22rpx; color: $brand-warning; }
.panel-card { padding: $spacing-md; }
.field-label { display: block; font-size: 24rpx; color: $text-secondary; margin: 16rpx 0 8rpx; }
.title-card { padding: $spacing-md; margin-bottom: 16rpx; border: 2rpx solid transparent; }
.title-card.picked { border-color: $brand-primary; }
.title-top { display: flex; justify-content: space-between; align-items: center; }
.title-score { font-size: 22rpx; color: $text-secondary; }
.title-text { display: block; font-size: 30rpx; font-weight: 600; color: $text-primary; margin: 12rpx 0 8rpx; }
.title-reason { display: block; font-size: 22rpx; color: $text-secondary; }
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
