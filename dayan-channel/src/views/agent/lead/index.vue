<script setup lang="ts">
import { onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageAgentClientRels } from '@/api/agent'
import type { AgentClientRel, AgentClientRelQuery } from '@/types/agent'
import { BIND_TYPE_OPTIONS } from '@/types/agent'
import { statusTagType } from '@/utils/format'

/**
 * 客户线索页（只读列表）。
 *
 * - 数据源：pageAgentClientRels（/channel-api/agent-client-rels，任务 6 新建）。
 * - 搜索：代理人编码 / 绑定类型 / 状态。
 * - 表格：agentCode / clientCode / clientName / clientPhone / bindType(text) / status(tag) / bindTime。
 * - 后端端点未实现时降级（空表 + 控制台 warn，不弹 toast）。
 *
 * 说明：useCrud 不返回 handleReset（已核实 composables/useCrud.ts），
 * 故在本页面内自定义 handleReset（与 agent/account 等页面一致）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  AgentClientRel,
  AgentClientRelQuery
>(
  { page: pageAgentClientRels },
  { initialQuery: { agentCode: '', bindType: undefined, status: undefined } }
)

function bindTypeText(v?: number) {
  return BIND_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

function handleReset() {
  query.agentCode = ''
  query.bindType = undefined
  query.status = undefined
  handleSearch()
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[agent/lead] 加载客户线索列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="代理人编码">
          <el-input v-model="query.agentCode" placeholder="代理人编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="绑定类型">
          <el-select v-model="query.bindType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in BIND_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><div class="card-header"><span>客户线索</span></div></template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="agentCode" label="代理人编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="clientCode" label="客户编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="clientName" label="客户姓名" min-width="100" />
        <el-table-column prop="clientPhone" label="客户手机" min-width="120" />
        <el-table-column prop="bindType" label="绑定类型" width="100" align="center">
          <template #default="{ row }">{{ bindTypeText(row.bindType) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.status === 1 ? '有效' : '已解绑' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bindTime" label="绑定时间" min-width="160" />
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
