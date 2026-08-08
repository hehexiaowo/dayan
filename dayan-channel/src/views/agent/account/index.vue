<script setup lang="ts">
import { onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageAgentAccounts } from '@/api/agent'
import type { AgentAccount, AgentAccountQuery } from '@/types/agent'
import { statusTagType } from '@/utils/format'

/**
 * 代理人账号页。
 *
 * - 数据源：pageAgentAccounts（/channel-api/agent-accounts，任务 6 新建）。
 * - 搜索：关键字（账号/姓名/手机）+ 账号状态。
 * - 表格：agentCode / username / realName / phone / agentLevel / accountStatus(tag) / createdAt。
 * - 后端端点未实现时降级（空表 + 控制台 warn，不弹 toast）。
 *
 * 说明：useCrud 不返回 handleReset（已核实 composables/useCrud.ts），
 * 故在本页面内自定义 handleReset（与现有 client/index.vue 等页面一致）。
 */

const ACCOUNT_STATUS_OPTIONS = [
  { value: 1, label: '正常' },
  { value: 0, label: '禁用' }
]

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  AgentAccount,
  AgentAccountQuery
>(
  { page: pageAgentAccounts },
  {
    initialQuery: {
      keyword: '',
      accountStatus: undefined
    }
  }
)

function handleReset() {
  query.keyword = ''
  query.accountStatus = undefined
  handleSearch()
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[agent/account] 加载代理人账号列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="账号/姓名/手机" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select v-model="query.accountStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in ACCOUNT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>代理账号</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="agentCode">
        <el-table-column prop="agentCode" label="账号编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="agentLevel" label="等级" width="80" align="center" />
        <el-table-column prop="accountStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.accountStatus)">{{ row.accountStatus === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
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
