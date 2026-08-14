<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContent } from '@/api/content'
import type { ContentInfo } from '@/types/content'
import { ContentStatus, CONTENT_TYPE_OPTIONS, CONTENT_STATUS_OPTIONS } from '@/types/content'
import BasicTab from './BasicTab.vue'
import MediaTab from './MediaTab.vue'
import ReadTab from './ReadTab.vue'
import ShareTab from './ShareTab.vue'

/**
 * 内容详情页（多 tab）。
 * 路由 ContentDetail，params: contentCode。
 * tab：基本信息 / 媒体资源 / 阅读记录 / 分享记录，均按 contentCode 关联。
 */
const route = useRoute()
const router = useRouter()
const contentCode = computed(() => route.params.contentCode as string)

const detailLoading = ref(false)
const info = ref<ContentInfo | null>(null)

const tabs = [
  { name: 'basic', label: '基本信息' },
  { name: 'media', label: '媒体资源' },
  { name: 'read', label: '阅读记录' },
  { name: 'share', label: '分享记录' }
] as const
const activeTab = ref<'basic' | 'media' | 'read' | 'share'>('basic')

async function loadDetail() {
  if (!contentCode.value) return
  detailLoading.value = true
  try {
    info.value = await getContent(contentCode.value)
  } catch {
    info.value = null
  } finally {
    detailLoading.value = false
  }
}

function contentTypeLabel(t?: number) {
  return CONTENT_TYPE_OPTIONS.find((o) => o.value === t)?.label ?? '--'
}
function contentStatusLabel(s?: number) {
  return CONTENT_STATUS_OPTIONS.find((o) => o.value === s)?.label ?? '--'
}
function statusTagType(s?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (s) {
    case ContentStatus.PASS:
      return 'success'
    case ContentStatus.PENDING:
      return 'warning'
    case ContentStatus.REJECT:
      return 'danger'
    default:
      return 'info'
  }
}

function goBack() {
  router.back()
}

onMounted(loadDetail)
</script>

<template>
  <div v-loading="detailLoading" class="page-container">
    <el-page-header @back="goBack">
      <template #content>
        <span class="header-title">{{ info?.title ?? '内容详情' }}</span>
        <el-tag v-if="info?.contentCode" size="small" style="margin-left: 12px">{{ info.contentCode }}</el-tag>
      </template>
    </el-page-header>

    <el-card v-if="info" shadow="never">
      <el-descriptions :column="4" border>
        <el-descriptions-item label="类型">
          <el-tag type="info">{{ contentTypeLabel(info.contentType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(info.contentStatus)">{{ contentStatusLabel(info.contentStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="作者">{{ info.authorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="浏览量">{{ info.viewCount ?? 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-card v-else-if="!detailLoading" shadow="never">
      <el-empty :description="`未找到内容（contentCode=${contentCode}）`" />
    </el-card>

    <el-card v-if="info" shadow="never">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" lazy>
          <BasicTab v-if="t.name === 'basic'" :content-code="contentCode" @updated="loadDetail" />
          <MediaTab v-else-if="t.name === 'media'" :content-code="contentCode" />
          <ReadTab v-else-if="t.name === 'read'" :content-code="contentCode" />
          <ShareTab v-else-if="t.name === 'share'" :content-code="contentCode" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.header-title {
  font-size: 16px;
  font-weight: 500;
}
</style>
