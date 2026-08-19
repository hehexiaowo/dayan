<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageSystemLogs } from '@/api/log'
import {
  type SystemLog,
  type SystemLogQuery,
  LOG_SOURCE_OPTIONS,
  RESULT_STATUS_OPTIONS,
  resultStatusLabel,
  logSourceLabel,
  logModuleLabel,
  logActionLabel,
  logAccountTypeLabel,
  logTargetTypeLabel
} from '@/types/log'
import { formatDateTime } from '@/utils/format'

/**
 * 系统日志审计页（四端分表）。
 *
 * 展示 system_log_organ / system_log_channel / system_log_agent / system_log_client
 * 四表数据（操作日志由 @OperationLog 切面落库，登录/登出由 AuthLogRecorder 落库，
 * module=auth），支持按来源、模块、操作人编码、结果状态、时间范围筛选。
 */

const loading = ref(false)
const tableData = ref<SystemLog[]>([])
const total = ref(0)

const query = reactive<SystemLogQuery>({
  source: 'organ',
  module: '',
  accountCode: '',
  resultStatus: undefined,
  startTime: undefined,
  endTime: undefined,
  current: 1,
  size: 20
})

/** 时间范围（el-date-picker 绑定 [start, end]） */
const timeRange = ref<[string, string] | null>(null)

