<script setup lang="ts">
/**
 * 供应商合同管理（独立列表页）。
 *
 * 全局合同列表，标准 CRUD + 6 态状态流转。
 *
 * 主键：contractCode（String，后端 CodeGenerator 生成 HT 前缀，create 表单不含）。
 * 搜索：contractCode / contractName(模糊) / supplierCode / contractType / status。
 *
 * 红线：
 * - 合同 update 的 contractCode 是 @RequestParam（query param），不是 path variable
 *   （见 api/supplier-contract.ts 的 updateContract）。因此**不使用 useCrud 的 handleUpdate**，
 *   仅用 useCrud 提供 page/loading/tableData/total/query/loadPage/handleSearch/handlePageChange/handleSizeChange。
 * - 后端不校验合同状态流转合法性，前端严格按状态守卫表显示流转按钮：
 *     0 草稿   → 提交审核(1) / 作废(5)
 *     1 待审核 → 审核通过(2) / 驳回(0) / 作废(5)
 *     2 已生效 → 终止(4) / 作废(5)
 *     3/4/5    → 无流转（只查看）
 * - create 时 contractCode 后端生成（表单不含），status 默认 1（待审核，表单不含）。
 * - isAutoRenew 布尔提交 0/1；attachmentUrls 用 JSON 字符串原文。
 */
import { reactive, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageContracts,
  createContract,
  updateContract,
  deleteContract
} from '@/api/supplier-contract'
import {
  ContractStatus,
  CONTRACT_STATUS_OPTIONS,
  CONTRACT_TYPE_OPTIONS,
  SETTLEMENT_CYCLE_OPTIONS
} from '@/types/supplier'
import type { SupplierContract, SupplierContractQuery } from '@/types/supplier'

// 从详情页"前往合同管理"跳转时可携带 supplierCode / contractCode 作为初始过滤（路由 query）
const route = useRoute()
const initialSupplierCode = (route.query.supplierCode as string) || ''
const initialContractCode = (route.query.contractCode as string) || ''

// ---------- 列表（useCrud 仅用于读侧：page/搜索/分页） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<SupplierContract, SupplierContractQuery>(
    { page: pageContracts },
    {
      initialQuery: {
        contractCode: initialContractCode,
        contractName: '',
        supplierCode: initialSupplierCode,
        contractType: undefined,
        status: undefined
      }
    }
  )

loadPage()

