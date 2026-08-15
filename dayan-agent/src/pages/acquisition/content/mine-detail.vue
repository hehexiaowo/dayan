<template>
  <view class="page dy-safe-bottom">
    <view v-if="!editing && detail" class="detail">
      <view class="head">
        <text class="type-tag" :class="`tag-${detail.contentType}`">{{ aiContentTypeLabel(detail.contentType) }}</text>
        <text class="time">{{ formatDateTime(detail.createdAt) }}</text>
      </view>
      <text class="title">{{ detail.title }}</text>
      <text v-if="detail.summary" class="summary">{{ detail.summary }}</text>
      <rich-text v-if="detail.contentType === 1" class="body" :nodes="detail.contentBody" />
      <text v-else class="body text">{{ detail.contentBody }}</text>
      <view class="footer-bar">
        <view class="btn-danger dy-clickable" @click="onDelete">删除</view>
        <view class="btn-plain dy-clickable" @click="copyBody">复制全文</view>
        <view class="btn-primary dy-clickable" @click="startEdit">编辑</view>
      </view>
    </view>

    <view v-else-if="editing" class="edit">
      <view class="field">
        <text class="field-label"><text class="req-mark">* </text>标题</text>
        <input v-model="form.title" class="field-input" placeholder="标题" />
      </view>
      <view class="field">
        <text class="field-label">摘要</text>
        <textarea v-model="form.summary" class="field-textarea" placeholder="摘要（可空）" :maxlength="500" />
      </view>
      <view class="field">
        <text class="field-label"><text class="req-mark">* </text>正文</text>
        <textarea v-model="form.contentBody" class="field-textarea tall" :placeholder="detail?.contentType === 1 ? '输入正文，保存时自动分段为段落格式' : '正文'" />
      </view>
      <view class="footer-bar">
        <view class="btn-plain dy-clickable" @click="exitEdit">取消</view>
        <view class="btn-primary dy-clickable" :class="{ disabled: saving }" @click="onSave">保存</view>
      </view>
    </view>

    <view v-else-if="loadFailed" class="state-box">
      <text class="state-text">内容加载失败</text>
      <view class="btn-plain state-btn dy-clickable" @click="loadDetail">重新加载</view>
    </view>

    <view v-else class="state-box">
      <view class="dot-loading">
        <view class="dl-dot" />
        <view class="dl-dot" />
        <view class="dl-dot" />
      </view>
      <text class="state-text">正在加载…</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getMyContentDetail, updateAiContent, deleteAiContent } from '@/api/aiContent'
import type { AiContent } from '@/types/aiContent'
import { aiContentTypeLabel } from '@/types/aiContent'
import { copyText } from '@/utils/clipboard'
import { htmlToText } from '@/utils/htmlToText'

const id = ref(0)
const detail = ref<AiContent | null>(null)
const editing = ref(false)
const saving = ref(false)
const loadFailed = ref(false)
const form = reactive({ title: '', summary: '', contentBody: '' })

onLoad((query) => {
  id.value = Number(query?.id ?? 0)
  loadDetail()
})

async function loadDetail() {
  loadFailed.value = false
  try {
    detail.value = await getMyContentDetail(id.value)
  } catch {
    loadFailed.value = true
  }
}

function setNavTitle(title: string) {
  uni.setNavigationBarTitle({ title })
}

function startEdit() {
  if (!detail.value) return
  form.title = detail.value.title
  form.summary = detail.value.summary ?? ''
  form.contentBody = detail.value.contentType === 1 ? htmlToText(detail.value.contentBody) : detail.value.contentBody
  editing.value = true
  setNavTitle('编辑内容')
}

function exitEdit() {
  editing.value = false
  setNavTitle('内容详情')
}

async function copyBody() {
  if (!detail.value) return
  const text = detail.value.contentType === 1 ? htmlToText(detail.value.contentBody) : detail.value.contentBody
  try {
    await copyText(text)
    uni.showToast({ title: '已复制全文', icon: 'success' })
  } catch {
    uni.showToast({ title: '复制失败', icon: 'none' })
  }
}

/** 图文正文保存：已是 HTML（含标签）直接存原文；纯文本按换行转段落 */
function toParagraphHtml(text: string): string {
  if (/<[a-zA-Z/][^>]*>/.test(text)) {
    return text
  }
  return text
    .split(/\n+/)
    .map((p) => p.trim())
    .filter(Boolean)
    .map((p) => `<p>${p}</p>`)
    .join('')
}

