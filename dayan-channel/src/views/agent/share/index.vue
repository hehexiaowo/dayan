<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageShareRecords } from '@/api/agent'
import type { ShareRecord, ShareRecordQuery } from '@/types/agent'
import { SHARE_TYPE_OPTIONS, shareChannelLabel } from '@/types/agent'
import { formatDateTime } from '@/utils/format'

/**
 * 分享记录页（只读列表）。
 *
 * - 数据源：pageShareRecords（/channel-api/agent-share-records，任务 6 新建）。
 * - 搜索：代理人编码 / 分享类型。
 * - 表格：shareCode / agentCode / shareType(text) / shareChannel(tag) / bizCode / clientName / viewCount / shareTime。
 * - 行内详情弹窗（行数据展开）：el-descriptions 展示全量字段，不调新接口。
 * - 后端端点未实现时降级（空表 + 控制台 warn，不弹 toast）。
 *
 * 说明：useCrud 不返回 handleReset（已核实 composables/useCrud.ts），
 * 故在本页面内自定义 handleReset（与 agent/account 等页面一致）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ShareRecord,
  ShareRecordQuery
>(
  { page: pageShareRecords },
  { initialQuery: { agentCode: '', shareType: undefined } }
)

function shareTypeText(v?: number) {
  return SHARE_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

function handleReset() {
  query.agentCode = ''
  query.shareType = undefined
  handleSearch()
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const currentRow = ref<ShareRecord | null>(null)

function openDetail(row: ShareRecord) {
  currentRow.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[agent/share] 加载分享记录列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input v-model="query.agentCode" placeholder="代理人编码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-select v-model="query.shareType" placeholder="分享类型" clearable style="width: 120px">
          <el-option v-for="o in SHARE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header><div class="card-header"><span class="card-title">分享记录列表</span></div></template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="shareCode">
        <el-table-column prop="shareCode" label="分享编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="agentCode" label="代理人编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="shareType" label="类型" width="90" align="center">
          <template #default="{ row }">{{ shareTypeText(row.shareType) }}</template>
        </el-table-column>
        <el-table-column prop="shareChannel" label="分享渠道" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ shareChannelLabel(row.shareChannel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizCode" label="业务编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="clientName" label="客户" min-width="100" show-overflow-tooltip />
        <el-table-column prop="viewCount" label="浏览次数" width="80" align="right" />
        <el-table-column prop="shareTime" label="分享时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.shareTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无数据" /></template>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
:current-page="query.current" :page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" background
          @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="分享记录详情" width="720px">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="分享编码">{{ currentRow.shareCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="代理人编码">{{ currentRow.agentCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="分享类型">{{ shareTypeText(currentRow.shareType) }}</el-descriptions-item>
        <el-descriptions-item label="业务编码">{{ currentRow.bizCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="分享渠道">{{ currentRow.shareChannel ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="客户编码">{{ currentRow.clientCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ currentRow.clientName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="浏览数">{{ currentRow.viewCount ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="分享时间">{{ formatDateTime(currentRow.shareTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card { :deep(.el-card__body) { padding-bottom: 2px; } }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
