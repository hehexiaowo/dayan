<template>
  <view class="page dy-safe-bottom">
    <view v-if="!editing && detail" class="detail">
      <view class="head">
        <text class="type-tag">{{ aiContentTypeLabel(detail.contentType) }}</text>
        <text class="time">{{ formatDateTime(detail.createdAt) }}</text>
      </view>
      <text class="title">{{ detail.title }}</text>
      <text v-if="detail.summary" class="summary">{{ detail.summary }}</text>
      <rich-text v-if="detail.contentType === 1" class="body" :nodes="detail.contentBody" />
      <text v-else class="body text">{{ detail.contentBody }}</text>
      <view class="footer-bar">
        <view class="btn-plain dy-clickable" @click="onDelete">删除</view>
        <view class="btn-plain dy-clickable" @click="copyBody">复制全文</view>
        <view class="btn-primary dy-clickable" @click="startEdit">编辑</view>
      </view>
    </view>

    <view v-else-if="editing" class="edit">
      <view class="field">
        <text class="field-label">标题</text>
        <input v-model="form.title" class="field-input" placeholder="标题" />
      </view>
      <view class="field">
        <text class="field-label">摘要</text>
        <textarea v-model="form.summary" class="field-textarea" placeholder="摘要（可空）" :maxlength="500" />
      </view>
      <view class="field">
        <text class="field-label">正文</text>
        <textarea v-model="form.contentBody" class="field-textarea tall" :placeholder="detail?.contentType === 1 ? '输入正文，保存时自动分段为段落格式' : '正文'" />
      </view>
      <view class="footer-bar">
        <view class="btn-plain dy-clickable" @click="editing = false">取消</view>
        <view class="btn-primary dy-clickable" :class="{ disabled: saving }" @click="onSave">保存</view>
      </view>
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
const form = reactive({ title: '', summary: '', contentBody: '' })

onLoad((query) => {
  id.value = Number(query?.id ?? 0)
  loadDetail()
})

async function loadDetail() {
  try {
    detail.value = await getMyContentDetail(id.value)
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

function startEdit() {
  if (!detail.value) return
  form.title = detail.value.title
  form.summary = detail.value.summary ?? ''
  form.contentBody = detail.value.contentType === 1 ? htmlToText(detail.value.contentBody) : detail.value.contentBody
  editing.value = true
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
    editing.value = false
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
.page { padding: 24rpx 24rpx 160rpx; background: $bg-page; min-height: 100vh; }
.detail { background: #fff; border-radius: 16rpx; padding: 32rpx; }
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.type-tag { background: rgba(64, 158, 255, .1); color: $brand-primary; font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; }
.time { font-size: 22rpx; color: #c0c4cc; }
.title { display: block; font-size: 36rpx; font-weight: 600; color: #303133; margin-bottom: 12rpx; }
.summary { display: block; font-size: 26rpx; color: #606266; margin-bottom: 20rpx; }
.body { font-size: 28rpx; color: #303133; line-height: 1.8; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; display: flex; gap: 20rpx; padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom)); background: #fff; box-shadow: 0 -4rpx 16rpx rgba(0,0,0,.04); }
.btn-primary { flex: 1; background: $brand-primary; color: #fff; border-radius: 12rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; font-size: 30rpx; }
.btn-primary.disabled { opacity: .5; }
.btn-plain { flex: 1; background: #f5f7fa; color: #606266; border-radius: 12rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; font-size: 30rpx; }
.field { margin-bottom: 24rpx; }
.field-label { display: block; font-size: 26rpx; color: #606266; margin-bottom: 12rpx; }
.field-input { background: #fff; border-radius: 12rpx; padding: 0 24rpx; height: 80rpx; font-size: 28rpx; }
.field-textarea { background: #fff; border-radius: 12rpx; padding: 20rpx 24rpx; font-size: 28rpx; min-height: 140rpx; width: 100%; box-sizing: border-box; }
.field-textarea.tall { min-height: 360rpx; }
</style>