function handleReset() {
  query.contractCode = ''
  query.contractName = ''
  query.supplierCode = ''
  query.contractType = undefined
  query.status = undefined
  handleSearch()
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SupplierContract>({
  contractCode: '',
  contractName: '',
  supplierCode: '',
  organCode: '',
  contractType: undefined,
  signDate: '',
  effectiveDate: '',
  expireDate: '',
  contractAmount: undefined,
  commissionRate: undefined,
  settlementCycle: undefined,
  terms: '',
  attachmentUrls: '',
  signPerson: '',
  signSealImage: '',
  isAutoRenew: 0,
  renewCount: undefined,
  parentContractCode: '',
  status: ContractStatus.PENDING_AUDIT,
  auditRemark: '',
  remark: ''
})

const rules: FormRules<SupplierContract> = {
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  supplierCode: [{ required: true, message: '请输入供应商编码', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    contractCode: '',
    contractName: '',
    supplierCode: '',
    organCode: '',
    contractType: undefined,
    signDate: '',
    effectiveDate: '',
    expireDate: '',
    contractAmount: undefined,
    commissionRate: undefined,
    settlementCycle: undefined,
    terms: '',
    attachmentUrls: '',
    signPerson: '',
    signSealImage: '',
    isAutoRenew: 0,
    renewCount: undefined,
    parentContractCode: '',
    status: ContractStatus.PENDING_AUDIT,
    auditRemark: '',
    remark: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  // 若路由带 supplierCode 过滤，则预填
  if (initialSupplierCode) form.supplierCode = initialSupplierCode
  dialogVisible.value = true
}

function openEdit(row: SupplierContract) {
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
      // create 不含 contractCode（后端生成）/ status（后端默认 1）
      await createContract(form)
      ElMessage.success('新增成功')
    } else if (form.contractCode) {
      // update 的 contractCode 走 query param（非 path variable）
      await updateContract(form.contractCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: SupplierContract) {
  if (!row.contractCode) return
  await ElMessageBox.confirm(`确定删除合同「${row.contractName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteContract(row.contractCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 状态流转（前端严格按守卫表显示按钮 + confirm/prompt 收集 auditRemark） ----------
/**
 * 状态流转守卫表（前端守卫，后端不校验）：
 *   当前 status → 可流转的目标 status 列表
 */
interface TransitionTarget {
  target: ContractStatus
  label: string
  actionType: 'primary' | 'success' | 'warning' | 'danger'
  /** 是否需要收集 auditRemark */
  needRemark: boolean
}

const TRANSITION_TABLE: Record<ContractStatus, TransitionTarget[]> = {
  [ContractStatus.DRAFT]: [
    { target: ContractStatus.PENDING_AUDIT, label: '提交审核', actionType: 'primary', needRemark: false },
    { target: ContractStatus.VOID, label: '作废', actionType: 'danger', needRemark: true }
  ],
  [ContractStatus.PENDING_AUDIT]: [
    { target: ContractStatus.EFFECTIVE, label: '审核通过', actionType: 'success', needRemark: true },
    { target: ContractStatus.DRAFT, label: '驳回', actionType: 'warning', needRemark: true },
    { target: ContractStatus.VOID, label: '作废', actionType: 'danger', needRemark: true }
  ],
  [ContractStatus.EFFECTIVE]: [
    { target: ContractStatus.TERMINATED, label: '终止', actionType: 'danger', needRemark: true },
    { target: ContractStatus.VOID, label: '作废', actionType: 'danger', needRemark: true }
  ],
  [ContractStatus.EXPIRED]: [],
  [ContractStatus.TERMINATED]: [],
  [ContractStatus.VOID]: []
}

/** 取当前行可执行的流转目标（按守卫表）。computed 包裹以便响应式。 */
function availableTransitions(row: SupplierContract): TransitionTarget[] {
  const s = (row.status ?? ContractStatus.DRAFT) as ContractStatus
  return TRANSITION_TABLE[s] ?? []
}

const transitionLoadingContractCode = ref<string>('')

async function handleTransition(row: SupplierContract, t: TransitionTarget) {
  if (!row.contractCode) return
  const payload: Partial<SupplierContract> = { status: t.target }
  if (t.needRemark) {
    // 用 prompt 收集 auditRemark（驳回/作废/终止等需要说明）
    let remark = ''
    try {
      const res = await ElMessageBox.prompt(
        `请输入审核备注（${t.label}）`,
        `${t.label}确认`,
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: '请输入审核备注（可选，建议填写）',
          inputValidator: () => true
        }
      )
      remark = (res.value ?? '').trim()
    } catch {
      return
    }
    payload.auditRemark = remark || undefined
  } else {
    // 不需要 remark 的流转（如提交审核）做二次确认
    try {
      await ElMessageBox.confirm(
        `确定对合同「${row.contractName}」执行「${t.label}」操作吗？`,
        '操作确认',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      return
    }
  }
  transitionLoadingContractCode.value = row.contractCode
  try {
    // update 的 contractCode 走 query param
    await updateContract(row.contractCode, payload)
    ElMessage.success(`${t.label}成功`)
    loadPage()
  } finally {
    transitionLoadingContractCode.value = ''
  }
}

// ---------- 辅助渲染 ----------
function statusLabel(v?: number): string {
  const found = CONTRACT_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}
/** 状态 tag 配色：0草稿info/1待审核warning/2已生效success/3已到期info/4已终止danger/5已作废info */
function statusTagType(v?: number): 'info' | 'warning' | 'success' | 'danger' {
  switch (v) {
    case ContractStatus.DRAFT:
      return 'info'
    case ContractStatus.PENDING_AUDIT:
      return 'warning'
    case ContractStatus.EFFECTIVE:
      return 'success'
    case ContractStatus.EXPIRED:
      return 'info'
    case ContractStatus.TERMINATED:
      return 'danger'
    case ContractStatus.VOID:
      return 'info'
    default:
      return 'info'
  }
}
function contractTypeLabel(v?: number): string {
  const found = CONTRACT_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}
function dateRange(a?: string, b?: string): string {
  return `${formatDate(a)} ~ ${formatDate(b)}`
}
function transitionButtonType(t: TransitionTarget): 'primary' | 'success' | 'warning' | 'danger' {
  return t.actionType
}

// 编辑模式下 contractCode 只读显示
const isEdit = computed(() => dialogMode.value === 'edit')
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="合同编码">
          <el-input v-model="query.contractCode" placeholder="合同编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="合同名称">
          <el-input v-model="query.contractName" placeholder="名称关键字" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="供应商编码">
          <el-input v-model="query.supplierCode" placeholder="供应商编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="合同类型">
          <el-select v-model="query.contractType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in CONTRACT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in CONTRACT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>供应商合同列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增合同</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="contractCode">
        <el-table-column prop="contractCode" label="合同编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="contractName" label="合同名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="supplierCode" label="供应商编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="contractType" label="类型" width="110" align="center">
          <template #default="{ row }">{{ contractTypeLabel(row.contractType) }}</template>
        </el-table-column>
        <el-table-column prop="signDate" label="签订日期" width="110" align="center">
          <template #default="{ row }">{{ formatDate(row.signDate) }}</template>
        </el-table-column>
        <el-table-column label="有效期" min-width="200" align="center">
          <template #default="{ row }">{{ dateRange(row.effectiveDate, row.expireDate) }}</template>
        </el-table-column>
        <el-table-column prop="contractAmount" label="合同金额" width="120" align="right" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-for="t in availableTransitions(row)"
              :key="t.target"
              link
              :type="transitionButtonType(t)"
              size="small"
              :loading="transitionLoadingContractCode === row.contractCode"
              @click="handleTransition(row, t)"
            >
              {{ t.label }}
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
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增合同' : '编辑合同'"
      width="860px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="合同编码">
              <el-input v-model="form.contractCode" :disabled="isEdit" placeholder="保存后由系统生成（HT 前缀）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同名称" prop="contractName">
              <el-input v-model="form.contractName" placeholder="合同名称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商编码" prop="supplierCode">
              <el-input v-model="form.supplierCode" placeholder="供应商编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构编码">
              <el-input v-model="form.organCode" placeholder="机构编码（可选）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合同类型">
              <el-select v-model="form.contractType" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in CONTRACT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="签订日期">
              <el-date-picker v-model="form.signDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="签订人">
              <el-input v-model="form.signPerson" placeholder="签订人" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生效日期">
              <el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="到期日期">
              <el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="自动续约">
              <el-switch v-model="form.isAutoRenew" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合同金额">
              <el-input-number v-model="form.contractAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="佣金比例(%)">
              <el-input-number v-model="form.commissionRate" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结算周期">
              <el-select v-model="form.settlementCycle" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in SETTLEMENT_CYCLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父合同编码">
              <el-input v-model="form.parentContractCode" placeholder="续约指向父合同（可选）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col v-if="isEdit" :span="12">
            <el-form-item label="状态">
              <el-tag :type="statusTagType(form.status)" size="default">{{ statusLabel(form.status) }}</el-tag>
              <span class="form-tip-text">状态由流转操作驱动，不在表单直接编辑</span>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="合同条款">
              <el-input v-model="form.terms" type="textarea" :rows="2" placeholder="合同条款" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="附件(JSON)">
              <el-input v-model="form.attachmentUrls" type="textarea" :rows="2" placeholder="附件 URL 列表 JSON 原文，如 [&quot;a.pdf&quot;]" />
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

.form-tip-text {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
