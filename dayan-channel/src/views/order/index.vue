<script setup lang="ts">
import { onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageOrders } from '@/api/order'
import { ORDER_STATUS_OPTIONS, OrderStatus, type Order, type OrderQuery } from '@/types/order'

/**
 * 订单查询页。
 *
 * - 搜索栏：订单编码 / 订单状态；
 * - el-table：orderCode / orderStatus / payAmount / totalAmount / createdAt；
 * - 后端 GET /channel-api/order-equities 未实现，onMounted 失败时降级（空表，不弹 toast）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  Order,
  OrderQuery
>(
  { page: pageOrders },
  {
    initialQuery: {
      orderCode: '',
      orderStatus: undefined
    }
  }
)

function handleReset() {
  query.orderCode = ''
  query.orderStatus = undefined
  handleSearch()
}

function statusTagType(v?: number): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  switch (v) {
    case OrderStatus.COMPLETED:
      return 'success'
    case OrderStatus.PENDING_PAY:
      return 'warning'
    case OrderStatus.PAID:
    case OrderStatus.DELIVERED:
      return 'primary'
    case OrderStatus.PARTIAL_DELIVERED:
      return 'warning'
    case OrderStatus.CANCELLED:
      return 'info'
    case OrderStatus.REFUNDING:
      return 'danger'
    case OrderStatus.REFUNDED:
      return 'info'
    default:
      return 'info'
  }
}

function statusText(v?: number) {
  const opt = ORDER_STATUS_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[order] 加载订单列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="订单编码">
          <el-input v-model="query.orderCode" placeholder="订单编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="query.orderStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in ORDER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
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
          <span>订单列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="orderCode">
        <el-table-column prop="orderCode" label="订单编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="orderStatus" label="订单状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.orderStatus !== undefined && row.orderStatus !== null" :type="statusTagType(row.orderStatus)">
              {{ statusText(row.orderStatus) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="实付金额（元）" width="130" align="right">
          <template #default="{ row }">{{ row.payAmount != null ? Number(row.payAmount).toFixed(2) : '--' }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="订单总额（元）" width="130" align="right">
          <template #default="{ row }">{{ row.totalAmount != null ? Number(row.totalAmount).toFixed(2) : '--' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
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