/** 拉取分页数据 */
async function loadData() {
  loading.value = true
  try {
    if (timeRange.value && timeRange.value.length === 2) {
      query.startTime = timeRange.value[0]
      query.endTime = timeRange.value[1]
    } else {
      query.startTime = undefined
      query.endTime = undefined
    }
    const res = await pageSystemLogs({ ...query })
    tableData.value = res.records
    total.value = res.total
  } catch {
    tableData.value = []
    total.value = 0
    ElMessage.error('加载系统日志失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  loadData()
}

function handlePageChange(page: number) {
  query.current = page
  loadData()
}

function handleSizeChange(size: number) {
  query.size = size
  query.current = 1
  loadData()
}

function handleReset() {
  query.source = 'organ'
  query.module = ''
  query.accountCode = ''
  query.resultStatus = undefined
  timeRange.value = null
  query.startTime = undefined
  query.endTime = undefined
  query.current = 1
  loadData()
}

// ---------------- 详情弹窗 ----------------
const detailVisible = ref(false)
const detailRow = ref<SystemLog | null>(null)

function openDetail(row: SystemLog) {
  detailRow.value = row
  detailVisible.value = true
}

/** 状态标签类型：成功=success / 失败=danger */
function statusTagType(status?: number): 'success' | 'danger' {
  return status === 1 ? 'success' : 'danger'
}

/** 操作人展示：优先姓名，兜底编码 */
function operatorDisplay(row: SystemLog): string {
  if (row.accountName) return row.accountName
  if (row.accountCode && row.accountCode !== 'unknown') return row.accountCode
  return row.accountCode || '—'
}

/** 尝试格式化 JSON 字符串（请求参数等），失败则原样返回 */
function prettyJson(s?: string): string {
  if (!s) return '—'
  try {
    return JSON.stringify(JSON.parse(s), null, 2)
  } catch {
    return s
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="log-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-select
          v-model="query.source"
          placeholder="日志来源"
          style="width: 130px"
          @change="handleSearch"
        >
          <el-option
            v-for="item in LOG_SOURCE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-input
          v-model="query.module"
          placeholder="模块（如 认证/渠道管理/订单管理）"
          clearable
          style="width: 180px"
        />
        <el-input
          v-model="query.accountCode"
          placeholder="操作人编码"
          clearable
          style="width: 160px"
        />
        <el-select
          v-model="query.resultStatus"
          placeholder="结果状态"
          clearable
          style="width: 120px"
        >
          <el-option
            v-for="item in RESULT_STATUS_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 360px"
        />
        <div class="toolbar-actions">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">系统日志</span>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        empty-text="暂无日志数据"
      >
        <el-table-column label="操作时间" min-width="160">
          <template #default="{ row }">
            <span class="nowrap-value">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="模块" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ logModuleLabel(row.module) }}</template>
        </el-table-column>
        <el-table-column label="动作" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ logActionLabel(row.action) }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ operatorDisplay(row) }}</template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" min-width="140" show-overflow-tooltip />
        <el-table-column label="耗时" width="110" align="center">
          <template #default="{ row }">{{ row.duration != null ? row.duration + 'ms' : '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.resultStatus)" size="small">
              {{ resultStatusLabel(row.resultStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="系统日志详情"
      width="760px"
      destroy-on-close
    >
      <el-descriptions v-if="detailRow" :column="2" border label-width="110px">
        <el-descriptions-item label="操作时间">
          <span class="nowrap-value">{{ formatDateTime(detailRow.createdAt) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detailRow.resultStatus)" size="small">
            {{ resultStatusLabel(detailRow.resultStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="来源（端口）">{{ logSourceLabel(query.source) }}</el-descriptions-item>
        <el-descriptions-item label="响应状态码">{{ detailRow.responseCode ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ logModuleLabel(detailRow.module) }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ logActionLabel(detailRow.action) }}</el-descriptions-item>
        <el-descriptions-item label="操作描述">{{ detailRow.actionDescription || '—' }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ detailRow.requestMethod || '—' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detailRow.duration != null ? detailRow.duration + 'ms' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="请求 URL" :span="2">{{ detailRow.requestUrl || '—' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ operatorDisplay(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="账号类型">{{ logAccountTypeLabel(detailRow.accountType) }}</el-descriptions-item>
        <el-descriptions-item label="操作对象">{{ logTargetTypeLabel(detailRow.targetType) }}</el-descriptions-item>
        <el-descriptions-item label="对象编码">{{ detailRow.targetCode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="对象描述" :span="2">{{ detailRow.targetDescription || '—' }}</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ detailRow.ipAddress || '—' }}</el-descriptions-item>
        <el-descriptions-item label="IP 归属地">{{ detailRow.ipLocation || '—' }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ detailRow.deviceType || '—' }}</el-descriptions-item>
        <el-descriptions-item label="操作系统">{{ detailRow.os || '—' }}</el-descriptions-item>
        <el-descriptions-item label="浏览器">{{ detailRow.browser || '—' }}</el-descriptions-item>
        <el-descriptions-item label="User-Agent" :span="2">{{ detailRow.userAgent || '—' }}</el-descriptions-item>
        <el-descriptions-item label="traceId" :span="2">{{ detailRow.traceId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2">
          <pre class="json-box">{{ detailRow.errorMsg || '—' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="json-box">{{ prettyJson(detailRow.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果" :span="2">
          <pre class="json-box">{{ prettyJson(detailRow.responseResult) }}</pre>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.log-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      font-size: 15px;
      font-weight: 600;
      color: #1f2329;
    }
  }

  .search-card {
    :deep(.el-card__body) {
      padding-bottom: 2px;
    }
  }

  // 表头标题单行展示，避免窄列被压缩后换行
  :deep(.el-table th .cell) {
    white-space: nowrap;
  }

  // 详情弹窗字段标签单行，避免长标签（响应状态码/IP 归属地等）换行
  :deep(.el-descriptions__label) {
    white-space: nowrap;
  }

  // 时间戳等短值单行展示，避免内容列被压缩时换行
  .nowrap-value {
    white-space: nowrap;
  }

  .toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;

    .toolbar-actions {
      display: flex;
      gap: 8px;
      margin-left: auto;
    }
  }

  .pager {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .json-box {
    margin: 0;
    max-height: 200px;
    overflow: auto;
    padding: 8px 12px;
    background-color: #f5f7fa;
    border-radius: 4px;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 12px;
    white-space: pre-wrap;
    word-break: break-all;
    color: #303133;
  }
}
</style>
