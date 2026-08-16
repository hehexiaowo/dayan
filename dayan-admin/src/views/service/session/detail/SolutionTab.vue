<script setup lang="ts">
/**
 * 服务会话详情页 - 权益方案 tab。
 *
 * 架构（useCrud 分页 + 弹窗 CRUD + accept 标记）：
 * 1. 搜索条（solutionType + isAccepted + status）+ 新增按钮
 * 2. 主表格：useCrud（idKey:'id', fixedParams:{sessionCode}）分页加载
 * 3. 新增/编辑 el-dialog：solutionCode 服务端生成，demandCode 必填（关联需求）
 * 4. 操作列额外「接受标记」按钮：调 acceptServiceEquitySolution(id, isAccepted, clientFeedback?)，
 *    用于业务链：会话 confirm_solution 前须存在 isAccepted=1 的方案。
 *
 * 红线遵守：
 * - 主键 id 雪花 Long，useCrud 传 idKey:'id'
 * - solutionType / presentationMethod / isAccepted / status 用 el-select + OPTIONS
 * - estimatedCost 才用 el-input-number（金额，precision=2）
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageServiceEquitySolutions,
  createServiceEquitySolution,
  updateServiceEquitySolution,
  deleteServiceEquitySolution,
  acceptServiceEquitySolution
} from '@/api/service-sub'
import {
  SOLUTION_TYPE_OPTIONS,
  PRESENTATION_METHOD_OPTIONS,
  SOLUTION_IS_ACCEPTED_OPTIONS,
  SOLUTION_STATUS_OPTIONS
} from '@/types/service'
import type { ServiceEquitySolution, ServiceEquitySolutionQuery } from '@/types/service'

const props = defineProps<{
  sessionCode: string
  /** 会话客户编码（从会话详情带入，新增时回填，客户端不可改） */
  clientCode?: string
}>()

// ---------- 列表（useCrud，主键 solutionCode） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ServiceEquitySolution,
  ServiceEquitySolutionQuery
>(
  {
    page: pageServiceEquitySolutions,
    create: createServiceEquitySolution,
    update: (code, data) => updateServiceEquitySolution(code, data),
    remove: deleteServiceEquitySolution
  },
  {
    initialQuery: { solutionType: undefined, isAccepted: undefined, status: undefined },
    idKey: 'solutionCode',
    fixedParams: { sessionCode: props.sessionCode }
  }
)

loadPage()

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ServiceEquitySolution>({
  id: undefined,
  sessionCode: '',
  demandCode: '',
  clientCode: '',
  butlerCode: '',
  solutionCode: '',
  solutionName: '',
  solutionType: 1,
  recommendedParks: '',
  planSummary: '',
  serviceItems: '',
  estimatedCost: undefined,
  costBreakdown: '',
  timeline: '',
  advantages: '',
  risks: '',
  comparison: '',
  presentationMethod: 1,
  clientFeedback: '',
  isAccepted: 0,
  status: 0,
  remark: ''
})

