<script setup lang="ts">
/**
 * 代理人详情页 - 分享记录 tab。
 *
 * 只增不改不删：无 update、无 delete 端点。仅 list(by-agent) + create（返回 shareCode string）。
 *
 * 关键约束：
 * - shareCode 服务端 UUID 生成（create 返回），列表只读展示。
 * - viewCount 只读（create 写0）。
 * - shareTime 服务端 now()，列表只读展示。
 * - shareType 5 态（1内容 2场景 3机构 4权益 5课程）。
 * - shareChannel 5 态（1微信 2朋友圈 3复制链接 4二维码 5短信）。
 * - bizCode/clientCode 无统一选择器，用 input 兜底 + TODO。
 */
import { reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { listAgentShareRecordsByAgent, createAgentShareRecord } from '@/api/agent'
import { listContents } from '@/api/content'
import { listScenes } from '@/api/scene'
import { listAllOrgans } from '@/api/organ'
import { listBatches } from '@/api/equity'
import { listCourses } from '@/api/course'
import { pageClients } from '@/api/client'
import {
  SHARE_TYPE_OPTIONS,
  SHARE_CHANNEL_OPTIONS,
  shareTypeLabel,
  shareChannelLabel
} from '@/types/agent'
import type { AgentShareRecord } from '@/types/agent'
import type { ClientInfo } from '@/types/client'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 代理人编码（路由参数） */
  agentCode: string
}>()

// ---------- 列表（手动 by-agent list，非分页） ----------
const loading = ref(false)
const tableData = ref<AgentShareRecord[]>([])

async function loadList() {
  if (!props.agentCode) return
  loading.value = true
  try {
    tableData.value = await listAgentShareRecordsByAgent(props.agentCode)
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

loadList()

// ---------- 新增弹窗（无编辑） ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Omit<AgentShareRecord, 'id' | 'shareCode' | 'viewCount' | 'shareTime' | 'createdAt'>>({
  agentCode: '',
  shareType: 1,
  bizCode: '',
  shareChannel: 1,
  clientCode: ''
})

const rules: FormRules<typeof form> = {
  shareType: [{ required: true, message: '请选择分享类型', trigger: 'change', type: 'number' }],
  shareChannel: [{ required: true, message: '请选择分享渠道', trigger: 'change', type: 'number' }]
}

function resetForm() {
  Object.assign(form, {
    agentCode: '',
    shareType: 1,
    bizCode: '',
    shareChannel: 1,
    clientCode: ''
  })
}

function openCreate() {
  resetForm()
  form.agentCode = props.agentCode
  dialogVisible.value = true
}

/** 业务对象下拉（按 shareType 动态切换：1内容 2场景 3机构 4权益 5课程） */
interface BizOption {
  code: string
  name: string
}
const bizOptions = ref<BizOption[]>([])
async function loadBizOptions(shareType?: number) {
  if (!shareType) {
    bizOptions.value = []
    return
  }
  try {
    let opts: BizOption[] = []
    if (shareType === 1) {
      opts = (await listContents()).map((c) => ({ code: c.contentCode!, name: c.title || c.contentCode! }))
    } else if (shareType === 2) {
      opts = (await listScenes()).map((s) => ({ code: s.sceneCode!, name: s.sceneName || s.sceneCode! }))
    } else if (shareType === 3) {
      opts = (await listAllOrgans()).map((o) => ({ code: o.organCode, name: o.fullName || o.shortName || o.organCode }))
    } else if (shareType === 4) {
      opts = (await listBatches()).map((b) => ({ code: b.batchCode!, name: b.batchName || b.batchCode! }))
    } else if (shareType === 5) {
      opts = (await listCourses()).map((c) => ({ code: c.courseCode!, name: c.courseName || c.courseCode! }))
    }
    bizOptions.value = opts
  } catch {
    bizOptions.value = []
  }
}
watch(
  () => form.shareType,
  (t) => {
    form.bizCode = ''
    loadBizOptions(t)
  }
)

/** 客户下拉 */
const clientOptions = ref<ClientInfo[]>([])
async function loadClients() {
  try {
    const res = await pageClients({ current: 1, size: 1000 })
    clientOptions.value = res.records
  } catch {
    clientOptions.value = []
  }
}
loadClients()

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    // create 返回 shareCode（服务端 UUID 生成）；viewCount 服务端写0；shareTime 服务端 now()
    const shareCode = await createAgentShareRecord(form)
    ElMessage.success(`分享成功${shareCode ? `（分享码：${shareCode}）` : ''}`)
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ loadList })
</script>

<template>
  <div class="share-tab">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button :icon="'Refresh'" @click="loadList">刷新</el-button>
      <el-button type="primary" :icon="'Plus'" @click="openCreate">新增分享记录</el-button>
      <span class="tip">分享记录只增不改不删；shareCode/浏览次数/分享时间由服务端记录。</span>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="shareCode" label="分享码" min-width="200" show-overflow-tooltip />
      <el-table-column label="分享类型" width="100" align="center">
        <template #default="{ row }">{{ shareTypeLabel(row.shareType) }}</template>
      </el-table-column>
      <el-table-column label="分享渠道" width="110" align="center">
        <template #default="{ row }">{{ shareChannelLabel(row.shareChannel) }}</template>
      </el-table-column>
      <el-table-column prop="bizCode" label="业务编码" min-width="160" show-overflow-tooltip />
      <el-table-column prop="clientCode" label="客户编码" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.clientCode || '--' }}</template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览次数" width="100" align="center" />
      <el-table-column prop="shareTime" label="分享时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.shareTime) }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <!-- 只增不改不删：无操作列 -->
    </el-table>

    <!-- 新增弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增分享记录"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="代理人编码">
              <el-input v-model="form.agentCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分享类型" prop="shareType">
              <el-select v-model="form.shareType" style="width: 100%">
                <el-option v-for="o in SHARE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分享渠道" prop="shareChannel">
              <el-select v-model="form.shareChannel" style="width: 100%">
                <el-option v-for="o in SHARE_CHANNEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务对象">
              <el-select
                v-model="form.bizCode"
                :placeholder="form.shareType ? '选择业务对象' : '请先选择分享类型'"
                filterable
                style="width: 100%"
              >
                <el-option v-for="o in bizOptions" :key="o.code" :label="o.name" :value="o.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户">
              <el-select v-model="form.clientCode" placeholder="选择客户（可选）" filterable clearable style="width: 100%">
                <el-option
                  v-for="c in clientOptions"
                  :key="c.clientCode"
                  :label="c.fullName || c.clientCode"
                  :value="c.clientCode!"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert
          type="info"
          :closable="false"
          title="shareCode 由服务端生成；浏览次数初始为 0；分享时间由服务端记录。"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.share-tab {
  .toolbar {
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 8px;
    .tip {
      color: var(--el-text-color-secondary);
      font-size: 13px;
      margin-left: 8px;
    }
  }
}
</style>
