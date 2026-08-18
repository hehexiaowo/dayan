<script setup lang="ts">
/**
 * 服务会话详情页 - 回访品控 tab。
 *
 * 架构（useCrud 分页 + 弹窗 CRUD）：
 * 1. 搜索条（followupType + status）+ 新增按钮
 * 2. 主表格：useCrud（idKey:'id', fixedParams:{sessionCode}）分页加载
 * 3. 新增/编辑 el-dialog：followupCode 服务端生成，arrangeCode 可选（软关联安排）
 *
 * 红线遵守：
 * - 主键 id 雪花 Long，useCrud 传 idKey:'id'
 * - followupType / followupMethod / status 用 el-select + OPTIONS
 * - 4 维满意度（service/park/butler/overall）用 el-rate（1-5）
 * - create 时不含 isFollowupNeeded / nextFollowupDate / status（服务端自动算/固定）
 * - edit 时 status / isFollowupNeeded / isResolved 可手改
 */
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageServiceEquityFollowups,
  createServiceEquityFollowup,
  updateServiceEquityFollowup,
  deleteServiceEquityFollowup,
  listServiceEquityArranges
} from '@/api/service-sub'
import {
  FOLLOWUP_TYPE_OPTIONS,
  FOLLOWUP_METHOD_OPTIONS,
  FOLLOWUP_STATUS_OPTIONS,
  FOLLOWUP_YES_NO_OPTIONS
} from '@/types/service'
import type { ServiceEquityFollowup, ServiceEquityFollowupQuery, ServiceEquityArrange } from '@/types/service'

const props = defineProps<{
  sessionCode: string
  /** 会话客户编码（从会话详情带入，新增时回填，客户端不可改） */
  clientCode?: string
}>()

// ---------- 关联安排下拉（P1-1：从本会话安排列表选择，避免手填编码） ----------
const arrangeOptions = ref<ServiceEquityArrange[]>([])

onMounted(async () => {
  try {
    arrangeOptions.value = await listServiceEquityArranges(props.sessionCode)
  } catch {
    arrangeOptions.value = []
  }
})

// ---------- 列表（useCrud，主键 followupCode） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ServiceEquityFollowup,
  ServiceEquityFollowupQuery
>(
  {
    page: pageServiceEquityFollowups,
    create: createServiceEquityFollowup,
    update: (code, data) => updateServiceEquityFollowup(code, data),
    remove: deleteServiceEquityFollowup
  },
  {
    initialQuery: { followupType: undefined, status: undefined },
    idKey: 'followupCode',
    fixedParams: { sessionCode: props.sessionCode }
  }
)

loadPage()

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ServiceEquityFollowup>({
  id: undefined,
  sessionCode: '',
  arrangeCode: '',
  clientCode: '',
  butlerCode: '',
  followupCode: '',
  followupType: 1,
  followupMethod: 1,
  followupDate: '',
  serviceSatisfaction: 0,
  parkSatisfaction: 0,
  butlerSatisfaction: 0,
  overallSatisfaction: 0,
  serviceEvaluation: '',
  improvementSuggestions: '',
  complaints: '',
  complaintHandle: '',
  isFollowupNeeded: 0,
  followupPlan: '',
  nextFollowupDate: '',
  isResolved: 0,
  status: 2,
  remark: ''
})

