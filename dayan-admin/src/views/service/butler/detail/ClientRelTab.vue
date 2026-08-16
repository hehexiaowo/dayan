<script setup lang="ts">
/**
 * 管家详情页 - 服务客户 tab。
 *
 * 数据模式：非标准 CRUD（无 update 端点），手动 list + bind(POST) + unbind(PUT /{id}/unbind) + delete。
 * 主键为雪花 id（前端 string），不能用 useCrud（它假设有 update）。
 *
 * 关键约束：
 * - bindTime 服务端设 now()，前端不传。
 * - 一客户一管家（后端校验：同 clientCode status=1 仅允许一条）。
 * - bind 表单 body 仅 butlerCode + clientCode。
 * - clientCode 无客户选择器文档，用 input 兜底 + TODO。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listButlerClientRels,
  bindButlerClient,
  unbindButlerClient,
  deleteButlerClientRel
} from '@/api/service'
import {
  CLIENT_REL_STATUS_OPTIONS,
  clientRelStatusLabel,
  clientRelStatusTagType
} from '@/types/service'
import type { ButlerClientRel } from '@/types/service'
import { formatDateTime } from '@/utils/format'
import { useClientPicker } from '@/composables/useClientPicker'

const props = defineProps<{
  /** 管家编码（路由参数） */
  butlerCode: string
}>()

// ---------- 客户远程搜索选择器（pageClients 数据源） ----------
const { clientOptions, clientLoading, searchClients } = useClientPicker()

// ---------- 列表（手动 list，非分页） ----------
const loading = ref(false)
const tableData = ref<ButlerClientRel[]>([])

async function loadList() {
  if (!props.butlerCode) return
  loading.value = true
  try {
    tableData.value = await listButlerClientRels(props.butlerCode)
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

loadList()

// ---------- 绑定弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<{ clientCode: string }>({
  clientCode: ''
})

const rules: FormRules<{ clientCode: string }> = {
  clientCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }]
}

function openCreate() {
  form.clientCode = ''
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
    await bindButlerClient({ butlerCode: props.butlerCode, clientCode: form.clientCode })
    ElMessage.success('绑定成功')
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

async function handleUnbind(row: ButlerClientRel) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定解绑客户「${row.clientCode}」吗？解绑后状态变为已解绑。`, '提示', {
    confirmButtonText: '确定解绑',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await unbindButlerClient(row.id)
  ElMessage.success('已解绑')
  loadList()
}

async function handleDelete(row: ButlerClientRel) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定彻底删除与客户「${row.clientCode}」的绑定记录吗？此操作不可恢复。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteButlerClientRel(row.id)
  ElMessage.success('删除成功')
  loadList()
}

defineExpose({ loadList })
</script>

<template>
  <div class="client-rel-tab">
    <!-- 搜索/工具栏 -->
    <div class="toolbar">
      <el-tag v-for="o in CLIENT_REL_STATUS_OPTIONS" :key="o.value" :type="clientRelStatusTagType(o.value)" size="small" class="mr-8">
        {{ o.label }}
      </el-tag>
      <div class="toolbar-actions">
        <el-button :icon="'Refresh'" @click="loadList">刷新</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">绑定客户</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="clientCode" label="客户编码" min-width="160" show-overflow-tooltip />
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
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 1"
            link
            type="warning"
            size="small"
            @click="handleUnbind(row)"
          >
            解绑
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
        <el-form-item label="管家编码">
          <el-input :model-value="props.butlerCode" disabled />
        </el-form-item>
        <el-form-item label="客户" prop="clientCode">
          <el-select
            v-model="form.clientCode"
            filterable
            remote
            clearable
            :remote-method="searchClients"
            :loading="clientLoading"
            placeholder="输入客户姓名搜索"
            style="width: 100%"
          >
            <el-option
              v-for="c in clientOptions"
              :key="c.clientCode"
              :label="`${c.fullName}（${c.clientCode}）`"
              :value="c.clientCode!"
            />
          </el-select>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          title="绑定时间由服务端记录；一客户仅可绑定一位在职管家（后端校验）。"
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
  .mr-8 {
    margin-right: 8px;
  }
}
</style>
