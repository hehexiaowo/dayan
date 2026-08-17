<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCrud } from '@/composables/useCrud'
import { pageAgentAccounts } from '@/api/agent'
import { AGENT_LEVEL_OPTIONS, type AgentAccount, type AgentAccountQuery } from '@/types/agent'
import { formatDateTime } from '@/utils/format'

/**
 * 代理人账号页。
 *
 * - 数据源：pageAgentAccounts（/channel-api/agent-accounts，任务 6 新建）。
 * - 搜索：关键字（账号/姓名/手机）+ 账号状态。
 * - 表格：agentCode / username / realName / phone / agentLevel(tag) / isCertified(tag) / accountStatus(tag) / lastLoginTime / createdAt。
 * - 行内"详情"跳转代理人详情页（路由 AgentDetail，/agent/detail/:agentCode，tab 式详情）。
 * - 后端端点未实现时降级（空表 + 控制台 warn，不弹 toast）。
 *
 * 说明：useCrud 不返回 handleReset（已核实 composables/useCrud.ts），
 * 故在本页面内自定义 handleReset（与现有 client/index.vue 等页面一致）。
 */

const router = useRouter()

/** 账号状态（DB 现有注释权威）：0 锁定 / 1 正常 / 2 禁用 */
const ACCOUNT_STATUS_OPTIONS = [
  { value: 0, label: '锁定' },
  { value: 1, label: '正常' },
  { value: 2, label: '禁用' }
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

/** 账号状态 tag 颜色：0 锁定 warning / 1 正常 success / 2 禁用 info */
function accountStatusTagType(v?: number): 'warning' | 'success' | 'info' {
  const map: Record<number, 'warning' | 'success' | 'info'> = {
    0: 'warning',
    1: 'success',
    2: 'info'
  }
  return map[v ?? -1] ?? 'info'
}

/** 账号状态中文标签 */
function accountStatusText(v?: number): string {
  return ACCOUNT_STATUS_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

/** 代理人等级 tag 颜色：普通 primary / 银牌 success / 金牌 warning / 钻石 info */
function agentLevelTagType(v?: number): 'primary' | 'success' | 'warning' | 'info' {
  const map: Record<number, 'primary' | 'success' | 'warning' | 'info'> = {
    1: 'primary',
    2: 'success',
    3: 'warning',
    4: 'info'
  }
  return map[v ?? 0] ?? 'info'
}

/** 代理人等级中文标签 */
function agentLevelText(v?: number): string {
  return AGENT_LEVEL_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

/** 跳转代理人详情页（路由 name=AgentDetail，params 携带 agentCode） */
function goDetail(row: AgentAccount) {
  router.push({ name: 'AgentDetail', params: { agentCode: row.agentCode } })
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
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="关键字（账号/姓名/手机）" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="query.accountStatus" placeholder="账号状态" clearable style="width: 130px">
          <el-option v-for="o in ACCOUNT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">代理账号列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="agentCode">
        <el-table-column prop="agentCode" label="账号编码" min-width="140" fixed="left" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="agentLevel" label="等级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="agentLevelTagType(row.agentLevel)">{{ agentLevelText(row.agentLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isCertified" label="认证" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isCertified === 1 ? 'success' : 'info'">{{ row.isCertified === 1 ? '已认证' : '未认证' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="accountStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="accountStatusTagType(row.accountStatus)">{{ accountStatusText(row.accountStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
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