const rules: FormRules<ServiceEquitySolution> = {
  demandCode: [{ required: true, message: '请输入关联需求编码', trigger: 'blur' }],
  solutionName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  solutionType: [{ required: true, message: '请选择方案类型', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    sessionCode: '',
    demandCode: '',
    clientCode: '',
    butlerCode: '',
    solutionCode: '',
    solutionName: '',
    solutionType: 1,
    recommendedParks: '',
    planSummary: '',
    serviceItems: '',
    estimatedCost: undefined,
    costBreakdown: '',
    timeline: '',
    advantages: '',
    risks: '',
    comparison: '',
    presentationMethod: 1,
    clientFeedback: '',
    isAccepted: 0,
    status: 0,
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

function openEdit(row: ServiceEquitySolution) {
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
      await createServiceEquitySolution(form)
      ElMessage.success('新增成功')
    } else if (form.solutionCode) {
      await updateServiceEquitySolution(form.solutionCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ServiceEquitySolution) {
  if (!row.solutionCode) return
  await ElMessageBox.confirm(`确定删除方案「${row.solutionCode}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteServiceEquitySolution(row.solutionCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 接受标记（业务链端点，单弹窗 radio + 反馈） ----------
const acceptLoading = ref(false)
const acceptDialogVisible = ref(false)
const acceptTargetCode = ref('')
const acceptForm = reactive({
  isAccepted: 1,
  clientFeedback: ''
})

function handleAccept(row: ServiceEquitySolution) {
  if (!row.solutionCode) return
  acceptTargetCode.value = row.solutionCode
  acceptForm.isAccepted = row.isAccepted ?? 1
  acceptForm.clientFeedback = row.clientFeedback ?? ''
  acceptDialogVisible.value = true
}

async function handleAcceptSubmit() {
  acceptLoading.value = true
  try {
    await acceptServiceEquitySolution(
      acceptTargetCode.value,
      acceptForm.isAccepted,
      acceptForm.clientFeedback.trim() || undefined
    )
    ElMessage.success('接受标记已更新')
    acceptDialogVisible.value = false
    loadPage()
  } finally {
    acceptLoading.value = false
  }
}

// ---------- 辅助渲染 ----------
function solutionTypeLabel(v?: number): string {
  const found = SOLUTION_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function presentationMethodLabel(v?: number): string {
  const found = PRESENTATION_METHOD_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function isAcceptedLabel(v?: number): string {
  const found = SOLUTION_IS_ACCEPTED_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function isAcceptedTagType(v?: number): 'danger' | 'success' | 'warning' {
  if (v === 1) return 'success'
  if (v === 2) return 'warning'
  return 'danger'
}

function statusLabel(v?: number): string {
  const found = SOLUTION_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusTagType(v?: number): 'info' | 'warning' | 'success' | 'danger' | 'primary' {
  switch (v) {
    case 3:
      return 'success'
    case 4:
      return 'danger'
    case 2:
      return 'primary'
    case 1:
    case 5:
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
  <div class="solution-tab">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-select v-model="query.solutionType" placeholder="方案类型" clearable style="width: 120px">
        <el-option v-for="o in SOLUTION_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="query.isAccepted" placeholder="接受状态" clearable style="width: 120px">
        <el-option v-for="o in SOLUTION_IS_ACCEPTED_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="方案状态" clearable style="width: 120px">
        <el-option v-for="o in SOLUTION_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增方案</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="solutionCode">
      <el-table-column prop="solutionCode" label="方案编码" min-width="150" show-overflow-tooltip />
      <el-table-column prop="solutionName" label="方案名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="solutionType" label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.solutionType === 1 ? 'primary' : 'info'">
            {{ solutionTypeLabel(row.solutionType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="demandCode" label="关联需求" min-width="140" show-overflow-tooltip />
      <el-table-column prop="estimatedCost" label="预估费用" width="120" align="right">
        <template #default="{ row }">
          {{ row.estimatedCost != null ? Number(row.estimatedCost).toFixed(2) : '--' }}
        </template>
      </el-table-column>
      <el-table-column prop="presentationMethod" label="呈现方式" width="110" align="center">
        <template #default="{ row }">{{ presentationMethodLabel(row.presentationMethod) }}</template>
      </el-table-column>
      <el-table-column prop="isAccepted" label="客户接受" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="isAcceptedTagType(row.isAccepted)" size="small">{{ isAcceptedLabel(row.isAccepted) }}</el-tag>
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button
            link
            type="success"
            size="small"
            :disabled="acceptLoading"
            @click="handleAccept(row)"
          >
            接受标记
          </el-button>
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
      :title="dialogMode === 'create' ? '新增方案' : '编辑方案'"
      width="860px"
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
            <el-form-item label="方案编码">
              <el-input v-model="form.solutionCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="方案名称" prop="solutionName">
              <el-input v-model="form.solutionName" placeholder="方案名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联需求" prop="demandCode">
              <!-- 编辑时 demandCode 不可改（UpdateDTO 不含 demandCode） -->
              <el-input
                v-model="form.demandCode"
                :disabled="dialogMode === 'edit'"
                placeholder="需求编码（必填，关联本会话需求）"
                maxlength="50"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="方案类型" prop="solutionType">
              <el-select v-model="form.solutionType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in SOLUTION_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预估费用">
              <el-input-number v-model="form.estimatedCost" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="呈现方式">
              <el-select v-model="form.presentationMethod" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in PRESENTATION_METHOD_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="推荐机构">
              <el-input v-model="form.recommendedParks" placeholder="推荐机构列表 JSON 数组（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="方案概要">
              <el-input v-model="form.planSummary" type="textarea" :rows="3" placeholder="方案概要说明" maxlength="1000" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="服务项目">
              <el-input v-model="form.serviceItems" type="textarea" :rows="3" placeholder="服务项目明细 JSON 数组（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="费用明细">
              <el-input v-model="form.costBreakdown" type="textarea" :rows="3" placeholder="费用明细 JSON（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="时间安排">
              <el-input v-model="form.timeline" type="textarea" :rows="2" placeholder="时间安排说明（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="方案优势">
              <el-input v-model="form.advantages" type="textarea" :rows="2" placeholder="方案优势（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="风险提示">
              <el-input v-model="form.risks" type="textarea" :rows="2" placeholder="风险提示（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="方案对比">
              <el-input v-model="form.comparison" type="textarea" :rows="2" placeholder="与其他方案对比（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="客户反馈">
              <el-input v-model="form.clientFeedback" type="textarea" :rows="2" placeholder="客户反馈（可选）" />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="12">
            <el-form-item label="接受标记">
              <el-select v-model="form.isAccepted" style="width: 100%">
                <el-option v-for="o in SOLUTION_IS_ACCEPTED_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in SOLUTION_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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

    <!-- 接受标记弹窗（radio + 反馈） -->
    <el-dialog
      v-model="acceptDialogVisible"
      title="方案接受标记"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form label-width="90px">
        <el-form-item label="方案编码">
          <el-input :model-value="acceptTargetCode" disabled />
        </el-form-item>
        <el-form-item label="接受标记">
          <el-radio-group v-model="acceptForm.isAccepted">
            <el-radio
              v-for="o in SOLUTION_IS_ACCEPTED_OPTIONS"
              :key="o.value"
              :value="o.value"
            >
              {{ o.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="客户反馈">
          <el-input
            v-model="acceptForm.clientFeedback"
            type="textarea"
            :rows="3"
            placeholder="客户反馈意见（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="acceptLoading" @click="handleAcceptSubmit">确定</el-button>
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
