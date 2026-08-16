<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageMessageRecords, getMessageRecordDetail } from '@/api/message'
import {
  type MessageRecord,
  CHANNEL_TYPE_OPTIONS,
  BIZ_TYPE_OPTIONS,
  SEND_STATUS_OPTIONS,
  TARGET_TYPE_OPTIONS,
  channelTypeLabel,
  messageTypeLabel,
  sendStatusLabel,
  sendStatusTagType,
  targetTypeLabel
} from '@/types/message'
import { formatDateTime } from '@/utils/format'

/**
 * 消息发送记录页（system_message，只读审计）。
 *
 * 记录由业务发送链路落库（短信/站内信/推送/邮件统一收口），
 * 本页仅查询排障：按业务类型/渠道/发送状态/接收者筛选，详情看
 * 渲染后正文、服务商回执与失败原因。
 */

const loading = ref(false)
const tableData = ref<MessageRecord[]>([])
const total = ref(0)

const query = reactive({
  bizType: '',
  channelType: undefined as number | undefined,
  sendStatus: undefined as number | undefined,
  targetType: '',
  templateCode: '',
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
  current: 1,
  size: 20
})

/** 时间范围（el-date-picker 绑定 [start, end]） */
const timeRange = ref<[string, string] | null>(null)

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
    const res = await pageMessageRecords({ ...query })
    tableData.value = res.records
    total.value = res.total
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  loadData()
}

function handleReset() {
  query.bizType = ''
  query.channelType = undefined
  query.sendStatus = undefined
  query.targetType = ''
  query.templateCode = ''
  timeRange.value = null
  query.startTime = undefined
  query.endTime = undefined
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

// ---------------- 详情弹窗 ----------------
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRow = ref<MessageRecord | null>(null)

async function openDetail(row: MessageRecord) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailRow.value = await getMessageRecordDetail(row.id!)
  } catch (err) {
    detailRow.value = row
    ElMessage.error('加载记录详情失败，已展示列表数据')
    void err
  } finally {
    detailLoading.value = false
  }
}

/** 跳转链接类型文案：1=内部页面 2=外部链接 3=APP路由 */
function linkTypeLabel(v?: number | null): string {
  if (v === 1) return '内部页面'
  if (v === 2) return '外部链接'
  if (v === 3) return 'APP路由'
  return '—'
}

onMounted(loadData)
</script>

<template>
  <div class="msg-record-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-select v-model="query.bizType" placeholder="业务类型" clearable filterable style="width: 140px">
          <el-option v-for="o in BIZ_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.channelType" placeholder="渠道" clearable style="width: 120px">
          <el-option v-for="o in CHANNEL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.sendStatus" placeholder="发送状态" clearable style="width: 120px">
          <el-option v-for="o in SEND_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.targetType" placeholder="接收者类型" clearable style="width: 120px">
          <el-option v-for="o in TARGET_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input
          v-model="query.templateCode"
          placeholder="模板编码"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 340px"
        />
        <div class="toolbar-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">消息记录</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="messageCode" label="消息编码" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.messageCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bizType" label="业务类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.bizType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="channelType" label="渠道" width="115">
          <template #default="{ row }">
            <el-tag size="small" :type="row.channelType === 1 ? 'success' : row.channelType === 2 ? 'primary' : 'warning'">
              {{ channelTypeLabel(row.channelType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="messageType" label="消息类型" width="100">
          <template #default="{ row }">{{ messageTypeLabel(row.messageType) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.title || '—' }}</template>
        </el-table-column>
        <el-table-column prop="content" label="正文" min-width="220" show-overflow-tooltip />
        <el-table-column label="接收者" min-width="150" show-overflow>
          <template #default="{ row }">
            {{ targetTypeLabel(row.targetType) }}<template v-if="row.targetName"> · {{ row.targetName }}</template>
            <template v-else-if="row.targetCode"> · {{ row.targetCode }}</template>
          </template>
        </el-table-column>
        <el-table-column prop="sendStatus" label="发送状态" width="110" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.sendStatus === 3 && row.errorMsg" :content="`${row.errorCode || ''} ${row.errorMsg}`" placement="top">
              <el-tag :type="sendStatusTagType(row.sendStatus)" size="small">
                {{ sendStatusLabel(row.sendStatus) }}
              </el-tag>
            </el-tooltip>
            <el-tag v-else :type="sendStatusTagType(row.sendStatus)" size="small">
              {{ sendStatusLabel(row.sendStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sendTime" label="发送时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.sendTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:msg-record:query'" link type="primary" size="small" @click="openDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          :current-page="query.current"
          :page-size="query.size"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="发送记录详情" width="720px">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detailRow" :column="2" border>
          <el-descriptions-item label="消息编码">
            <span class="mono">{{ detailRow.messageCode }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="发送批次">{{ detailRow.batchCode || '—' }}</el-descriptions-item>
          <el-descriptions-item label="模板编码">
            <span class="mono">{{ detailRow.templateCode || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ detailRow.bizType }}</el-descriptions-item>
          <el-descriptions-item label="发送渠道">{{ channelTypeLabel(detailRow.channelType) }}</el-descriptions-item>
          <el-descriptions-item label="消息类型">{{ messageTypeLabel(detailRow.messageType) }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            {{ detailRow.priority === 2 ? '紧急' : detailRow.priority === 1 ? '重要' : '普通' }}
          </el-descriptions-item>
          <el-descriptions-item label="发送状态">
            <el-tag :type="sendStatusTagType(detailRow.sendStatus)" size="small">
              {{ sendStatusLabel(detailRow.sendStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="接收者">
            {{ targetTypeLabel(detailRow.targetType) }}
            {{ detailRow.targetName || detailRow.targetCode || '' }}
          </el-descriptions-item>
          <el-descriptions-item label="联系方式">{{ detailRow.targetContact || '—' }}</el-descriptions-item>
          <el-descriptions-item label="发送者">
            {{ detailRow.senderType }}<template v-if="detailRow.senderCode"> · {{ detailRow.senderCode }}</template>
          </el-descriptions-item>
          <el-descriptions-item label="服务商消息ID">{{ detailRow.providerMsgId || '—' }}</el-descriptions-item>
          <el-descriptions-item label="发送时间">{{ formatDateTime(detailRow.sendTime) }}</el-descriptions-item>
          <el-descriptions-item label="送达时间">{{ formatDateTime(detailRow.deliverTime) }}</el-descriptions-item>
          <el-descriptions-item label="阅读时间">{{ formatDateTime(detailRow.readTime) }}</el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ formatDateTime(detailRow.expireTime) }}</el-descriptions-item>
          <el-descriptions-item label="重试次数">{{ detailRow.retryCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="跳转链接">{{ detailRow.linkUrl || '—' }}</el-descriptions-item>
          <el-descriptions-item label="链接类型">{{ linkTypeLabel(detailRow.linkType) }}</el-descriptions-item>
          <el-descriptions-item label="消息标题" :span="2">{{ detailRow.title || '—' }}</el-descriptions-item>
          <el-descriptions-item label="消息正文（渲染后）" :span="2">
            <div class="content-box">{{ detailRow.content }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailRow.errorCode || detailRow.errorMsg" label="失败原因" :span="2">
            <span class="error-text">{{ detailRow.errorCode }} {{ detailRow.errorMsg }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.msg-record-page {
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

  .mono {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 12px;
  }

  .content-box {
    white-space: pre-wrap;
    word-break: break-all;
    max-height: 200px;
    overflow: auto;
  }

  .error-text {
    color: #f56c6c;
  }
}
</style>
