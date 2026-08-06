<script setup lang="ts">
/**
 * 供应商详情页 - 评价 tab。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{supplierCode}）。
 * 搜索：evalPeriod + evalType + scoreLevel + status。
 *
 * 红线：
 * - 主键 Long id；supplierCode 从 prop 带入 create 表单隐藏。
 * - totalScore（综合分）/ scoreLevel（等级 A/B/C/D）由后端按三科评分和投诉率自动计算
 *   （为空时算），**前端表单不含这两字段**，列表只读展示。
 * - status create 默认 1（已提交）。
 * - 三科评分与投诉率均为 0-100 number。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageEvaluations,
  createEvaluation,
  updateEvaluation,
  deleteEvaluation
} from '@/api/supplier-evaluation'
import {
  EVALUATION_TYPE_OPTIONS,
  SCORE_LEVEL_OPTIONS
} from '@/types/supplier'
import type { SupplierEvaluation, SupplierEvaluationQuery } from '@/types/supplier'

const props = defineProps<{
  supplierCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  SupplierEvaluation,
  SupplierEvaluationQuery,
  number
>(
  {
    page: pageEvaluations,
    create: createEvaluation,
    update: (id, data) => updateEvaluation(id, data),
    remove: deleteEvaluation
  },
  {
    initialQuery: { evalPeriod: '', evalType: undefined, scoreLevel: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { supplierCode: props.supplierCode }
  }
)

loadPage()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SupplierEvaluation>({
  id: undefined,
  supplierCode: '',
  evalPeriod: '',
  evalType: undefined,
  serviceQualityScore: undefined,
  facilityQualityScore: undefined,
  cooperationScore: undefined,
  complaintRate: undefined,
  totalOrderCount: undefined,
  complaintCount: undefined,
  evalContent: '',
  improvementSuggestions: '',
  evaluatorCode: '',
  evaluatorName: '',
  evalDate: '',
  status: 1,
  remark: ''
})

const rules: FormRules<SupplierEvaluation> = {
  evalPeriod: [{ required: true, message: '请输入评价周期，如 2026Q3', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    supplierCode: '',
    evalPeriod: '',
    evalType: undefined,
    serviceQualityScore: undefined,
    facilityQualityScore: undefined,
    cooperationScore: undefined,
    complaintRate: undefined,
    totalOrderCount: undefined,
    complaintCount: undefined,
    evalContent: '',
    improvementSuggestions: '',
    evaluatorCode: '',
    evaluatorName: '',
    evalDate: '',
    status: 1,
    remark: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.supplierCode = props.supplierCode
  dialogVisible.value = true
}

function openEdit(row: SupplierEvaluation) {
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
      await createEvaluation(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateEvaluation(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: SupplierEvaluation) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该评价记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteEvaluation(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function evalTypeLabel(v?: number): string {
  const found = EVALUATION_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}
function scoreLevelLabel(v?: number): string {
  const found = SCORE_LEVEL_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}
function scoreLevelTagType(v?: number): 'success' | 'warning' | 'info' | 'danger' {
  if (v === 1) return 'success' // A
  if (v === 2) return 'success' // B
  if (v === 3) return 'warning' // C
  if (v === 4) return 'danger' // D
  return 'info'
}
function statusLabel(s?: number): string {
  return s === 1 ? '已提交' : '草稿'
}
function statusTagType(s?: number): 'success' | 'info' {
  return s === 1 ? 'success' : 'info'
}
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}
defineExpose({ loadPage })
</script>

<template>
  <div class="evaluation-tab">
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="评价周期">
        <el-input v-model="query.evalPeriod" placeholder="如 2026Q3" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="评价类型">
        <el-select v-model="query.evalType" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in EVALUATION_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="等级">
        <el-select v-model="query.scoreLevel" placeholder="全部" clearable style="width: 100px">
          <el-option v-for="o in SCORE_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="已提交" :value="1" />
          <el-option label="草稿" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增评价</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="evalPeriod" label="评价周期" width="100" align="center" />
      <el-table-column prop="evalType" label="类型" width="120" align="center">
        <template #default="{ row }">{{ evalTypeLabel(row.evalType) }}</template>
      </el-table-column>
      <el-table-column prop="serviceQualityScore" label="服务质量" width="100" align="center" />
      <el-table-column prop="facilityQualityScore" label="设施质量" width="100" align="center" />
      <el-table-column prop="cooperationScore" label="配合度" width="90" align="center" />
      <el-table-column prop="totalScore" label="综合分" width="100" align="center">
        <template #default="{ row }">
          <span class="score-total">{{ row.totalScore ?? '--' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="scoreLevel" label="等级" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.scoreLevel" :type="scoreLevelTagType(row.scoreLevel)" size="small">
            {{ scoreLevelLabel(row.scoreLevel) }}
          </el-tag>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="120" align="center">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增评价' : '编辑评价'"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="form-tip"
          title="综合分（totalScore）与等级（scoreLevel）由后端根据三科评分和投诉率自动计算，前端无需填写。"
        />
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="评价周期" prop="evalPeriod">
              <el-input v-model="form.evalPeriod" placeholder="如 2026Q3" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评价类型">
              <el-select v-model="form.evalType" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in EVALUATION_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="服务质量分">
              <el-input-number v-model="form.serviceQualityScore" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="设施质量分">
              <el-input-number v-model="form.facilityQualityScore" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="配合度分">
              <el-input-number v-model="form.cooperationScore" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="投诉率(%)">
              <el-input-number v-model="form.complaintRate" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总订单数">
              <el-input-number v-model="form.totalOrderCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="投诉数">
              <el-input-number v-model="form.complaintCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="评价内容">
              <el-input v-model="form.evalContent" type="textarea" :rows="2" placeholder="评价内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="改进建议">
              <el-input v-model="form.improvementSuggestions" type="textarea" :rows="2" placeholder="改进建议" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="评价人工号">
              <el-input v-model="form.evaluatorCode" placeholder="评价人工号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="评价人姓名">
              <el-input v-model="form.evaluatorName" placeholder="评价人姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="评价日期">
              <el-date-picker v-model="form.evalDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">已提交</el-radio>
                <el-radio :value="0">草稿</el-radio>
              </el-radio-group>
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

<style scoped lang="scss">
.evaluation-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
  .form-tip {
    margin-bottom: 16px;
  }
  .score-total {
    font-weight: 600;
    color: var(--el-color-primary);
  }
}
</style>
