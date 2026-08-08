<script setup lang="ts">
import { onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageReadRecords } from '@/api/content'
import type { ContentReadRecord, ContentReadRecordQuery } from '@/types/content'

/**
 * 阅读记录页（只读列表）。
 *
 * - 数据源：pageReadRecords（/channel-api/content-read-records，任务 6 新建）。
 * - 搜索：内容编码（readSource 在 query 初始值但搜索栏未暴露，与 agent/account 的 accountStatus 同模式）。
 * - 表格：contentCode / readerName / readerCode / readSource(text 1代理人2客户) / readDuration / readTime。
 * - 后端端点未实现时降级（空表 + 控制台 warn，不弹 toast）。
 *
 * 说明：useCrud 不返回 handleReset（已核实 composables/useCrud.ts），
 * 故在本页面内自定义 handleReset（与 agent/account 等页面一致）。
 */

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
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="内容编码">
          <el-input v-model="query.contentCode" placeholder="内容编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><div class="card-header"><span>阅读记录</span></div></template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="contentCode" label="内容编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="readerName" label="读者" min-width="100" />
        <el-table-column prop="readerCode" label="读者编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="readSource" label="来源" width="90" align="center">
          <template #default="{ row }">{{ row.readSource === 1 ? '代理人' : row.readSource === 2 ? '客户' : '--' }}</template>
        </el-table-column>
        <el-table-column prop="readDuration" label="阅读时长(秒)" width="120" align="right" />
        <el-table-column prop="readTime" label="阅读时间" min-width="160" />
        <template #empty><el-empty description="暂无数据" /></template>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination :current-page="query.current" :page-size="query.size" :total="total"
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
