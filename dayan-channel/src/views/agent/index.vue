<script setup lang="ts">
import { onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageAgents } from '@/api/agent'
import {
  AGENT_LEVEL_OPTIONS,
  AGENT_STATUS_OPTIONS,
  AgentStatus,
  type Agent,
  type AgentQuery
} from '@/types/agent'

/**
 * 代理人管理页。
 *
 * - 搜索栏：代理人名称 / 手机号 / 等级 / 状态；
 * - el-table：agentCode / agentName / realName / phone / agentLevel / agentStatus；
 * - 后端 GET /channel-api/agents 未实现，onMounted 失败时降级（空表，不弹 toast）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  Agent,
  AgentQuery
>(
  { page: pageAgents },
  {
    initialQuery: {
      agentCode: '',
      agentName: '',
      phone: '',
      agentLevel: undefined,
      agentStatus: undefined
    }
  }
)

function handleReset() {
  query.agentCode = ''
  query.agentName = ''
  query.phone = ''
  query.agentLevel = undefined
  query.agentStatus = undefined
  handleSearch()
}

function levelText(v?: number) {
  const opt = AGENT_LEVEL_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

function statusTagType(v?: number) {
  return v === AgentStatus.ENABLED ? 'success' : v === AgentStatus.DISABLED ? 'info' : '-'
}

function statusText(v?: number) {
  const opt = AGENT_STATUS_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[agent] 加载代理人列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="代理人编码">
          <el-input v-model="query.agentCode" placeholder="代理人编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.agentName" placeholder="代理人名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="手机号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="query.agentLevel" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in AGENT_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.agentStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in AGENT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>代理人列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="agentCode">
        <el-table-column prop="agentCode" label="代理人编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="agentName" label="代理人名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="agentLevel" label="等级" width="90" align="center">
          <template #default="{ row }">{{ levelText(row.agentLevel) }}</template>
        </el-table-column>
        <el-table-column prop="agentStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.agentStatus !== undefined && row.agentStatus !== null" :type="statusTagType(row.agentStatus)">
              {{ statusText(row.agentStatus) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
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
