<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageSuppliers,
  getSupplier,
  createSupplier,
  updateSupplier,
  deleteSupplier,
  auditSupplier
} from '@/api/supplier'
import type { SupplierInfo, SupplierInfoQuery } from '@/types/supplier'
import {
  SupplierType,
  SupplierStatus,
  SupplierAuditStatus,
  SUPPLIER_TYPE_OPTIONS,
  SUPPLIER_STATUS_OPTIONS,
  SUPPLIER_AUDIT_STATUS_OPTIONS
} from '@/types/supplier'

/**
 * 供应商管理页。
 *
 * - 标准 CRUD（搜索 + 表格 + 分页 + 新增/编辑弹窗）；
 * - 审核流：audit（待审→通过/驳回），独立审核弹窗；
 * - 主键 supplierCode 由服务端 CodeGenerator 生成，新增表单不含该字段；
 * - 修改走 PUT query string（supplierCode=@RequestParam）。
 *
 * 状态约定：
 * - supplierType：1机构 / 2服务商 / 3商品供应商
 * - status：1启用 / 0禁用
 * - auditStatus：0待审 / 1通过 / 2驳回
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
} = useCrud<SupplierInfo, SupplierInfoQuery>(
  { page: pageSuppliers },
  {
    initialQuery: {
      supplierCode: '',
      fullName: '',
      supplierType: undefined,
      status: undefined,
      auditStatus: undefined
    }
  }
)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SupplierInfo>({
  supplierCode: undefined,
  fullName: '',
  shortName: '',
  supplierType: SupplierType.ORGANIZATION,
  unifiedCreditCode: '',
  legalPerson: '',
  registeredCapital: undefined,
  establishDate: '',
  businessLicenseNo: '',
  businessScope: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  logoUrl: '',
  description: '',
  commissionRate: undefined,
  status: SupplierStatus.ENABLED,
  sortOrder: 0,
  remark: ''
})

const rules: FormRules<SupplierInfo> = {
  fullName: [{ required: true, message: '请输入供应商全称', trigger: 'blur' }],
  supplierType: [{ required: true, message: '请选择供应商类型', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    supplierCode: undefined,
    fullName: '',
    shortName: '',
    supplierType: SupplierType.ORGANIZATION,
    unifiedCreditCode: '',
    legalPerson: '',
    registeredCapital: undefined,
    establishDate: '',
    businessLicenseNo: '',
    businessScope: '',
    provinceCode: '',
    cityCode: '',
    districtCode: '',
    address: '',
    contactPerson: '',
    contactPhone: '',
    contactEmail: '',
    logoUrl: '',
    description: '',
    commissionRate: undefined,
    status: SupplierStatus.ENABLED,
    sortOrder: 0,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

/** 将详情/行数据回填到表单（缺省值兜底，避免 undefined 渲染问题）。 */
function fillForm(detail: SupplierInfo) {
  Object.assign(form, {
    supplierCode: detail.supplierCode,
    fullName: detail.fullName ?? '',
    shortName: detail.shortName ?? '',
    supplierType: detail.supplierType ?? SupplierType.ORGANIZATION,
    unifiedCreditCode: detail.unifiedCreditCode ?? '',
    legalPerson: detail.legalPerson ?? '',
    registeredCapital: detail.registeredCapital,
    establishDate: detail.establishDate ?? '',
    businessLicenseNo: detail.businessLicenseNo ?? '',
    businessScope: detail.businessScope ?? '',
    provinceCode: detail.provinceCode ?? '',
    cityCode: detail.cityCode ?? '',
    districtCode: detail.districtCode ?? '',
    address: detail.address ?? '',
    contactPerson: detail.contactPerson ?? '',
    contactPhone: detail.contactPhone ?? '',
    contactEmail: detail.contactEmail ?? '',
    logoUrl: detail.logoUrl ?? '',
    description: detail.description ?? '',
    commissionRate: detail.commissionRate,
    status: detail.status ?? SupplierStatus.ENABLED,
    sortOrder: detail.sortOrder ?? 0,
    remark: detail.remark ?? ''
  })
}

