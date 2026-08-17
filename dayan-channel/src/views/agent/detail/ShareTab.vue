<script setup lang="ts">
/**
 * 代理人详情页 - 分享记录 tab（只读分页列表）。
 *
 * 手动 ref 分页调 pageShareRecords({ agentCode, current, size })，
 * 表格列：分享编码 / 类型（SHARE_TYPE_OPTIONS）/ 业务编码 / 浏览数 / 分享时间，无操作列。
 * 接口失败 try/catch 降级为空列表。
 */
import { ref, watch } from 'vue'
import { pageShareRecords } from '@/api/agent'
import { SHARE_TYPE_OPTIONS, type ShareRecord } from '@/types/agent'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 代理人编码（路由参数） */
  agentCode: string
}>()

const loading = ref(false)
const tableData = ref<ShareRecord[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)

async function loadPage() {
  if (!props.agentCode) return
  loading.value = true
  try {
    const res = await pageShareRecords({
      agentCode: props.agentCode,
      current: current.value,
      size: size.value
    })
    tableData.value = res.records ?? []
    total.value = res.total ?? 0
  } catch (err) {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[agent/detail ShareTab] 加载分享记录列表失败（接口可能未实现）:', err)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

loadPage()

/** 翻页 */
function handlePageChange(page: number) {
  current.value = page
  loadPage()
}

/** 每页条数变化 */
function handleSizeChange(s: number) {
  size.value = s
  current.value = 1
  loadPage()
}

// 路由参数变化（同一组件实例复用）时回到第 1 页重查
watch(
  () => props.agentCode,
  () => {
    current.value = 1
    loadPage()
  }
)

/** 分享类型文本（1 内容分享 / 2 场景分享 / 3 权益分享） */
function shareTypeText(v?: number): string {
  return SHARE_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}
</script>

<template>
  <div class="share-tab">
    <el-table v-loading="loading" :data="tableData" border stripe row-key="shareCode">
      <el-table-column prop="shareCode" label="分享编码" min-width="160" show-overflow-tooltip />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">{{ shareTypeText(row.shareType) }}</template>
      </el-table-column>
      <el-table-column prop="bizCode" label="业务编码" min-width="160" show-overflow-tooltip />
      <el-table-column prop="viewCount" label="浏览数" width="90" align="right" />
      <el-table-column prop="shareTime" label="分享时间" min-width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.shareTime) }}</template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无数据" />
      </template>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        :current-page="current"
        :page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.share-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