const rules: FormRules<ServiceEquityFollowup> = {
  followupType: [{ required: true, message: '请选择回访类型', trigger: 'change' }],
  followupMethod: [{ required: true, message: '请选择回访方式', trigger: 'change' }],
  followupDate: [{ required: true, message: '请选择回访日期', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    sessionCode: '',
    arrangeCode: '',
    clientCode: '',
    butlerCode: '',
    followupCode: '',
    followupType: 1,
    followupMethod: 1,
    followupDate: '',
    serviceSatisfaction: 0,
    parkSatisfaction: 0,
    butlerSatisfaction: 0,
    overallSatisfaction: 0,
    serviceEvaluation: '',
    improvementSuggestions: '',
    complaints: '',
    complaintHandle: '',
    isFollowupNeeded: 0,
    followupPlan: '',
    nextFollowupDate: '',
    isResolved: 0,
    status: 2,
    remark: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.sessionCode = props.sessionCode
  // clientCode 由会话详情带入（否则新增提交空串被后端 400 拒绝）
  form.clientCode = props.clientCode ?? ''
  dialogVisible.value = true
}

function openEdit(row: ServiceEquityFollowup) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
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
    if (dialogMode.value === 'create') {
      // create 不传服务端自动字段（isFollowupNeeded / nextFollowupDate / status 固定 2）
      await createServiceEquityFollowup({
        sessionCode: form.sessionCode,
        arrangeCode: form.arrangeCode,
        clientCode: form.clientCode,
        butlerCode: form.butlerCode,
        followupType: form.followupType,
        followupMethod: form.followupMethod,
        followupDate: form.followupDate,
        serviceSatisfaction: form.serviceSatisfaction,
        parkSatisfaction: form.parkSatisfaction,
        butlerSatisfaction: form.butlerSatisfaction,
        overallSatisfaction: form.overallSatisfaction,
        serviceEvaluation: form.serviceEvaluation,
        improvementSuggestions: form.improvementSuggestions,
        complaints: form.complaints,
        complaintHandle: form.complaintHandle,
        followupPlan: form.followupPlan,
        isResolved: form.isResolved,
        remark: form.remark
      })
      ElMessage.success('新增成功')
    } else if (form.followupCode) {
      await updateServiceEquityFollowup(form.followupCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ServiceEquityFollowup) {
  if (!row.followupCode) return
  await ElMessageBox.confirm(`确定删除回访「${row.followupCode}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteServiceEquityFollowup(row.followupCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function followupTypeLabel(v?: number): string {
  const found = FOLLOWUP_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function followupMethodLabel(v?: number): string {
  const found = FOLLOWUP_METHOD_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusLabel(v?: number): string {
  const found = FOLLOWUP_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusTagType(v?: number): 'info' | 'success' | 'warning' | 'danger' {
  switch (v) {
    case 2:
      return 'success'
    case 3:
      return 'danger'
    case 1:
      return 'warning'
    case 0:
    default:
      return 'info'
  }
}

function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

defineExpose({ loadPage })
</script>

<template>
  <div class="followup-tab">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-select v-model="query.followupType" placeholder="回访类型" clearable style="width: 140px">
        <el-option v-for="o in FOLLOWUP_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option v-for="o in FOLLOWUP_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增回访</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="followupCode">
      <el-table-column prop="followupCode" label="回访编码" min-width="150" show-overflow-tooltip />
      <el-table-column prop="followupType" label="类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ followupTypeLabel(row.followupType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="followupMethod" label="方式" width="100" align="center">
        <template #default="{ row }">{{ followupMethodLabel(row.followupMethod) }}</template>
      </el-table-column>
      <el-table-column prop="followupDate" label="回访日期" width="120" align="center" />
      <el-table-column prop="overallSatisfaction" label="综合满意度" width="130" align="center">
        <template #default="{ row }">
          <el-rate :model-value="row.overallSatisfaction ?? 0" disabled size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="isFollowupNeeded" label="需跟进" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isFollowupNeeded === 1" type="warning" size="small">是</el-tag>
          <span v-else>否</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="140" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增回访' : '编辑回访'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户编码">
              <el-input v-model="form.clientCode" disabled placeholder="会话客户编码" />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="12">
            <el-form-item label="回访编码">
              <el-input v-model="form.followupCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="回访类型" prop="followupType">
              <el-select v-model="form.followupType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in FOLLOWUP_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="回访方式" prop="followupMethod">
              <el-select v-model="form.followupMethod" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in FOLLOWUP_METHOD_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="回访日期" prop="followupDate">
              <el-date-picker
                v-model="form.followupDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联安排">
              <!-- 编辑时 arrangeCode 不可改（UpdateDTO 不含 arrangeCode） -->
              <el-select
                v-model="form.arrangeCode"
                :disabled="dialogMode === 'edit'"
                placeholder="选择关联安排（本会话，可选）"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="a in arrangeOptions"
                  :key="a.arrangeCode"
                  :label="`${a.parkFullName || a.arrangeDate || a.arrangeCode}（${a.arrangeCode}）`"
                  :value="a.arrangeCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务满意度">
              <el-rate v-model="form.serviceSatisfaction" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构满意度">
              <el-rate v-model="form.parkSatisfaction" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管家满意度">
              <el-rate v-model="form.butlerSatisfaction" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="综合满意度">
              <el-rate v-model="form.overallSatisfaction" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="服务评价">
              <el-input v-model="form.serviceEvaluation" type="textarea" :rows="2" placeholder="服务评价（可选）" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="改进建议">
              <el-input v-model="form.improvementSuggestions" type="textarea" :rows="2" placeholder="改进建议（可选）" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="投诉内容">
              <el-input v-model="form.complaints" type="textarea" :rows="2" placeholder="投诉内容（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="投诉处理">
              <el-input v-model="form.complaintHandle" type="textarea" :rows="2" placeholder="投诉处理情况（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="跟进计划">
              <el-input v-model="form.followupPlan" type="textarea" :rows="2" placeholder="后续跟进计划（可选）" />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="8">
            <el-form-item label="需跟进">
              <el-select v-model="form.isFollowupNeeded" style="width: 100%">
                <el-option v-for="o in FOLLOWUP_YES_NO_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="8">
            <el-form-item label="已解决">
              <el-select v-model="form.isResolved" style="width: 100%">
                <el-option v-for="o in FOLLOWUP_YES_NO_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in FOLLOWUP_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="12">
            <el-form-item label="下次回访">
              <el-date-picker
                v-model="form.nextFollowupDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="下次回访日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
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
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
