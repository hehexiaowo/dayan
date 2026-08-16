<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageContentRecordRead, deleteContentRecordRead, getContentReadStats } from '@/api/content-sub'
import type { ContentRecordRead, ContentRecordReadQuery, ContentReadStats } from '@/types/content'
import { formatDateTime } from '@/utils/format'

/**
 * 阅读记录 tab：管理端只读 + 删除 + UV/PV 统计（记录由前端上报）。
 */
const props = defineProps<{ contentCode: string }>()

/** 阅读来源映射（对齐 DDL read_source：1=自主浏览, 2=分享链接, 3=推荐, 4=搜索） */
const READ_SOURCE_OPTIONS = [
  { label: '自主浏览', value: 1 },
  { label: '分享链接', value: 2 },
  { label: '推荐', value: 3 },
  { label: '搜索', value: 4 }
] as const

function readSourceLabel(v?: number) {
  return READ_SOURCE_OPTIONS.find((o) => o.value === v)?.label ?? (v != null ? String(v) : '-')
}

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handlePageChange,
  handleSizeChange
} = useCrud<ContentRecordRead, ContentRecordReadQuery, number>(
  {
    page: pageContentRecordRead,
    remove: deleteContentRecordRead
  },
  {
    initialQuery: { readSource: undefined },
    idKey: 'id',
    fixedParams: { contentCode: props.contentCode }
  }
)

const stats = ref<ContentReadStats>({ contentCode: props.contentCode, pv: 0, uv: 0 })

async function loadStats() {
  try {
    stats.value = await getContentReadStats(props.contentCode)
  } catch {
    /* 忽略统计加载失败 */
  }
}

async function handleDelete(row: ContentRecordRead) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该阅读记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteContentRecordRead(row.id)
  ElMessage.success('删除成功')
  loadPage()
  loadStats()
}

onMounted(() => {
  loadStats()
  loadPage()
})
</script>

<template>
  <div>
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="阅读次数（PV）" :value="stats.pv" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="访客数（UV）" :value="stats.uv" />
        </el-card>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="readerCode" label="读者编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="readerType" label="读者类型" width="110" />
      <el-table-column prop="readDuration" label="阅读时长(秒)" width="120" align="right" />
      <el-table-column prop="readProgress" label="进度(%)" width="100" align="right" />
      <el-table-column prop="readSource" label="来源" width="90" align="center">
        <template #default="{ row }">{{ readSourceLabel(row.readSource) }}</template>
      </el-table-column>
      <el-table-column prop="deviceType" label="设备" width="110" show-overflow-tooltip />
      <el-table-column prop="ipAddress" label="IP" min-width="130" show-overflow-tooltip />
      <el-table-column prop="readTime" label="阅读时间" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ formatDateTime(row.readTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 12px">
      <el-pagination
        :current-page="query.current"
        :page-size="query.size"
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
