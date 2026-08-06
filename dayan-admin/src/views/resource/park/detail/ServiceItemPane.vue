<script setup lang="ts">
/**
 * 机构详情页 - 周边/服务项目子面板。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{parkCode}）。
 * 搜索：serviceName + serviceCategory + isIncluded + status。
 * serviceCode 必填（业务编码，编辑时 disabled，update 不可改）；serviceName 必填。
 * isIncluded 布尔提交 0/1。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageServiceItems,
  createServiceItem,
  updateServiceItem,
  deleteServiceItem
} from '@/api/park-misc'
import { SERVICE_CATEGORY_OPTIONS, serviceCategoryLabel } from '@/types/park'
import type { ParkServiceItem, ParkServiceItemQuery } from '@/types/park'

const props = defineProps<{
  parkCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkServiceItem,
  ParkServiceItemQuery,
  number
>(
  {
    page: pageServiceItems,
    create: createServiceItem,
    update: (id, data) => updateServiceItem(id, data),
    remove: deleteServiceItem
  },
  {
    initialQuery: {
      serviceCode: '',
      serviceName: '',
      serviceCategory: undefined,
      isIncluded: undefined,
      status: undefined
    },
    idKey: 'id',
    fixedParams: { parkCode: props.parkCode }
  }
)

loadPage()

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ParkServiceItem>({
  id: undefined,
  parkCode: '',
  serviceCode: '',
  serviceName: '',
  serviceCategory: undefined,
  serviceDescription: '',
  isIncluded: 0,
  feeStandard: '',
  serviceFrequency: '',
  serviceDuration: '',
  coverImage: '',
  sortOrder: 0,
  status: 1
})

const rules: FormRules<ParkServiceItem> = {
  serviceCode: [
    { required: true, message: '请输入服务编码', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  serviceName: [
    { required: true, message: '请输入服务名称', trigger: 'blur' },
    { max: 200, message: '不超过 200 个字符', trigger: 'blur' }
  ]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    parkCode: '',
    serviceCode: '',
    serviceName: '',
    serviceCategory: undefined,
    serviceDescription: '',
    isIncluded: 0,
    feeStandard: '',
    serviceFrequency: '',
    serviceDuration: '',
    coverImage: '',
    sortOrder: 0,
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.parkCode = props.parkCode
  dialogVisible.value = true
}

function openEdit(row: ParkServiceItem) {
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
      await createServiceItem(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateServiceItem(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParkServiceItem) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该服务项记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteServiceItem(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

function statusLabel(v?: number): string {
  return v === 1 ? '启用' : '停用'
}
function statusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}
function includedLabel(v?: number): string {
  return v === 1 ? '包含' : '不包含'
}
function includedTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}
defineExpose({ loadPage })
</script>

<template>
  <div class="service-item-pane">
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="服务名称">
        <el-input v-model="query.serviceName" placeholder="服务名称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="服务类别">
        <el-select v-model="query.serviceCategory" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in SERVICE_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否包含">
        <el-select v-model="query.isIncluded" placeholder="全部" clearable style="width: 120px">
          <el-option label="包含" :value="1" />
          <el-option label="不包含" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增服务项</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="serviceCode" label="服务编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="serviceName" label="服务名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="serviceCategory" label="类别" width="90" align="center">
        <template #default="{ row }">{{ serviceCategoryLabel(row.serviceCategory) }}</template>
      </el-table-column>
      <el-table-column label="包含" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="includedTagType(row.isIncluded)" size="small">
            {{ includedLabel(row.isIncluded) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="feeStandard" label="收费标准" min-width="150" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
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
      :title="dialogMode === 'create' ? '新增服务项' : '编辑服务项'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="服务编码" prop="serviceCode">
              <el-input
                v-model="form.serviceCode"
                placeholder="业务编码（同机构下唯一）"
                maxlength="50"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务名称" prop="serviceName">
              <el-input v-model="form.serviceName" placeholder="服务名称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务类别">
              <el-select v-model="form.serviceCategory" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in SERVICE_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否包含">
              <el-switch v-model="form.isIncluded" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务频次">
              <el-input v-model="form.serviceFrequency" placeholder="如 每天/每周" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务时长">
              <el-input v-model="form.serviceDuration" placeholder="如 30分钟" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收费标准">
              <el-input v-model="form.feeStandard" placeholder="收费标准" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图URL">
              <el-input v-model="form.coverImage" placeholder="封面图 URL" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="服务描述">
              <el-input v-model="form.serviceDescription" type="textarea" :rows="3" placeholder="服务描述" />
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
.service-item-pane {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