async function onSave() {
  if (!form.title.trim()) {
    uni.showToast({ title: '标题不能为空', icon: 'none' })
    return
  }
  if (!form.contentBody.trim()) {
    uni.showToast({ title: '正文不能为空', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const body = detail.value?.contentType === 1 ? toParagraphHtml(form.contentBody) : form.contentBody
    await updateAiContent(id.value, {
      title: form.title.trim(),
      summary: form.summary.trim() || undefined,
      contentBody: body
    })
    uni.showToast({ title: '已保存', icon: 'success' })
    exitEdit()
    await loadDetail()
  } finally {
    saving.value = false
  }
}

function onDelete() {
  uni.showModal({
    title: '删除内容',
    content: '确定删除该内容？删除后不可恢复',
    confirmColor: '#f56c6c',
    success: async (res) => {
      if (!res.confirm) return
      await deleteAiContent(id.value)
      uni.showToast({ title: '已删除', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 500)
    }
  })
}

function formatDateTime(dt?: string): string {
  if (!dt) return ''
  // 后端返回 "2026-08-12T10:30:00" 或 "2026-08-12 10:30:00"
  return dt.length >= 16 ? dt.substring(0, 16).replace('T', ' ') : dt
}
</script>

<style lang="scss" scoped>
.page { padding: $spacing-md $spacing-md 180rpx; background: $bg-page; min-height: 100vh; }
.detail { background: $bg-card; border-radius: $radius-md; padding: $spacing-lg; box-shadow: $shadow-card; }
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-sm; }
.type-tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: $radius-sm; background: $brand-primary-light; color: $brand-primary-dark; }
.type-tag.tag-2 { background: $brand-success-light; color: $brand-success-dark; }
.type-tag.tag-3 { background: $brand-warning-light; color: $brand-warning-dark; }
.time { font-size: 22rpx; color: $text-secondary; }
.title { display: block; font-size: 36rpx; font-weight: 600; color: $text-primary; line-height: 1.4; margin-bottom: 12rpx; }
.summary { display: block; font-size: 26rpx; color: $text-regular; line-height: 1.7; background: $bg-page; border-left: 6rpx solid $brand-primary; border-radius: $radius-sm; padding: 16rpx 20rpx; margin-bottom: $spacing-md; }
.body { display: block; font-size: 28rpx; color: $text-primary; line-height: 1.8; word-break: break-word; }
.body.text { white-space: pre-wrap; }
// AI 图文正文基础排版（rich-text 渲染的 h2/p/ul/li）
.body :deep(h2) { font-size: 32rpx; font-weight: 600; margin: 32rpx 0 16rpx; color: $text-primary; }
.body :deep(p) { margin: 0 0 20rpx; }
.body :deep(ul), .body :deep(ol) { margin: 0 0 20rpx; padding-left: 40rpx; }
.body :deep(li) { margin-bottom: 8rpx; }

.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 50; display: flex; gap: 20rpx; padding: 20rpx $spacing-md calc(20rpx + env(safe-area-inset-bottom)); background: $bg-card; box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, .04); }
.btn-primary, .btn-plain, .btn-danger { flex: 1; border-radius: $radius-md; height: $control-height; display: flex; align-items: center; justify-content: center; font-size: 30rpx; transition: opacity $transition-fast, transform $transition-fast; }
.btn-primary { background: $brand-primary; color: #fff; }
.btn-plain { background: $bg-page; color: $text-regular; }
.btn-danger { background: $brand-error-light; color: $brand-error; }
.btn-primary.disabled { opacity: .5; pointer-events: none; }
.btn-primary:active, .btn-plain:active, .btn-danger:active { transform: scale(.97); opacity: .85; }

.field { margin-bottom: $spacing-md; }
.field-label { display: block; font-size: 26rpx; color: $text-regular; margin-bottom: 12rpx; }
.req-mark { color: $brand-error; }
.field-input { background: $bg-card; border-radius: $radius-md; padding: 0 $spacing-md; height: 80rpx; font-size: 28rpx; }
.field-textarea { background: $bg-card; border-radius: $radius-md; padding: 20rpx $spacing-md; font-size: 28rpx; min-height: 140rpx; width: 100%; box-sizing: border-box; }
.field-textarea.tall { min-height: 360rpx; }

// 加载/失败态
.state-box { display: flex; flex-direction: column; align-items: center; gap: $spacing-md; padding: 200rpx 0; }
.state-text { font-size: 26rpx; color: $text-secondary; }
.state-btn { flex: none; padding: 0 64rpx; }
.dot-loading { display: flex; gap: 12rpx; }
.dl-dot { width: 16rpx; height: 16rpx; border-radius: 50%; background: $brand-primary; animation: dl-bounce 1s ease-in-out infinite; }
.dl-dot:nth-child(2) { animation-delay: .15s; }
.dl-dot:nth-child(3) { animation-delay: .3s; }
@keyframes dl-bounce {
  0%, 100% { transform: translateY(0); opacity: .5; }
  50% { transform: translateY(-12rpx); opacity: 1; }
}
</style>
