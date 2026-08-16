<script setup lang="ts">
/**
 * 代理人详情页 - 绑定客户 tab。
 *
 * 数据模式：非标准 CRUD（无 update 端点），手动 list(by-agent) + bind(POST) + unbind(PUT /{id}/unbind)。
 * 主键为雪花 id（前端 string），unbind 路径用 id。
 *
 * 关键约束：
 * - bindTime 服务端设 now()，前端不传。
 * - 同 agentCode+clientCode 仅允许一条 status=1（后端校验）。
 * - bindType 默认 1（权益赠送绑定），3 态（1权益赠送 2活动邀请 3自主）。
 * - status 2 态（0已解绑 1服务中）。
 * - clientCode 无客户选择器文档，用 input 兜底 + TODO。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listAgentClientRelsByAgent,
  bindAgentClient,
  unbindAgentClient
} from '@/api/agent'
import { pageClients } from '@/api/client'
import {
  BIND_TYPE_OPTIONS,
  CLIENT_REL_STATUS_OPTIONS,
  bindTypeLabel,
  clientRelStatusLabel,
  clientRelStatusTagType
} from '@/types/agent'
import type { AgentClientRel } from '@/types/agent'
import type { ClientInfo } from '@/types/client'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 代理人编码（路由参数） */
  agentCode: string
}>()

// ---------- 列表（手动 by-agent list，非分页） ----------
const loading = ref(false)
const tableData = ref<AgentClientRel[]>([])

async function loadList() {
  if (!props.agentCode) return
  loading.value = true
  try {
    tableData.value = await listAgentClientRelsByAgent(props.agentCode)
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

loadList()

/** 客户下拉选项（绑定客户用） */
const clientOptions = ref<ClientInfo[]>([])
async function loadClients() {
  try {
    const res = await pageClients({ current: 1, size: 1000 })
    clientOptions.value = res.records
  } catch {
    clientOptions.value = []
  }
}
onMounted(loadClients)

// ---------- 绑定弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<{ clientCode: string; bindType: number }>({
  clientCode: '',
  bindType: 1
})

const rules: FormRules<{ clientCode: string }> = {
  clientCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }]
}

function openCreate() {
  form.clientCode = ''
  form.bindType = 1
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    await bindAgentClient({
      agentCode: props.agentCode,
      clientCode: form.clientCode,
      bindType: form.bindType
    })
    ElMessage.success('绑定成功')
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

async function handleUnbind(row: AgentClientRel) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定解绑客户「${row.clientCode}」吗？解绑后状态变为已解绑。`, '提示', {
    confirmButtonText: '确定解绑',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await unbindAgentClient(row.id)
  ElMessage.success('已解绑')
  loadList()
}

defineExpose({ loadList })
</script>

<template>
  <div class="client-rel-tab">
    <!-- 搜索/工具栏 -->
    <div class="toolbar">
      <el-tag
        v-for="o in CLIENT_REL_STATUS_OPTIONS"
        :key="o.value"
        :type="clientRelStatusTagType(o.value)"
        size="small"
      >
        {{ o.label }}
      </el-tag>
      <div class="toolbar-actions">
        <el-button :icon="'Refresh'" @click="loadList">刷新</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">绑定客户</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="clientCode" label="客户编码" min-width="160" show-overflow-tooltip />
      <el-table-column label="绑定类型" width="140" align="center">
        <template #default="{ row }">{{ bindTypeLabel(row.bindType) }}</template>
      </el-table-column>
      <el-table-column prop="bindTime" label="绑定时间" width="180" align="center">
        <template #default="{ row }">{{ formatDateTime(row.bindTime) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="clientRelStatusTagType(row.status)" size="small">
            {{ clientRelStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <!-- 仅服务中(status=1)可解绑；无 update，无独立 delete（unbind 即软删） -->
          <el-button
            v-if="row.status === 1"
            link
            type="warning"
            size="small"
            @click="handleUnbind(row)"
          >
            解绑
          </el-button>
          <span v-else class="muted">--</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 绑定客户弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="绑定客户"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="代理人编码">
          <el-input :model-value="props.agentCode" disabled />
        </el-form-item>
        <el-form-item label="客户" prop="clientCode">
          <el-select v-model="form.clientCode" placeholder="选择客户" filterable style="width: 100%">
            <el-option
              v-for="c in clientOptions"
              :key="c.clientCode"
              :label="c.fullName || c.clientCode"
              :value="c.clientCode!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定类型">
          <el-select v-model="form.bindType" style="width: 100%">
            <el-option v-for="o in BIND_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          title="绑定时间由服务端记录；同一客户与该代理人仅允许一条服务中记录（后端校验）。"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.client-rel-tab {
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

  .muted {
    color: var(--el-text-color-placeholder);
  }
}
</style>
