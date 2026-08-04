<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageOperationLogs } from '@/api/log'
import {
  type SystemOperationLog,
  type OperationLogQuery,
  type LogType,
  LOG_TYPE_OPTIONS
} from '@/types/log'

/**
 * 操作日志页（降级）。
 *
 * 后端 SystemOperationLogAdminController 暂未提供，页面在 onMounted 用 try/catch 包裹，
 * 失败时 ElMessage.warning 提示 + 显示空状态表格（不阻塞构建/路由）。
 */

const loading = ref(false)
const tableData = ref<SystemOperationLog[]>([])
const total = ref(0)
/** 后端接口是否可用（首次调用失败后置 false，UI 显示降级提示） */
const available = ref(true)

const query = reactive<OperationLogQuery>({
  logType: undefined,
  module: '',
  operatorCode: '',
  startTime: undefined,
  endTime: undefined,
  current: 1,
  size: 20
})

/** 时间范围（el-date-picker 绑定 [start, end]） */
const timeRange = ref<[string, string] | null>(null)

/** 拉取分页数据（try/catch 降级） */
async function loadData() {
  loading.value = true
  try {
    // 处理时间范围
    if (timeRange.value && timeRange.value.length === 2) {
      query.startTime = timeRange.value[0]
      query.endTime = timeRange.value[1]
    } else {
      query.startTime = undefined
      query.endTime = undefined
    }
    const res = await pageOperationLogs({ ...query })
    tableData.value = res.records
    total.value = res.total
    available.value = true
  } catch (err) {
    // 后端接口暂缺：降级为空状态 + 友好提示
    tableData.value = []
    total.value = 0
    available.value = false
    ElMessage.warning('操作日志接口待后端提供')
    void err
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
  query.logType = undefined
  query.module = ''
  query.operatorCode = ''
  timeRange.value = null
  query.startTime = undefined
  query.endTime = undefined
  query.current = 1
  loadData()
}

// ---------------- 详情弹窗 ----------------
const detailVisible = ref(false)
const detailRow = ref<SystemOperationLog | null>(null)

function openDetail(row: SystemOperationLog) {
  detailRow.value = row
  detailVisible.value = true
}

/** 状态标签 */
function statusTagType(status: string): 'success' | 'danger' {
  return status === 'success' ? 'success' : 'danger'
}

/** 日志类型标签文案 */
function logTypeLabel(t: string): string {
  const o = LOG_TYPE_OPTIONS.find((i) => i.value === t)
  return o ? o.label : t
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="log-page">
    <el-card shadow="never">
      <!-- 降级提示条 -->
      <el-alert
        v-if="!available"
        title="操作日志接口待后端提供"
        type="warning"
        description="当前后端 SystemOperationLogAdminController 暂未实现，页面展示为空状态。接口就绪后将自动加载。"
        show-icon
        :closable="false"
        class="degrade-alert"
      />

      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-select
          v-model="query.logType"
          placeholder="日志类型"
          clearable
          style="width: 140px"
        >
          <el-option
            v-for="item in LOG_TYPE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value as LogType"
          />
        </el-select>
        <el-input
          v-model="query.module"
          placeholder="模块"
          clearable
          style="width: 160px"
        />
        <el-input
          v-model="query.operatorCode"
          placeholder="操作人编码"
          clearable
          style="width: 160px"
        />
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
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>

      <!-- 列表 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        :empty-text="available ? '暂无日志数据' : '接口待后端提供'"
      >
        <el-table-column prop="operateTime" label="操作时间" min-width="160" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ logTypeLabel(row.logType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" min-width="140" show-overflow-tooltip />
        <el-table-column prop="action" label="操作" min-width="160" show-overflow-tooltip />
        <el-table-column prop="operatorName" label="操作人" min-width="120" show-overflow-tooltip />
        <el-table-column prop="operatorIp" label="IP" min-width="130" />
        <el-table-column label="耗时" width="90" align="center">
          <template #default="{ row }">{{ row.costTime }}ms</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
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
          :page-sizes="[20, 50, 100]"
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
      title="操作日志详情"
      width="720px"
      destroy-on-close
    >
      <el-descriptions v-if="detailRow" :column="2" border>
        <el-descriptions-item label="操作时间">{{ detailRow.operateTime }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ logTypeLabel(detailRow.logType) }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ detailRow.module }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ detailRow.action }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ detailRow.method }}</el-descriptions-item>
        <el-descriptions-item label="请求 URL">{{ detailRow.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detailRow.operatorName }}（{{ detailRow.operatorCode }}）</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detailRow.operatorIp }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detailRow.costTime }}ms</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detailRow.status)" size="small">
            {{ detailRow.status === 'success' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="json-box">{{ detailRow.requestParams || '—' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果" :span="2">
          <pre class="json-box">{{ detailRow.responseResult || '—' }}</pre>
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
  .degrade-alert {
    margin-bottom: 16px;
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
