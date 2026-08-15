<template>
  <view class="page dy-safe-bottom">
    <view v-if="list.length" class="list">
      <view v-for="item in list" :key="item.id" class="card dy-clickable" @click="goDetail(item.id)">
        <view class="card-top">
          <text class="type-tag">{{ aiContentTypeLabel(item.contentType) }}</text>
          <text class="time">{{ formatDateTime(item.createdAt) }}</text>
        </view>
        <text class="title">{{ item.title }}</text>
        <text v-if="item.summary" class="summary one-line">{{ item.summary }}</text>
        <view class="card-actions">
          <text class="action dy-clickable" @click.stop="onDelete(item)">删除</text>
        </view>
      </view>
      <view v-if="loading" class="more">加载中…</view>
      <view v-else-if="list.length < total" class="more dy-clickable" @click="loadMore">加载更多</view>
    </view>
    <DyEmpty v-else-if="!loading" text="还没有生成的内容，去 AI 创作试试" icon="AI" color="blue" action-text="去创作" @action="goCreate" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getMyContents, deleteAiContent } from '@/api/aiContent'
import type { AiContent } from '@/types/aiContent'
import { aiContentTypeLabel } from '@/types/aiContent'
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue'

const list = ref<AiContent[]>([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)

async function loadList(reset = false) {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getMyContents({ current: reset ? 1 : page.value, size: 10 })
    list.value = reset ? res.records : [...list.value, ...res.records]
    total.value = res.total
    page.value = reset ? 1 : page.value + 1
  } catch {
    // 全局拦截器已提示
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  await loadList()
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/acquisition/content/mine-detail?id=${id}` })
}

function goCreate() {
  uni.navigateTo({ url: '/pages/acquisition/ai-generate/index' })
}

function onDelete(item: AiContent) {
  uni.showModal({
    title: '删除内容',
    content: `确定删除「${item.title}」？删除后不可恢复`,
    confirmColor: '#f56c6c',
    success: async (res) => {
      if (!res.confirm) return
      await deleteAiContent(item.id)
      uni.showToast({ title: '已删除', icon: 'success' })
      loadList(true)
    }
  })
}

function formatDateTime(dt?: string): string {
  if (!dt) return ''
  // 后端返回 "2026-08-12T10:30:00" 或 "2026-08-12 10:30:00"
  return dt.length >= 16 ? dt.substring(0, 16).replace('T', ' ') : dt
}

onShow(() => loadList(true))

async function handlePullDown() {
  await loadList(true)
  uni.stopPullDownRefresh()
}

onPullDownRefresh(handlePullDown)
</script>

<style lang="scss" scoped>
.page { padding: 24rpx; background: $bg-page; min-height: 100vh; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.type-tag { background: rgba(64, 158, 255, .1); color: $brand-primary; font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; }
.time { font-size: 22rpx; color: #c0c4cc; }
.title { display: block; font-size: 30rpx; font-weight: 600; color: #303133; line-height: 1.5; }
.summary { display: block; margin-top: 8rpx; font-size: 24rpx; color: #909399; }
.card-actions { display: flex; justify-content: flex-end; margin-top: 16rpx; }
.action { font-size: 24rpx; color: #f56c6c; padding: 8rpx 20rpx; }
.more { text-align: center; color: #909399; font-size: 24rpx; padding: 24rpx 0; }
</style>
