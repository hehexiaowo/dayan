<template>
  <view class="page">
    <!-- 生成中 -->
    <view v-if="generating" class="gen-box dy-card">
      <view class="dot-loading"><view class="dot" /><view class="dot" /><view class="dot" /></view>
      <text class="stage-text">{{ stageText }}</text>
      <scroll-view v-if="streamText" class="stream-preview" scroll-y :scroll-top="scrollTop">
        <text class="stream-text">{{ streamText }}</text><text class="stream-cursor">▍</text>
      </scroll-view>
    </view>

    <template v-else-if="project">
      <view v-if="project.warnings?.length" class="warn-box">
        <text v-for="(w, i) in project.warnings" :key="i" class="warn-text">⚠ {{ w }}</text>
      </view>

      <!-- 正文 -->
      <view class="dy-card body-card">
        <text class="body-title">{{ project.selectedTitle }}</text>
        <!-- H5：图文走 v-html 富文本，其余形态纯文本 -->
        <!-- #ifdef H5 -->
        <view v-if="project.contentType === 1" class="body-html" v-html="project.body" />
        <text v-else class="body-text">{{ project.body }}</text>
        <!-- #endif -->
        <!-- #ifndef H5 -->
        <text class="body-text">{{ project.body }}</text>
        <!-- #endif -->
      </view>

      <!-- 五维打分 -->
      <view v-if="project.scores" class="dy-card score-card">
        <view class="dy-section-title inner">五维打分</view>
        <view class="score-row" v-for="s in scoreRows" :key="s.key">
          <text class="score-label">{{ s.label }}</text>
          <view class="score-bar"><view class="score-fill" :style="{ width: (s.value ?? 0) * 10 + '%' }" /></view>
          <text class="score-num">{{ s.value ?? '-' }}</text>
        </view>
        <text v-if="project.scores.editorCritique" class="critique">主编点评：{{ project.scores.editorCritique }}</text>
      </view>

      <!-- 审计日志 -->
      <view v-if="project.auditLog?.length" class="dy-card audit-card">
        <view class="dy-section-title inner" @click="showAudit = !showAudit">
          审计记录（{{ project.auditLog.length }} 条）<text class="toggle">{{ showAudit ? '收起 ▲' : '展开 ▼' }}</text>
        </view>
        <view v-if="showAudit">
          <view v-for="(a, i) in project.auditLog" :key="i" class="audit-row">
            <text class="audit-type">{{ a.type }}</text>
            <text class="audit-msg">{{ a.message }}</text>
          </view>
        </view>
      </view>

      <!-- 段落勘误 -->
      <view class="dy-card revise-card">
        <view class="dy-section-title inner">发现事实错误？段落勘误</view>
        <input class="dy-input" v-model="reviseAnchor" placeholder="锚文本（可选）：正文中的原句片段" />
        <textarea class="dy-textarea" v-model="reviseFeedback" maxlength="500" placeholder="勘误意见，如：护理额度应为大洋 120 万而不是 100 万" />
        <view class="revise-btn dy-clickable" :class="{ disabled: revising }" @click="doRevise">
          <text class="revise-btn-text">{{ revising ? '修订中…' : '提交勘误（最小化修订）' }}</text>
        </view>
      </view>

      <view class="footer-bar">
        <view class="btn-plain dy-clickable" @click="regen"><text class="btn-plain-text">重新生成</text></view>
        <view v-if="project.contentType === 2" class="btn-primary dy-clickable" @click="save">
          <text class="btn-primary-text">{{ saving ? '保存中…' : '保存到内容中心' }}</text>
        </view>
        <view v-else class="btn-primary dy-clickable" @click="goPreview">
          <text class="btn-primary-text">下一步：配图预览</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getAiProject, reviseAiBody, saveAiProject, genAiBody } from '@/api/aiCreation'
import { postSseStream } from '@/utils/sse'
import type { AiProject } from '@/types/aiCreation'

/**
 * 第 4 步：正文生成（H5 SSE 流式打字机 + 阶段条）→ 审计/打分 → 段落勘误。
 */
