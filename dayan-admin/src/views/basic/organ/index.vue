<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageOrgans, createOrgan, updateOrgan, deleteOrgan } from '@/api/organ'
import type { Organ, OrganQuery } from '@/types/organ'
import { OrganStatus, ORGAN_STATUS_OPTIONS, ORGAN_TYPE_OPTIONS } from '@/types/organ'
import RegionSelect from '@/components/RegionSelect.vue'
import FileUploader from '@/components/FileUploader/index.vue'

/**
 * 机构管理页。
 *
 * - CRUD 标准模式（useCrud 分页）；
 * - 机构编码 organCode 由后端自动生成，编辑时禁用；
 * - 机构为扁平结构（无 parentCode），故用分页表格而非树形。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  Organ,
  OrganQuery
>(
  {
    page: pageOrgans
  },
  {
    initialQuery: {
      organCode: '',
      fullName: '',
      organType: undefined,
      status: undefined
    }
  }
)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Organ>({
  organCode: undefined,
  fullName: '',
  shortName: '',
  organType: 1,
  unifiedCreditCode: '',
  legalPerson: '',
  registeredCapital: undefined,
  establishDate: '',
  businessScope: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  logoUrl: '',
  website: '',
  description: '',
  address: '',
  status: OrganStatus.ENABLED,
  sortOrder: 0,
  remark: ''
})

const rules: FormRules<Organ> = {
  fullName: [{ required: true, message: '请输入机构全称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    organCode: undefined,
    fullName: '',
    shortName: '',
    organType: 1,
    unifiedCreditCode: '',
    legalPerson: '',
    registeredCapital: undefined,
    establishDate: '',
    businessScope: '',
    provinceCode: '',
    cityCode: '',
    districtCode: '',
    contactPerson: '',
    contactPhone: '',
    contactEmail: '',
    logoUrl: '',
    website: '',
    description: '',
    address: '',
    status: OrganStatus.ENABLED,
    sortOrder: 0,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Organ) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, {
    organCode: row.organCode,
    fullName: row.fullName,
    shortName: row.shortName,
    organType: row.organType,
    unifiedCreditCode: row.unifiedCreditCode,
    legalPerson: row.legalPerson,
    registeredCapital: row.registeredCapital,
    establishDate: row.establishDate,
    businessScope: row.businessScope,
    provinceCode: row.provinceCode,
    cityCode: row.cityCode,
    districtCode: row.districtCode,
    contactPerson: row.contactPerson,
    contactPhone: row.contactPhone,
    contactEmail: row.contactEmail,
    logoUrl: row.logoUrl,
    website: row.website,
    description: row.description,
    address: row.address,
    status: row.status,
    sortOrder: row.sortOrder,
    remark: row.remark
  })
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
      await createOrgan(form)
      ElMessage.success('新增成功')
    } else if (form.organCode) {
      await updateOrgan(form.organCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: Organ) {
  if (!row.organCode) return
  await ElMessageBox.confirm(`确定删除机构「${row.fullName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteOrgan(row.organCode)
  ElMessage.success('删除成功')
  loadPage()
}

function handleReset() {
  query.fullName = ''
  query.organCode = ''
  query.organType = undefined
  query.status = undefined
  handleSearch()
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input v-model="query.organCode" placeholder="机构编码" clearable style="width: 150px" @keyup.enter="handleSearch" />
        <el-input v-model="query.fullName" placeholder="机构全称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        <el-select v-model="query.organType" placeholder="类型" clearable style="width: 140px">
          <el-option v-for="o in ORGAN_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="o in ORGAN_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">机构列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增机构</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="organCode">
        <el-table-column prop="organCode" label="机构编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="fullName" label="机构全称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="shortName" label="简称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="organType" label="类型" width="100" align="center">
          <template #default="{ row }">
            {{ ORGAN_TYPE_OPTIONS.find((o) => o.value === row.organType)?.label ?? row.organType }}
          </template>
        </el-table-column>
        <el-table-column prop="unifiedCreditCode" label="统一社会信用代码" min-width="180" show-overflow-tooltip />
        <el-table-column prop="legalPerson" label="法人" width="110" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
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
      :title="dialogType === 'create' ? '新增机构' : '编辑机构'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="机构编码">
              <el-input
                :model-value="dialogType === 'create' ? '保存时自动生成' : form.organCode"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构全称" prop="fullName">
              <el-input v-model="form.fullName" placeholder="机构全称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="简称">
              <el-input v-model="form.shortName" placeholder="简称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构类型">
              <el-select v-model="form.organType" placeholder="机构类型" style="width: 100%">
                <el-option v-for="o in ORGAN_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="统一信用代码">
              <el-input v-model="form.unifiedCreditCode" placeholder="统一社会信用代码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="法定代表人">
              <el-input v-model="form.legalPerson" placeholder="法定代表人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactPerson" placeholder="联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="注册资本">
              <el-input-number
                v-model="form.registeredCapital"
                :min="0"
                :precision="2"
                controls-position="right"
                placeholder="万元"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成立日期">
              <el-date-picker
                v-model="form.establishDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱">
              <el-input v-model="form.contactEmail" placeholder="联系邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="官网">
              <el-input v-model="form.website" placeholder="官网地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Logo">
              <FileUploader v-model="form.logoUrl" type="image" module="organ" />
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
            <el-form-item label="经营范围">
              <el-input v-model="form.businessScope" type="textarea" :rows="3" placeholder="经营范围" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="机构简介">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="机构简介" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in ORGAN_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址">
              <el-input v-model="form.address" placeholder="详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .card-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2329;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