async function openEdit(row: SupplierInfo) {
  if (!row.supplierCode) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getSupplier(row.supplierCode)
    fillForm(detail)
  } catch {
    // 拉取详情失败时回退到行数据
    fillForm(row)
  }
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
    if (dialogType.value === 'create') {
      await createSupplier(form)
      ElMessage.success('新增成功')
    } else if (form.supplierCode) {
      await updateSupplier(form.supplierCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  query.supplierCode = ''
  query.fullName = ''
  query.supplierType = undefined
  query.status = undefined
  query.auditStatus = undefined
  handleSearch()
}

async function handleDeleteRow(row: SupplierInfo) {
  if (!row.supplierCode) return
  await ElMessageBox.confirm(`确定删除「${row.fullName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteSupplier(row.supplierCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 审核弹窗 ----------
const auditDialogVisible = ref(false)
const auditSubmitLoading = ref(false)
const auditForm = reactive<{
  supplierCode: string
  supplierName: string
  auditStatus: number
  auditRemark: string
}>({
  supplierCode: '',
  supplierName: '',
  // 1=通过 / 2=驳回（对齐后端 SupplierAuditDTO），默认通过
  auditStatus: SupplierAuditStatus.PASS,
  auditRemark: ''
})

function openAudit(row: SupplierInfo) {
  if (!row.supplierCode) return
  auditForm.supplierCode = row.supplierCode
  auditForm.supplierName = row.fullName ?? ''
  auditForm.auditStatus = SupplierAuditStatus.PASS
  auditForm.auditRemark = row.auditRemark ?? ''
  auditDialogVisible.value = true
}

async function handleAuditSubmit() {
  auditSubmitLoading.value = true
  try {
    await auditSupplier({
      supplierCode: auditForm.supplierCode,
      auditStatus: auditForm.auditStatus,
      auditRemark: auditForm.auditRemark || undefined
    })
    ElMessage.success(auditForm.auditStatus === SupplierAuditStatus.PASS ? '已通过' : '已驳回')
    auditDialogVisible.value = false
    loadPage()
  } finally {
    auditSubmitLoading.value = false
  }
}

// ---------- 辅助渲染 ----------
function supplierTypeLabel(t?: number): string {
  const found = SUPPLIER_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function statusLabel(s?: number): string {
  const found = SUPPLIER_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

function auditStatusLabel(s?: number): string {
  const found = SUPPLIER_AUDIT_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/** 根据启用状态返回 el-tag type：启用 success / 禁用 info / 暂停 warning。 */
function statusTagType(status?: number): 'success' | 'info' | 'warning' {
  if (status === SupplierStatus.ENABLED) return 'success'
  if (status === SupplierStatus.SUSPENDED) return 'warning'
  return 'info'
}

/** 根据审核状态返回 el-tag type：通过 success / 待审 warning / 驳回 danger。 */
function auditStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case SupplierAuditStatus.PASS:
      return 'success'
    case SupplierAuditStatus.PENDING:
      return 'warning'
    case SupplierAuditStatus.REJECT:
      return 'danger'
    default:
      return 'info'
  }
}

/** 佣金比例显示（百分比）。 */
function commissionLabel(v?: number): string {
  if (v == null) return '--'
  return `${v}%`
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="供应商编码">
          <el-input
            v-model="query.supplierCode"
            placeholder="供应商编码"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="供应商全称">
          <el-input
            v-model="query.fullName"
            placeholder="全称关键字"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="供应商类型">
          <el-select v-model="query.supplierType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in SUPPLIER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in SUPPLIER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in SUPPLIER_AUDIT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>供应商列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增供应商</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="supplierCode">
        <el-table-column prop="supplierCode" label="供应商编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="fullName" label="全称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="shortName" label="简称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="supplierType" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ supplierTypeLabel(row.supplierType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="unifiedCreditCode" label="统一信用代码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="legalPerson" label="法定代表人" min-width="100" show-overflow-tooltip />
        <el-table-column prop="contactPerson" label="联系人" min-width="100" show-overflow-tooltip />
        <el-table-column prop="commissionRate" label="佣金比例" width="100" align="center">
          <template #default="{ row }">
            {{ commissionLabel(row.commissionRate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditStatus" label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="auditStatusTagType(row.auditStatus)">
              {{ auditStatusLabel(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.auditStatus === SupplierAuditStatus.PENDING"
              link
              type="primary"
              size="small"
              @click="openAudit(row)"
            >
              审核
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增供应商' : '编辑供应商'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="供应商全称" prop="fullName">
              <el-input v-model="form.fullName" placeholder="供应商全称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="简称">
              <el-input v-model="form.shortName" placeholder="简称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商类型" prop="supplierType">
              <el-select v-model="form.supplierType" placeholder="供应商类型" style="width: 100%">
                <el-option v-for="o in SUPPLIER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="统一信用代码">
              <el-input v-model="form.unifiedCreditCode" placeholder="统一社会信用代码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="营业执照号">
              <el-input v-model="form.businessLicenseNo" placeholder="营业执照号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="法定代表人">
              <el-input v-model="form.legalPerson" placeholder="法定代表人" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="注册资本">
              <el-input-number
                v-model="form.registeredCapital"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成立日期">
              <el-input v-model="form.establishDate" placeholder="yyyy-MM-dd" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认佣金比例">
              <el-input-number
                v-model="form.commissionRate"
                :min="0"
                :max="100"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="省级编码">
              <el-input v-model="form.provinceCode" placeholder="省级编码" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="市级编码">
              <el-input v-model="form.cityCode" placeholder="市级编码" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县编码">
              <el-input v-model="form.districtCode" placeholder="区县编码" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址">
              <el-input v-model="form.address" placeholder="详细地址" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="经营范围">
              <el-input v-model="form.businessScope" type="textarea" :rows="2" placeholder="经营范围" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactPerson" placeholder="联系人" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="联系电话" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱">
              <el-input v-model="form.contactEmail" placeholder="联系邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Logo">
              <el-input v-model="form.logoUrl" placeholder="Logo URL" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" placeholder="状态" style="width: 100%">
                <el-option v-for="o in SUPPLIER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="供应商描述" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="内部备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" title="供应商审核" width="520px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="供应商名称">
          <span>{{ auditForm.supplierName }}</span>
        </el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" placeholder="审核备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitLoading" @click="handleAuditSubmit">确定</el-button>
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
