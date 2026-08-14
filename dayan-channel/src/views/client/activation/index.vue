<script setup lang="ts">
import { onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageEquityActivates } from '@/api/equity'
import type { EquityActivate, EquityActivateQuery } from '@/types/equity'
import { statusTagType } from '@/utils/format'

/**
 * 激活记录页（只读列表）。
 *
 * - 数据源：pageEquityActivates（/channel-api/equity-activates，任务 6 新建）。
 * - 搜索：激活码 / 权益码 / 客户编码。
 * - 表格：activateCode / equityCode / goodsCode / clientFullName / clientPhone / activateTime / expireTime / isIdCardVerified(tag)。
 * - 后端端点未实现时降级（空表 + 控制台 warn，不弹 toast）。
 *
 * 说明：useCrud 不返回 handleReset（已核实 composables/useCrud.ts），
 * 故在本页面内自定义 handleReset（与 agent/account 等页面一致）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  EquityActivate,
  EquityActivateQuery
>(
  { page: pageEquityActivates },
  { initialQuery: { activateCode: '', equityCode: '', clientCode: '' } }
)

function handleReset() {
  query.activateCode = ''
  query.equityCode = ''
  query.clientCode = ''
  handleSearch()
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[client/activation] 加载激活记录列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="激活码">
          <el-input v-model="query.activateCode" placeholder="激活码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="权益码">
          <el-input v-model="query.equityCode" placeholder="权益码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="客户编码">
          <el-input v-model="query.clientCode" placeholder="客户编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><div class="card-header"><span>激活记录</span></div></template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="activateCode">
        <el-table-column prop="activateCode" label="激活码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="equityCode" label="权益码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="goodsCode" label="商品编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="clientFullName" label="客户" min-width="100" />
        <el-table-column prop="clientPhone" label="手机" min-width="120" />
        <el-table-column prop="activateTime" label="激活时间" min-width="160" />
        <el-table-column prop="expireTime" label="过期时间" min-width="160" />
        <el-table-column prop="isIdCardVerified" label="实名" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.isIdCardVerified === 1 ? 2 : 0)">{{ row.isIdCardVerified === 1 ? '是' : '否' }}</el-tag>
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
  </div>
</template>

<style scoped lang="scss">
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card { :deep(.el-card__body) { padding-bottom: 2px; } }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