const projectId = ref(0)
const project = ref<AiProject | null>(null)
const generating = ref(false)
const stageText = ref('')
const streamText = ref('')
const scrollTop = ref(0)
const showAudit = ref(false)
const reviseAnchor = ref('')
const reviseFeedback = ref('')
const revising = ref(false)
const saving = ref(false)

const scoreRows = computed(() => [
  { key: 'naturalness', label: '表达自然度', value: project.value?.scores?.naturalness },
  { key: 'viralDesign', label: '爆款设计', value: project.value?.scores?.viralDesign },
  { key: 'styleSimilarity', label: '风格贴合', value: project.value?.scores?.styleSimilarity },
  { key: 'emotionalImpact', label: '情绪感染力', value: project.value?.scores?.emotionalImpact },
  { key: 'conversionRate', label: '行动转化', value: project.value?.scores?.conversionRate }
])

onLoad(async (options: any) => {
  projectId.value = Number(options?.id ?? 0)
  if (!projectId.value) { uni.showToast({ title: '参数错误', icon: 'none' }); return }
  project.value = await getAiProject(projectId.value)
  const canGen = ['STRATEGY_CONFIRMED', 'OUTLINE_CONFIRMED'].includes(project.value.status ?? '')
    || (project.value.status === 'BODY_DONE' && !project.value.body)
  if (canGen) await generate()
})

async function generate() {
  if (generating.value) return
  generating.value = true
  streamText.value = ''
  stageText.value = '正在准备素材…'
  // #ifdef H5
  try {
    await postSseStream(`/agent-api/ai/projects/${projectId.value}/body/stream`, {}, {
      onEvent: (name, data) => {
        let parsed: any
        try { parsed = JSON.parse(data) } catch { return }
        if (name === 'stage') {
          stageText.value = parsed.message
        } else if (name === 'delta') {
          streamText.value += parsed.text
          scrollTop.value += 9999
        } else if (name === 'done') {
          project.value = parsed
          streamText.value = ''
        } else if (name === 'error') {
          uni.showToast({ title: parsed.message || '生成失败', icon: 'none' })
        }
      }
    })
  } catch {
    uni.showToast({ title: '生成失败，请稍后重试', icon: 'none' })
  } finally { generating.value = false }
  // #endif
  // #ifndef H5
  const stages = ['正在准备素材…', '正在撰写正文…', '事实核查与合规审计…', '润色去 AI 味…']
  let idx = 0
  stageText.value = stages[0]
  const timer = setInterval(() => { idx = Math.min(idx + 1, stages.length - 1); stageText.value = stages[idx] }, 10000)
  try {
    project.value = await genAiBody(projectId.value)
  } catch { /* 已提示 */ } finally { clearInterval(timer); generating.value = false }
  // #endif
}

function regen() {
  uni.showModal({
    title: '重新生成正文',
    content: '将覆盖当前正文与打分（已有配图将失效），确定？',
    success: async (res) => { if (res.confirm) await generate() }
  })
}

async function doRevise() {
  if (revising.value) return
  if (!reviseFeedback.value.trim()) { uni.showToast({ title: '请填写勘误意见', icon: 'none' }); return }
  revising.value = true
  try {
    project.value = await reviseAiBody(projectId.value, reviseFeedback.value.trim(), reviseAnchor.value.trim() || undefined)
    reviseFeedback.value = ''
    reviseAnchor.value = ''
    uni.showToast({ title: '已修订', icon: 'success' })
  } catch { /* 已提示 */ } finally { revising.value = false }
}

function goPreview() {
  uni.redirectTo({ url: `/pages/acquisition/ai-create/step-preview?id=${projectId.value}` })
}

async function save() {
  if (saving.value) return
  saving.value = true
  try {
    await saveAiProject(projectId.value)
    uni.showToast({ title: '已保存到内容中心', icon: 'success' })
    setTimeout(() => uni.redirectTo({ url: '/pages/acquisition/content/mine' }), 800)
  } catch { /* 已提示 */ } finally { saving.value = false }
}
</script>

