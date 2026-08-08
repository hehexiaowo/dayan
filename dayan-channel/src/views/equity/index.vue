<script setup lang="ts">
import { onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageEquities } from '@/api/equity'
import { EQUITY_STATUS_OPTIONS, EquityStatus, type Equity, type EquityQuery } from '@/types/equity'

/**
 * 权益综合页（业务运营目录）。
 *
 * - 搜索栏：权益编码 / 权益状态 / 关联客户编码；
 * - el-table：equityCode / equityStatus / equityType / equityValue / expireTime / clientCode；
 * - 后端 GET /channel-api/equities 未实现，onMounted 失败时降级（空表，不弹 toast）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  Equity,
  EquityQuery
>(
  { page: pageEquities },
  {
    initialQuery: {
      equityCode: '',
      equityStatus: undefined,
      clientCode: ''
    }
  }
)

function handleReset() {
  query.equityCode = ''
  query.equityStatus = undefined
  query.clientCode = ''
  handleSearch()
}

function statusTagType(v?: number): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  switch (v) {
    case EquityStatus.ACTIVATED:
    case EquityStatus.IN_USE:
      return 'success'
    case EquityStatus.STOCK:
    case EquityStatus.OUTBOUND:
      return 'warning'
    case EquityStatus.COMPLETED:
      return 'primary'
    case EquityStatus.EXPIRED:
    case EquityStatus.VOID:
      return 'danger'
    case EquityStatus.CHANGING_HOLDER:
      return 'info'
    default:
      return 'info'
  }
}

function statusText(v?: number) {
  const opt = EQUITY_STATUS_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[equity] 加载权益列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="权益编码">
          <el-input v-model="query.equityCode" placeholder="权益编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="权益状态">
          <el-select v-model="query.equityStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in EQUITY_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户编码">
          <el-input v-model="query.clientCode" placeholder="关联客户编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>权益列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="equityCode">
        <el-table-column prop="equityCode" label="权益编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="equityStatus" label="权益状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.equityStatus !== undefined && row.equityStatus !== null" :type="statusTagType(row.equityStatus)">
              {{ statusText(row.equityStatus) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="equityType" label="权益类型" min-width="120" show-overflow-tooltip />
        <el-table-column prop="equityValue" label="权益价值（元）" width="130" align="right">
          <template #default="{ row }">{{ row.equityValue != null ? Number(row.equityValue).toFixed(2) : '--' }}</template>
        </el-table-column>
        <el-table-column prop="expireTime" label="到期时间" min-width="160" />
        <el-table-column prop="clientCode" label="客户编码" min-width="140" show-overflow-tooltip />
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>

      <div class="pagination-wrap">
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
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
