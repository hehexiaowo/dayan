<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageSessions,
  updateSession,
  deleteSession,
  transitionSession,
  assignButler,
  listButlers
} from '@/api/service'
import type { ButlerInfo, ServiceSession, ServiceSessionQuery } from '@/types/service'
import {
  SessionStatus,
  SESSION_STATUS_OPTIONS,
  SERVICE_TYPE_OPTIONS
} from '@/types/service'

/**
 * 服务会话管理页（第一版简化）。
 *
 * - 列表 + 详情 + 编辑（普通字段）+ 删除。
 * - 状态机动作统一走 POST /service/session/transition（@RequestBody TransitionDTO），
 *   assign-butler / submit-demand / confirm-solution / reject-solution /
 *   start-service / finish / cancel 均通过 transitionSession(sessionCode, event) 触发。
 * - 操作列按钮按 sessionStatus 动态显示当前可执行的事件。
 *
 * 会话状态（service_session.session_status）：
 *   0待受理 / 1已受理 / 2需求提交 / 3方案确认 / 4服务中 / 5已完成 / 6已取消。
 *
 * 状态机事件（ServiceSessionEvent）：
 *   assign_butler(0→1) / submit_demand(1→2) / confirm_solution(2→3) /
 *   reject_solution(3→2) / start_service(3→4) / finish(4→5) / cancel(0|1|4→6)。
 */

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<ServiceSession, ServiceSessionQuery>(
  { page: pageSessions },
  {
    initialQuery: {
      sessionCode: '',
      equityCode: '',
      clientCode: '',
      butlerCode: '',
      serviceType: undefined,
      sessionStatus: undefined
    }
  }
)

const router = useRouter()

function goDetail(row: ServiceSession) {
  if (!row.sessionCode) return
  router.push({ name: 'SessionDetail', params: { sessionCode: row.sessionCode } })
}

function handleReset() {
  query.sessionCode = ''
  query.equityCode = ''
  query.clientCode = ''
  query.butlerCode = ''
  query.serviceType = undefined
  query.sessionStatus = undefined
  handleSearch()
}

// ---------- 编辑弹窗（普通字段） ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ServiceSession>({
  sessionCode: undefined,
  serviceTitle: '',
  serviceDescription: '',
  priority: undefined,
  remark: ''
})

const rules: FormRules<ServiceSession> = {
  serviceTitle: [{ required: true, message: '请输入服务标题', trigger: 'blur' }]
}