<style scoped lang="scss">
.page { padding: $spacing-md $spacing-md 180rpx; background: $bg-page; min-height: 100vh; }
.gen-box { padding: $spacing-md; display: flex; flex-direction: column; gap: 20rpx; }
.dot-loading { display: flex; gap: 12rpx; }
.dot { width: 14rpx; height: 14rpx; border-radius: 50%; background: $brand-primary; animation: dotBounce 1s infinite ease-in-out; }
.dot:nth-child(2) { animation-delay: .15s; }
.dot:nth-child(3) { animation-delay: .3s; }
@keyframes dotBounce { 0%, 80%, 100% { transform: scale(.6); opacity: .4; } 40% { transform: scale(1); opacity: 1; } }
.stage-text { font-size: 26rpx; color: $text-secondary; }
.stream-preview { max-height: 50vh; background: $bg-page; border-radius: $radius-md; padding: 20rpx; }
.stream-text { font-size: 26rpx; color: $text-regular; line-height: 1.8; word-break: break-all; }
.stream-cursor { color: $brand-primary; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0; } }
.warn-box { background: rgba(230, 162, 60, .08); border-radius: $radius-md; padding: 16rpx 24rpx; margin-bottom: 24rpx; }
.warn-text { display: block; font-size: 22rpx; color: $brand-warning; }
.body-card { padding: $spacing-md; margin-bottom: 24rpx; }
.body-title { display: block; font-size: 32rpx; font-weight: 700; color: $text-primary; margin-bottom: 20rpx; }
.body-html { font-size: 28rpx; color: $text-regular; line-height: 1.9; }
.body-text { font-size: 28rpx; color: $text-regular; line-height: 1.9; white-space: pre-wrap; }
.score-card, .audit-card, .revise-card { padding: $spacing-md; margin-bottom: 24rpx; }
.dy-section-title.inner { margin-bottom: 16rpx; }
.score-row { display: flex; align-items: center; gap: 16rpx; margin-bottom: 14rpx; }
.score-label { width: 160rpx; font-size: 24rpx; color: $text-secondary; flex-shrink: 0; }
.score-bar { flex: 1; height: 14rpx; background: $bg-page; border-radius: 999rpx; overflow: hidden; }
.score-fill { height: 100%; background: $gradient-blue; border-radius: 999rpx; }
.score-num { width: 60rpx; text-align: right; font-size: 24rpx; color: $brand-primary; }
.critique { display: block; font-size: 22rpx; color: $text-secondary; margin-top: 8rpx; }
.toggle { font-size: 22rpx; color: $brand-primary; margin-left: 12rpx; }
.audit-row { display: flex; gap: 16rpx; padding: 10rpx 0; border-bottom: 1rpx dashed $border-light; }
.audit-type { font-size: 22rpx; color: $brand-primary; flex-shrink: 0; }
.audit-msg { font-size: 22rpx; color: $text-regular; }
.revise-btn { margin-top: 16rpx; height: $control-height-sm; border: 2rpx solid $brand-primary; border-radius: $radius-md; display: flex; align-items: center; justify-content: center; }
.revise-btn.disabled { opacity: .5; }
.revise-btn-text { color: $brand-primary; font-size: 26rpx; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 50; display: flex; gap: 20rpx; padding: 20rpx $spacing-md calc(20rpx + env(safe-area-inset-bottom)); background: $bg-card; box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, .04); }
.btn-primary { flex: 1.6; height: $control-height; background: $gradient-blue; border-radius: $radius-md; display: flex; align-items: center; justify-content: center; }
.btn-primary-text { color: #fff; font-size: 28rpx; font-weight: 600; }
.btn-plain { flex: 1; height: $control-height; border: 2rpx solid $border-base; border-radius: $radius-md; display: flex; align-items: center; justify-content: center; }
.btn-plain-text { color: $text-regular; font-size: 26rpx; }
</style>
