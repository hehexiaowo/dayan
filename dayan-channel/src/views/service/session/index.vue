<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageServiceSessions } from '@/api/service'
import type { ServiceSession, ServiceSessionQuery } from '@/types/service'
import { SERVICE_TYPE_OPTIONS, SESSION_STATUS_OPTIONS, sessionPriorityLabel, sessionStatusTagType } from '@/types/service'
import { formatDateTime } from '@/utils/format'

/**
 * 服务记录页（只读列表）。
 *
 * - 数据源：pageServiceSessions（/channel-api/service-sessions，任务 6 新建）。
 * - 搜索：会话编码 / 服务类型 / 状态。
 * - 表格：sessionCode / serviceType(text) / serviceTitle / clientName / butlerFullName / sessionStatus(tag) / acceptTime / completeTime / 操作(详情)。
 * - 详情：el-drawer + el-descriptions 结构化抽屉展示行数据全字段（对齐 order-manage 详情模式）。
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

/** 优先级 tag 颜色：0普通/1优先=info，2紧急/3非常紧急=warning */
function priorityTagType(v?: number): 'info' | 'warning' {
  return v === 2 || v === 3 ? 'warning' : 'info'
}

function handleReset() {
  query.sessionCode = ''
  query.serviceType = undefined
  query.sessionStatus = undefined
  handleSearch()
}

// ==================== 查看详情（el-drawer + el-descriptions） ====================

const detailVisible = ref(false)
const currentRow = ref<ServiceSession | null>(null)

/** 打开详情抽屉：展示行数据全字段 */
function openDetail(row: ServiceSession) {
  currentRow.value = row
  detailVisible.value = true
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
        <el-table-column prop="serviceType" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ serviceTypeText(row.serviceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="serviceTitle" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column label="客户" min-width="130">
          <template #default="{ row }">
            <div>
              <div>{{ row.clientName || '--' }}</div>
              <div style="font-size:12px;color:#909399">{{ row.clientCode }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="butlerFullName" label="管家" min-width="100" />
        <el-table-column prop="sessionStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="sessionStatusTagType(row.sessionStatus)">{{ statusText(row.sessionStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)">{{ sessionPriorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="acceptTime" label="受理时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.acceptTime) }}</template>
        </el-table-column>
        <el-table-column prop="completeTime" label="完成时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.completeTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">查看详情</el-button>
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="服务记录详情" size="560px">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="会话编码">{{ currentRow.sessionCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="会话状态">
          <el-tag :type="sessionStatusTagType(currentRow.sessionStatus)">
            {{ statusText(currentRow.sessionStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="服务类型">{{ serviceTypeText(currentRow.serviceType) }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ currentRow.priority ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="服务标题" :span="2">{{ currentRow.serviceTitle || '--' }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ currentRow.clientName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="客户编码">{{ currentRow.clientCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="权益码">{{ currentRow.equityCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="子状态">{{ currentRow.subStatus || '--' }}</el-descriptions-item>
        <el-descriptions-item label="管家姓名">{{ currentRow.butlerFullName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="管家编码">{{ currentRow.butlerCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="机构">{{ currentRow.parkFullName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="机构编码">{{ currentRow.parkCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="代理人编码">{{ currentRow.agentCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="渠道编码">{{ currentRow.channelCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="来源类型">{{ currentRow.sourceType ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="受理时间">{{ formatDateTime(currentRow.acceptTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDateTime(currentRow.completeTime) }}</el-descriptions-item>
        <el-descriptions-item label="关闭时间">{{ formatDateTime(currentRow.closeTime) }}</el-descriptions-item>
        <el-descriptions-item label="服务总时长">{{ currentRow.totalDuration ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="触达次数">{{ currentRow.touchCount ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="是否满意">
          {{ currentRow.isSatisfied === 1 ? '是' : currentRow.isSatisfied === 0 ? '否' : '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="综合评分">{{ currentRow.overallRating ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="服务描述" :span="2">{{ currentRow.serviceDescription || '--' }}</el-descriptions-item>
        <el-descriptions-item label="关闭原因" :span="2">{{ currentRow.closeReason || '--' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remark || '--' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card { :deep(.el-card__body) { padding-bottom: 2px; } }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
