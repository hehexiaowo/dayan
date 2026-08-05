<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageDistributors,
  getDistributor,
  createDistributor,
  updateDistributor,
  deleteDistributor
} from '@/api/distributor'
import type { DistributorInfo, DistributorInfoQuery } from '@/types/distributor'
import RegionSelect from '@/components/RegionSelect.vue'
import {
  SubjectType,
  Gender,
  DistributorStatus,
  SUBJECT_TYPE_OPTIONS,
  GENDER_OPTIONS,
  DISTRIBUTOR_STATUS_OPTIONS
} from '@/types/distributor'

/**
 * 分销商管理页。
 *
 * - 标准 CRUD（搜索 + 表格 + 分页 + 新增/编辑弹窗）；
 * - 主键 distributorCode 由服务端 CodeGenerator 生成，新增表单不含该字段；
 * - 修改走 PUT query string（distributorCode=@RequestParam），与 content/info 一致。
 *
 * 状态约定：
 * - subjectType：1企业 / 2个体工商户 / 3个人
 * - gender：0未知 / 1男 / 2女
 * - status：1启用 / 0禁用
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
} = useCrud<DistributorInfo, DistributorInfoQuery>(
  { page: pageDistributors },
  {
    initialQuery: {
      distributorCode: '',
      fullName: '',
      subjectType: undefined,
      status: undefined
    }
  }
)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<DistributorInfo>({
  distributorCode: undefined,
  fullName: '',
  shortName: '',
  subjectType: SubjectType.ENTERPRISE,
  unifiedCreditCode: '',
  legalPerson: '',
  businessLicenseNo: '',
  registeredCapital: undefined,
  establishDate: '',
  gender: Gender.UNKNOWN,
  phone: '',
  contactPerson: '',
  contactEmail: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  bankName: '',
  bankAccount: '',
  bankAccountName: '',
  status: DistributorStatus.ENABLED,
  sortOrder: 0,
  remark: ''
})

const rules: FormRules<DistributorInfo> = {
  fullName: [{ required: true, message: '请输入分销商全称', trigger: 'blur' }],
  subjectType: [{ required: true, message: '请选择主体类型', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    distributorCode: undefined,
    fullName: '',
    shortName: '',
    subjectType: SubjectType.ENTERPRISE,
    unifiedCreditCode: '',
    legalPerson: '',
    businessLicenseNo: '',
    registeredCapital: undefined,
    establishDate: '',
    gender: Gender.UNKNOWN,
    phone: '',
    contactPerson: '',
    contactEmail: '',
    provinceCode: '',
    cityCode: '',
    districtCode: '',
    address: '',
    bankName: '',
    bankAccount: '',
    bankAccountName: '',
    status: DistributorStatus.ENABLED,
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
function fillForm(detail: DistributorInfo) {
  Object.assign(form, {
    distributorCode: detail.distributorCode,
    fullName: detail.fullName ?? '',
    shortName: detail.shortName ?? '',
    subjectType: detail.subjectType ?? SubjectType.ENTERPRISE,
    unifiedCreditCode: detail.unifiedCreditCode ?? '',
    legalPerson: detail.legalPerson ?? '',
    businessLicenseNo: detail.businessLicenseNo ?? '',
    registeredCapital: detail.registeredCapital,
    establishDate: detail.establishDate ?? '',
    gender: detail.gender ?? Gender.UNKNOWN,
    phone: detail.phone ?? '',
    contactPerson: detail.contactPerson ?? '',
    contactEmail: detail.contactEmail ?? '',
    provinceCode: detail.provinceCode ?? '',
    cityCode: detail.cityCode ?? '',
    districtCode: detail.districtCode ?? '',
    address: detail.address ?? '',
    bankName: detail.bankName ?? '',
    bankAccount: detail.bankAccount ?? '',
    bankAccountName: detail.bankAccountName ?? '',
    status: detail.status ?? DistributorStatus.ENABLED,
    sortOrder: detail.sortOrder ?? 0,
    remark: detail.remark ?? ''
  })
}

async function openEdit(row: DistributorInfo) {
  if (!row.distributorCode) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getDistributor(row.distributorCode)
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
      await createDistributor(form)
      ElMessage.success('新增成功')
    } else if (form.distributorCode) {
      await updateDistributor(form.distributorCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  query.distributorCode = ''
  query.fullName = ''
  query.subjectType = undefined
  query.status = undefined
  handleSearch()
}

async function handleDeleteRow(row: DistributorInfo) {
  if (!row.distributorCode) return
  await ElMessageBox.confirm(`确定删除「${row.fullName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteDistributor(row.distributorCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function subjectTypeLabel(t?: number): string {
  const found = SUBJECT_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function statusLabel(s?: number): string {
  const found = DISTRIBUTOR_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/** 根据启用状态返回 el-tag type：启用 success / 禁用 info。 */
function statusTagType(status?: number): 'success' | 'info' {
  return status === DistributorStatus.ENABLED ? 'success' : 'info'
}

/** 注册资本显示（万元兜底）。 */
function capitalLabel(v?: number): string {
  if (v == null) return '--'
  return `${v} 万元`
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="分销商编码">
          <el-input
            v-model="query.distributorCode"
            placeholder="分销商编码"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="分销商全称">
          <el-input
            v-model="query.fullName"
            placeholder="全称关键字"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="主体类型">
          <el-select v-model="query.subjectType" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="o in SUBJECT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in DISTRIBUTOR_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>分销商列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增分销商</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="distributorCode">
        <el-table-column prop="distributorCode" label="分销商编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="fullName" label="全称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="shortName" label="简称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="subjectType" label="主体类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ subjectTypeLabel(row.subjectType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="unifiedCreditCode" label="统一信用代码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="legalPerson" label="法定代表人" min-width="100" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" min-width="120" show-overflow-tooltip />
        <el-table-column prop="contactPerson" label="联系人" min-width="100" show-overflow-tooltip />
        <el-table-column prop="registeredCapital" label="注册资本" width="120" align="center">
          <template #default="{ row }">
            {{ capitalLabel(row.registeredCapital) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
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
      :title="dialogType === 'create' ? '新增分销商' : '编辑分销商'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="分销商全称" prop="fullName">
              <el-input v-model="form.fullName" placeholder="分销商全称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="简称">
              <el-input v-model="form.shortName" placeholder="简称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主体类型" prop="subjectType">
              <el-select v-model="form.subjectType" placeholder="主体类型" style="width: 100%">
                <el-option v-for="o in SUBJECT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="性别" style="width: 100%">
                <el-option v-for="o in GENDER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="联系电话" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactPerson" placeholder="联系人" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱">
              <el-input v-model="form.contactEmail" placeholder="联系邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所在地区">
              <RegionSelect
                v-model:province-code="form.provinceCode"
                v-model:city-code="form.cityCode"
                v-model:district-code="form.districtCode"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址">
              <el-input v-model="form.address" placeholder="详细地址" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开户行">
              <el-input v-model="form.bankName" placeholder="开户行" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="银行账号">
              <el-input v-model="form.bankAccount" placeholder="银行账号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开户名">
              <el-input v-model="form.bankAccountName" placeholder="开户名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" placeholder="状态" style="width: 100%">
                <el-option v-for="o in DISTRIBUTOR_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
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
