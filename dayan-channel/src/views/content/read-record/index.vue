<script setup lang="ts">
import { onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageReadRecords } from '@/api/content'
import type { ContentReadRecord, ContentReadRecordQuery } from '@/types/content'
import { READ_SOURCE_OPTIONS, READER_TYPE_OPTIONS } from '@/types/content'
import { formatDateTime } from '@/utils/format'

/**
 * 阅读记录页（只读列表）。
 *
 * - 数据源：pageReadRecords（/channel-api/content-read-records，任务 6 新建）。
 * - 搜索：内容编码 + 来源（readSource 下拉）。
 * - 表格：contentCode / readerType(tag) / readerCode / readSource(tag) / readDuration / readTime。
 * - 后端端点未实现时降级（空表 + 控制台 warn，不弹 toast）。
 *
 * 说明：useCrud 不返回 handleReset（已核实 composables/useCrud.ts），
 * 故在本页面内自定义 handleReset（与 agent/account 等页面一致）。
 */

/** 阅读来源中文标签 */
function readSourceText(v?: number): string {
  return READ_SOURCE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

/** 阅读来源 tag 颜色：1 自主浏览 primary / 2 分享链接 success / 3 推荐 warning / 4 搜索 info */
function readSourceTagType(v?: number): 'primary' | 'success' | 'warning' | 'info' {
  const map: Record<number, 'primary' | 'success' | 'warning' | 'info'> = {
    1: 'primary',
    2: 'success',
    3: 'warning',
    4: 'info'
  }
  return map[v ?? -1] ?? 'info'
}

/** 读者类型中文标签 */
function readerTypeText(v?: string): string {
  return READER_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

/** 读者类型 tag 颜色：agent 代理人 primary / client 客户 success / butler 管家 warning / guest 访客 info */
function readerTypeTagType(v?: string): 'primary' | 'success' | 'warning' | 'info' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info'> = {
    agent: 'primary',
    client: 'success',
    butler: 'warning',
    guest: 'info'
  }
  return map[v ?? ''] ?? 'info'
}

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ContentReadRecord,
  ContentReadRecordQuery
>(
  { page: pageReadRecords },
  {
    initialQuery: {
      contentCode: '',
      readSource: undefined
    }
  }
)

function handleReset() {
  query.contentCode = ''
  query.readSource = undefined
  handleSearch()
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[content/read-record] 加载阅读记录列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input v-model="query.contentCode" placeholder="内容编码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-select v-model="query.readSource" placeholder="来源" clearable style="width: 130px">
          <el-option v-for="o in READ_SOURCE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header><div class="card-header"><span class="card-title">阅读记录列表</span></div></template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="contentCode" label="内容编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="readerType" label="读者类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="readerTypeTagType(row.readerType)">{{ readerTypeText(row.readerType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="readerCode" label="读者编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="readSource" label="来源" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="readSourceTagType(row.readSource)">{{ readSourceText(row.readSource) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="readDuration" label="阅读时长(秒)" width="120" align="right" />
        <el-table-column prop="readTime" label="阅读时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.readTime) }}</template>
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
  </div>
</template>

<style scoped lang="scss">
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card { :deep(.el-card__body) { padding-bottom: 2px; } }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
