<script setup lang="ts">
/**
 * 供应商详情页 - 联系人 tab。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{supplierCode}）。
 * 搜索：contactName(模糊) + contactType + isPrimary；contactName 必填。
 * isPrimary=1 主联系人，同供应商唯一（后端自动互斥）——前端在 submit 前弹 confirm 提示用户。
 *
 * 红线：主键 Long id；supplierCode 从 prop 带入 create 表单隐藏；isPrimary 布尔提交 0/1。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageContacts,
  createContact,
  updateContact,
  deleteContact
} from '@/api/supplier-contact'
import { CONTACT_TYPE_OPTIONS } from '@/types/supplier'
import type { SupplierContact, SupplierContactQuery } from '@/types/supplier'

const props = defineProps<{
  supplierCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  SupplierContact,
  SupplierContactQuery,
  number
>(
  {
    page: pageContacts,
    create: createContact,
    update: (id, data) => updateContact(id, data),
    remove: deleteContact
  },
  {
    initialQuery: { contactName: '', contactType: undefined, isPrimary: undefined },
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

const form = reactive<SupplierContact>({
  id: undefined,
  supplierCode: '',
  contactName: '',
  contactType: undefined,
  position: '',
  phone: '',
  email: '',
  wechat: '',
  isPrimary: 0,
  remark: ''
})

const rules: FormRules<SupplierContact> = {
  contactName: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    supplierCode: '',
    contactName: '',
    contactType: undefined,
    position: '',
    phone: '',
    email: '',
    wechat: '',
    isPrimary: 0,
    remark: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.supplierCode = props.supplierCode
  dialogVisible.value = true
}

function openEdit(row: SupplierContact) {
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
  // isPrimary 互斥提示：设为主联系人时弹 confirm 提醒将自动取消该供应商其他主联系人
  if (form.isPrimary === 1) {
    try {
      await ElMessageBox.confirm(
        '设为主联系人将自动取消该供应商其他主联系人，是否继续？',
        '主联系人互斥提示',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      return
    }
  }
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createContact(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateContact(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: SupplierContact) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该联系人记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteContact(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function contactTypeLabel(v?: number): string {
  const found = CONTACT_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}
defineExpose({ loadPage })
</script>

<template>
  <div class="contact-tab">
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="联系人姓名">
        <el-input v-model="query.contactName" placeholder="联系人姓名" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="联系人类型">
        <el-select v-model="query.contactType" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in CONTACT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否主联系人">
        <el-select v-model="query.isPrimary" placeholder="全部" clearable style="width: 120px">
          <el-option label="主联系人" :value="1" />
          <el-option label="普通" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增联系人</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="contactName" label="联系人姓名" min-width="120" show-overflow-tooltip />
      <el-table-column prop="contactType" label="类型" width="90" align="center">
        <template #default="{ row }">{{ contactTypeLabel(row.contactType) }}</template>
      </el-table-column>
      <el-table-column prop="position" label="职位" min-width="120" show-overflow-tooltip />
      <el-table-column prop="phone" label="电话" width="140" show-overflow-tooltip />
      <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
      <el-table-column prop="wechat" label="微信" min-width="120" show-overflow-tooltip />
      <el-table-column label="主联系人" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isPrimary === 1" type="success" size="small">主联系人</el-tag>
          <span v-else>普通</span>
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
      :title="dialogMode === 'create' ? '新增联系人' : '编辑联系人'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系人姓名" prop="contactName">
              <el-input v-model="form.contactName" placeholder="联系人姓名" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人类型">
              <el-select v-model="form.contactType" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in CONTACT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-input v-model="form.position" placeholder="职位" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话">
              <el-input v-model="form.phone" placeholder="电话" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="微信">
              <el-input v-model="form.wechat" placeholder="微信" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否主联系人">
              <div class="primary-row">
                <el-switch v-model="form.isPrimary" :active-value="1" :inactive-value="0" />
                <span class="primary-tip">设为主联系人将自动取消该供应商其他主联系人</span>
              </div>
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
.contact-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
  .primary-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .primary-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