function openEdit(row: ServiceSession) {
  if (!row.sessionCode) return
  form.sessionCode = row.sessionCode
  form.serviceTitle = row.serviceTitle ?? ''
  form.serviceDescription = row.serviceDescription ?? ''
  form.priority = row.priority
  form.remark = row.remark ?? ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value || !form.sessionCode) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    await updateSession(form.sessionCode, {
      serviceTitle: form.serviceTitle,
      serviceDescription: form.serviceDescription,
      priority: form.priority,
      remark: form.remark
    })
    ElMessage.success('修改成功')
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: ServiceSession) {
  if (!row.sessionCode) return
  await ElMessageBox.confirm(`确定删除会话「${row.sessionCode}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteSession(row.sessionCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 状态机流转（transition 通用方法） ----------
const actionLoading = ref(false)

/**
 * 触发状态机流转。
 *
 * event 取值：assign_butler / submit_demand / confirm_solution / reject_solution /
 * start_service / finish / cancel（对齐后端 ServiceSessionEvent）。
 */
async function handleTransition(
  row: ServiceSession,
  event: string,
  actionLabel: string,
  confirmText?: string
) {
  if (!row.sessionCode) return
  if (confirmText) {
    await ElMessageBox.confirm(confirmText, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  }
  actionLoading.value = true
  try {
    await transitionSession(row.sessionCode, event)
    ElMessage.success(`${actionLabel}成功`)
    loadPage()
  } finally {
    actionLoading.value = false
  }
}

// ---------- 分配管家（下拉选择弹窗） ----------
/**
 * 改造：原先用 prompt 手输 butlerCode，体验差且易错。
 * 现改为独立弹窗 + el-select（数据源 listButlers，仅展示在职管家）。
 */
const butlerOptions = ref<ButlerInfo[]>([])
const assignDialogVisible = ref(false)
const assignTargetCode = ref('')
const assignButlerCode = ref('')

async function loadButlerOptions() {
  try {
    // status=1 仅在职管家
    butlerOptions.value = await listButlers({ status: 1 })
  } catch {
    butlerOptions.value = []
  }
}

onMounted(loadButlerOptions)

function handleAssignButler(row: ServiceSession) {
  if (!row.sessionCode) return
  assignTargetCode.value = row.sessionCode
  assignButlerCode.value = row.butlerCode ?? ''
  assignDialogVisible.value = true
}

async function handleAssignSubmit() {
  if (!assignButlerCode.value) {
    ElMessage.warning('请选择管家')
    return
  }
  actionLoading.value = true
  try {
    await assignButler(assignTargetCode.value, assignButlerCode.value)
    const found = butlerOptions.value.find((b) => b.butlerCode === assignButlerCode.value)
    ElMessage.success(`已分配管家：${found?.fullName ?? assignButlerCode.value}`)
    assignDialogVisible.value = false
    loadPage()
  } finally {
    actionLoading.value = false
  }
}

/** 取消会话（cancel: 0|1|4→6）：收集关闭原因。 */
async function handleCancelSession(row: ServiceSession) {
  if (!row.sessionCode) return
  let closeReason = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因（可选）', '取消会话', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '关闭原因',
      inputType: 'textarea'
    })
    closeReason = value?.trim() ?? ''
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await transitionSession(row.sessionCode, 'cancel')
    // closeReason 通过 update 回填（cancel 事件后端写 closeReason/closeTime）
    if (closeReason) {
      try {
        await updateSession(row.sessionCode, { closeReason })
      } catch {
        // 忽略 closeReason 回填失败
      }
    }
    ElMessage.success('已取消会话')
    loadPage()
  } finally {
    actionLoading.value = false
  }
}

// ---------- 辅助渲染 ----------
function serviceTypeLabel(t?: number): string {
  const found = SERVICE_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function sessionStatusLabel(s?: number): string {
  const found = SESSION_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/** 会话状态 el-tag type：0待受理info / 1已受理warning / 2需求primary / 3方案primary / 4服务warning / 5完成success / 6取消info。 */
function sessionStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (status) {
    case SessionStatus.COMPLETED:
      return 'success'
    case SessionStatus.ACCEPTED:
    case SessionStatus.IN_SERVICE:
      return 'warning'
    case SessionStatus.DEMAND_SUBMITTED:
    case SessionStatus.SOLUTION_CONFIRMED:
      return 'primary'
    case SessionStatus.CANCELLED:
    default:
      return 'info'
  }
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="会话编码">
          <el-input v-model="query.sessionCode" placeholder="会话编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="权益编码">
          <el-input v-model="query.equityCode" placeholder="权益编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="客户编码">
          <el-input v-model="query.clientCode" placeholder="客户编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="管家编码">
          <el-input v-model="query.butlerCode" placeholder="管家编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="服务类型">
          <el-select v-model="query.serviceType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in SERVICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="会话状态">
          <el-select v-model="query.sessionStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="o in SESSION_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>服务会话列表</span>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        row-key="sessionCode"
      >
        <el-table-column prop="sessionCode" label="会话编码" min-width="150" show-overflow-tooltip />
        <el-table-column prop="serviceType" label="服务类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ serviceTypeLabel(row.serviceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="serviceTitle" label="服务标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="clientCode" label="客户" min-width="120" show-overflow-tooltip />
        <el-table-column prop="butlerFullName" label="管家" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.butlerFullName || row.butlerCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center" />
        <el-table-column prop="sessionStatus" label="会话状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="sessionStatusTagType(row.sessionStatus)">{{ sessionStatusLabel(row.sessionStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subStatus" label="子状态" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.subStatus || '--' }}</template>
        </el-table-column>
        <el-table-column prop="acceptTime" label="受理时间" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <!-- 按状态动态展示流转按钮（统一走 transition 端点） -->
            <el-button
              v-if="row.sessionStatus === SessionStatus.PENDING"
              link
              type="success"
              size="small"
              :disabled="actionLoading"
              @click="handleAssignButler(row)"
            >
              分配管家
            </el-button>
            <el-button
              v-if="row.sessionStatus === SessionStatus.ACCEPTED"
              link
              type="primary"
              size="small"
              :disabled="actionLoading"
              @click="handleTransition(row, 'submit_demand', '提交需求', '确定提交需求吗？')"
            >
              提交需求
            </el-button>
            <el-button
              v-if="row.sessionStatus === SessionStatus.DEMAND_SUBMITTED"
              link
              type="success"
              size="small"
              :disabled="actionLoading"
              @click="handleTransition(row, 'confirm_solution', '确认方案', '确定确认方案吗？')"
            >
              确认方案
            </el-button>
            <el-button
              v-if="row.sessionStatus === SessionStatus.SOLUTION_CONFIRMED"
              link
              type="primary"
              size="small"
              :disabled="actionLoading"
              @click="handleTransition(row, 'start_service', '开始服务', '确定开始服务吗？')"
            >
              开始服务
            </el-button>
            <el-button
              v-if="row.sessionStatus === SessionStatus.IN_SERVICE"
              link
              type="success"
              size="small"
              :disabled="actionLoading"
              @click="handleTransition(row, 'finish', '完成服务', '确定完成服务吗？')"
            >
              完成服务
            </el-button>
            <el-button
              v-if="
                row.sessionStatus === SessionStatus.PENDING ||
                row.sessionStatus === SessionStatus.ACCEPTED ||
                row.sessionStatus === SessionStatus.IN_SERVICE
              "
              link
              type="danger"
              size="small"
              :disabled="actionLoading"
              @click="handleCancelSession(row)"
            >
              取消
            </el-button>
            <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
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

    <!-- 编辑弹窗（普通字段） -->
    <el-dialog v-model="dialogVisible" title="编辑会话" width="620px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="会话编码">
          <el-input v-model="form.sessionCode" disabled />
        </el-form-item>
        <el-form-item label="服务标题" prop="serviceTitle">
          <el-input v-model="form.serviceTitle" placeholder="服务标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="服务描述">
          <el-input v-model="form.serviceDescription" type="textarea" :rows="3" placeholder="服务描述" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="内部备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配管家弹窗（下拉选择） -->
    <el-dialog v-model="assignDialogVisible" title="分配管家" width="460px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="会话编码">
          <el-input :model-value="assignTargetCode" disabled />
        </el-form-item>
        <el-form-item label="指派管家">
          <el-select
            v-model="assignButlerCode"
            placeholder="请选择管家"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="b in butlerOptions"
              :key="b.butlerCode"
              :label="`${b.fullName}（${b.butlerCode}）`"
              :value="b.butlerCode!"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleAssignSubmit">确定</el-button>
      </template>
    </el-dialog>
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
