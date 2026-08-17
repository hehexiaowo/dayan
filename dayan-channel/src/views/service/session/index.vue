<script setup lang="ts">
import { onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageServiceSessions } from '@/api/service'
import type { ServiceSession, ServiceSessionQuery } from '@/types/service'
import { SERVICE_TYPE_OPTIONS, SESSION_STATUS_OPTIONS } from '@/types/service'
import { formatDateTime, statusTagType } from '@/utils/format'

/**
 * 服务记录页（只读列表）。
 *
 * - 数据源：pageServiceSessions（/channel-api/service-sessions，任务 6 新建）。
 * - 搜索：会话编码 / 服务类型 / 状态。
 * - 表格：sessionCode / serviceType(text) / serviceTitle / clientName / butlerFullName / sessionStatus(tag) / acceptTime / completeTime。
 * - 后端端点未实现时降级（空表 + 控制台 warn，不弹 toast）。
 *
 * 说明：useCrud 不返回 handleReset（已核实 composables/useCrud.ts），
 * 故在本页面内自定义 handleReset（与 agent/account 等页面一致）。
 *
 * 注意：本页文件路径为 service/session/index.vue，对齐菜单 seed 中
 * channel_client_service 的 component（'service/session/index'），
 * 而非计划任务 10 步骤 4 标题写的 client/service/index.vue。
 * 路由路径仍为 /client/service。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ServiceSession,
  ServiceSessionQuery
>(
  { page: pageServiceSessions },
  { initialQuery: { sessionCode: '', serviceType: undefined, sessionStatus: undefined } }
)

function serviceTypeText(v?: number) {
  return SERVICE_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}
function statusText(v?: number) {
  return SESSION_STATUS_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

function handleReset() {
  query.sessionCode = ''
  query.serviceType = undefined
  query.sessionStatus = undefined
  handleSearch()
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[service/session] 加载服务记录列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input v-model="query.sessionCode" placeholder="会话编码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-select v-model="query.serviceType" placeholder="服务类型" clearable style="width: 130px">
          <el-option v-for="o in SERVICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.sessionStatus" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="o in SESSION_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header><div class="card-header"><span class="card-title">服务记录列表</span></div></template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="sessionCode">
        <el-table-column prop="sessionCode" label="会话编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="serviceType" label="类型" width="90" align="center">
          <template #default="{ row }">{{ serviceTypeText(row.serviceType) }}</template>
        </el-table-column>
        <el-table-column prop="serviceTitle" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="clientName" label="客户" min-width="100" />
        <el-table-column prop="butlerFullName" label="管家" min-width="100" />
        <el-table-column prop="sessionStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.sessionStatus)">{{ statusText(row.sessionStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="acceptTime" label="受理时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.acceptTime) }}</template>
        </el-table-column>
        <el-table-column prop="completeTime" label="完成时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.completeTime) }}</template>
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
