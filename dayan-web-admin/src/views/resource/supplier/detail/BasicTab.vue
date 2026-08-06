<script setup lang="ts">
/**
 * 供应商详情页 - 基本信息 tab。
 *
 * 只读展示 SupplierInfo 关键字段（el-descriptions），提供"编辑"按钮打开 el-dialog + el-form
 * 修改主表（提交 updateSupplier）。字段集参考供应商主列表页编辑表单。
 *
 * 红线：supplierCode 由服务端生成不可改（编辑弹窗内 disabled）；
 *      status / auditStatus 由后端审核流程驱动，编辑表单**不包含**，仅只读展示。
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getSupplier, updateSupplier } from '@/api/supplier'
import {
  SupplierType,
  SUPPLIER_TYPE_OPTIONS,
  SUPPLIER_STATUS_OPTIONS,
  SUPPLIER_AUDIT_STATUS_OPTIONS
} from '@/types/supplier'
import type { SupplierInfo } from '@/types/supplier'

const props = defineProps<{
  /** 供应商编码（从详情页路由 prop 带入） */
  supplierCode: string
}>()

const loading = ref(false)
const supplierInfo = ref<SupplierInfo | null>(null)

async function loadDetail() {
  if (!props.supplierCode) return
  loading.value = true
  try {
    supplierInfo.value = await getSupplier(props.supplierCode)
  } catch {
    supplierInfo.value = null
  } finally {
    loading.value = false
  }
}

loadDetail()

// ---------- 辅助渲染 ----------
function supplierTypeLabel(v?: number): string {
  const found = SUPPLIER_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}
function statusLabel(s?: number): string {
  const found = SUPPLIER_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : '--'
}
function statusTagType(status?: number): 'success' | 'info' | 'warning' | 'danger' {
  if (status === 1) return 'success' // 已合作
  if (status === 0) return 'info' // 待审核
  if (status === 2) return 'warning' // 已暂停
  if (status === 3) return 'danger' // 已终止
  return 'info'
}
function auditStatusLabel(s?: number): string {
  const found = SUPPLIER_AUDIT_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : '--'
}
function auditStatusTagType(s?: number): 'success' | 'warning' | 'danger' | 'info' {
  if (s === 1) return 'success' // 通过
  if (s === 0) return 'warning' // 待审
  if (s === 2) return 'danger' // 驳回
  return 'info'
}
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}

// ---------- 编辑弹窗 ----------
const dialogVisible = ref(false)
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
  sortOrder: 0,
  remark: ''
})

const rules: FormRules<SupplierInfo> = {
  fullName: [{ required: true, message: '请输入供应商全称', trigger: 'blur' }],
  supplierType: [{ required: true, message: '请选择供应商类型', trigger: 'change' }]
}

function openEdit() {
  if (!supplierInfo.value) return
  Object.assign(form, {
    supplierCode: supplierInfo.value.supplierCode,
    fullName: supplierInfo.value.fullName ?? '',
    shortName: supplierInfo.value.shortName ?? '',
    supplierType: supplierInfo.value.supplierType ?? SupplierType.ORGANIZATION,
    unifiedCreditCode: supplierInfo.value.unifiedCreditCode ?? '',
    legalPerson: supplierInfo.value.legalPerson ?? '',
    registeredCapital: supplierInfo.value.registeredCapital,
    establishDate: supplierInfo.value.establishDate ?? '',
    businessLicenseNo: supplierInfo.value.businessLicenseNo ?? '',
    businessScope: supplierInfo.value.businessScope ?? '',
    provinceCode: supplierInfo.value.provinceCode ?? '',
    cityCode: supplierInfo.value.cityCode ?? '',
    districtCode: supplierInfo.value.districtCode ?? '',
    address: supplierInfo.value.address ?? '',
    contactPerson: supplierInfo.value.contactPerson ?? '',
    contactPhone: supplierInfo.value.contactPhone ?? '',
    contactEmail: supplierInfo.value.contactEmail ?? '',
    logoUrl: supplierInfo.value.logoUrl ?? '',
    description: supplierInfo.value.description ?? '',
    commissionRate: supplierInfo.value.commissionRate,
    sortOrder: supplierInfo.value.sortOrder ?? 0,
    remark: supplierInfo.value.remark ?? ''
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
  if (!form.supplierCode) return
  submitLoading.value = true
  try {
    await updateSupplier(form.supplierCode, form)
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

/** 暴露刷新方法，供详情页外部刷新 */
defineExpose({ loadDetail })
</script>

<template>
  <div v-loading="loading">
    <template v-if="supplierInfo">
      <div class="basic-toolbar">
        <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="供应商编码">{{ supplierInfo.supplierCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="供应商全称">{{ supplierInfo.fullName }}</el-descriptions-item>
        <el-descriptions-item label="简称">{{ supplierInfo.shortName ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag size="small" type="info">{{ supplierTypeLabel(supplierInfo.supplierType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="统一信用代码">{{ supplierInfo.unifiedCreditCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="法定代表人">{{ supplierInfo.legalPerson ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ supplierInfo.contactPerson ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ supplierInfo.contactPhone ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="佣金比例">
          {{ supplierInfo.commissionRate != null ? `${supplierInfo.commissionRate}%` : '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(supplierInfo.status)" size="small">
            {{ statusLabel(supplierInfo.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="auditStatusTagType(supplierInfo.auditStatus)" size="small">
            {{ auditStatusLabel(supplierInfo.auditStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="排序号">{{ supplierInfo.sortOrder ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱" :span="3">{{ supplierInfo.contactEmail ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="3">{{ supplierInfo.address ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="经营范围" :span="3">{{ supplierInfo.businessScope ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="3">{{ supplierInfo.description ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(supplierInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ supplierInfo.remark ?? '--' }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-else-if="!loading" description="未加载到供应商信息" />

    <!-- 编辑弹窗（不含 status / auditStatus，由后端审核流程驱动） -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑供应商基本信息"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="供应商编码">
              <el-input v-model="form.supplierCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
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
  </div>
</template>

<style scoped>
.basic-toolbar {
  margin-bottom: 16px;
}
</style>
