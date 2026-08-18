<template>
  <view class="page dy-safe-bottom">
    <!-- ===== 渐变 header（与其他页面统一） ===== -->
    <view class="header">
      <text class="header-title">我的内容</text>
      <text class="header-sub">AI 创作的图文、朋友圈、脚本</text>
    </view>

    <!-- ===== 类型筛选 ===== -->
    <view class="filter-bar">
      <view
        v-for="f in typeFilters"
        :key="f.label"
        class="filter-pill dy-clickable"
        :class="{ active: activeType === f.value }"
        @click="switchType(f.value)"
      >{{ f.label }}</view>
    </view>

    <!-- ===== 内容列表 ===== -->
    <view v-if="list.length" class="list">
      <view v-for="item in list" :key="item.id" class="card dy-clickable" @click="goDetail(item.id)">
        <view class="card-top">
          <view class="tags">
            <text class="type-tag" :class="`tag-${item.contentType}`">{{ aiContentTypeLabel(item.contentType) }}</text>
            <text v-if="item.purpose && AI_PURPOSE_TAG[item.purpose]" class="purpose-tag">{{ AI_PURPOSE_TAG[item.purpose] }}</text>
          </view>
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
import { aiContentTypeLabel, AI_PURPOSE_TAG } from '@/types/aiContent'
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue'

const list = ref<AiContent[]>([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)

const typeFilters = [
  { label: '全部', value: undefined as number | undefined },
  { label: '图文', value: 1 },
  { label: '朋友圈', value: 2 },
  { label: '脚本', value: 3 },
  { label: '小红书', value: 4 }
]
const activeType = ref<number | undefined>(undefined)

function switchType(v: number | undefined) {
  if (loading.value || activeType.value === v) return
  activeType.value = v
  loadList(true)
}

async function loadList(reset = false) {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getMyContents({ current: reset ? 1 : page.value, size: 10, contentType: activeType.value })
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
  uni.navigateTo({ url: '/pages/acquisition/tools/ai-create/index' })
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
.page { padding: $spacing-md; background: $bg-page; min-height: 100vh; }

/* 渐变 header（与其他页面统一） */
.header {
  background: $gradient-blue;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  margin-bottom: $spacing-md;
}
.header-title {
  display: block;
  font-size: 38rpx;
  font-weight: bold;
  color: #fff;
}
.header-sub {
  display: block;
  margin-top: $spacing-sm;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

.filter-bar { display: flex; gap: $spacing-sm; padding: 0 0 20rpx; }
.filter-pill { background: $bg-card; border-radius: 32rpx; padding: 12rpx 36rpx; font-size: 26rpx; color: $text-regular; transition: background-color $transition-base, color $transition-base; }
.filter-pill.active { background: $brand-primary; color: #fff; font-weight: 500; }
.card { background: $bg-card; border-radius: $radius-md; padding: 28rpx; margin-bottom: 20rpx; box-shadow: $shadow-card; transition: transform $transition-fast; }
.card:active { transform: scale(.98); }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.tags { display: flex; gap: 12rpx; align-items: center; }
.type-tag { background: $brand-primary-light; color: $brand-primary-dark; font-size: 22rpx; padding: 4rpx 16rpx; border-radius: $radius-sm; }
.type-tag.tag-2 { background: $brand-success-light; color: $brand-success-dark; }
.type-tag.tag-3 { background: $brand-warning-light; color: $brand-warning-dark; }
.type-tag.tag-4 { background: $brand-error-light; color: $brand-error; }
.purpose-tag { background: $bg-page; color: $text-secondary; font-size: 22rpx; padding: 4rpx 16rpx; border-radius: $radius-sm; }
.time { font-size: 22rpx; color: $text-secondary; }
.title { display: block; font-size: 30rpx; font-weight: 600; color: $text-primary; line-height: 1.5; }
.summary { display: block; margin-top: 8rpx; font-size: 24rpx; color: $text-secondary; }
.card-actions { display: flex; justify-content: flex-end; margin-top: 16rpx; }
.action { font-size: 24rpx; color: $brand-error; padding: 12rpx 28rpx; }
.more { text-align: center; color: $text-secondary; font-size: 24rpx; padding: $spacing-md 0; }
</style>
