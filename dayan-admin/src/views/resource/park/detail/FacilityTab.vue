<script setup lang="ts">
/**
 * 机构详情页 - 设施 tab。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{parkCode}）。
 * 搜索：facilityName + facilityCategory + status。
 * facilityCode 必填（业务编码，编辑时 disabled，update 不可改）；facilityName 必填。
 * isFree 布尔提交 0/1；images 字段用 textarea 原文编辑（JSON 字符串）。
 *
 * 红线：主键 Long id；编码字段 update 不可改；parkCode 从 prop 带入 create 表单隐藏。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageFacilities,
  createFacility,
  updateFacility,
  deleteFacility
} from '@/api/park-facility'
import { FACILITY_CATEGORY_OPTIONS, facilityCategoryLabel } from '@/types/park'
import type { ParkFacility, ParkFacilityQuery } from '@/types/park'
import FileUploader from '@/components/FileUploader/index.vue'

const props = defineProps<{
  parkCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkFacility,
  ParkFacilityQuery,
  number
>(
  {
    page: pageFacilities,
    create: createFacility,
    update: (id, data) => updateFacility(id, data),
    remove: deleteFacility
  },
  {
    initialQuery: { facilityCode: '', facilityName: '', facilityCategory: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { parkCode: props.parkCode }
  }
)

loadPage()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ParkFacility>({
  id: undefined,
  parkCode: '',
  facilityCode: '',
  facilityName: '',
  facilityCategory: undefined,
  buildingName: '',
  floor: '',
  area: undefined,
  capacity: undefined,
  openTime: '',
  facilityDescription: '',
  coverImage: '',
  images: '',
  isFree: 0,
  feeDescription: '',
  sortOrder: 0,
  status: 1
})

const rules: FormRules<ParkFacility> = {
  facilityCode: [
    { required: true, message: '请输入设施编码', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  facilityName: [
    { required: true, message: '请输入设施名称', trigger: 'blur' },
    { max: 200, message: '不超过 200 个字符', trigger: 'blur' }
  ]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    parkCode: '',
    facilityCode: '',
    facilityName: '',
    facilityCategory: undefined,
    buildingName: '',
    floor: '',
    area: undefined,
    capacity: undefined,
    openTime: '',
    facilityDescription: '',
    coverImage: '',
    images: '',
    isFree: 0,
    feeDescription: '',
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

function openEdit(row: ParkFacility) {
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
      await createFacility(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateFacility(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParkFacility) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该设施记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteFacility(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function statusLabel(v?: number): string {
  return v === 1 ? '启用' : '停用'
}
function statusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}
function freeLabel(v?: number): string {
  return v === 1 ? '免费' : '收费'
}
function freeTagType(v?: number): 'success' | 'warning' {
  return v === 1 ? 'success' : 'warning'
}
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}
defineExpose({ loadPage })
</script>

<template>
  <div class="facility-tab">
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="设施名称">
        <el-input v-model="query.facilityName" placeholder="设施名称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="设施类别">
        <el-select v-model="query.facilityCategory" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in FACILITY_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
        <el-button :icon="'Plus'" @click="openCreate">新增设施</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="facilityCode" label="设施编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="facilityName" label="设施名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="facilityCategory" label="类别" width="90" align="center">
        <template #default="{ row }">{{ facilityCategoryLabel(row.facilityCategory) }}</template>
      </el-table-column>
      <el-table-column prop="buildingName" label="楼栋" width="120" show-overflow-tooltip />
      <el-table-column prop="floor" label="楼层" width="90" align="center" />
      <el-table-column prop="area" label="面积(㎡)" width="100" align="right" />
      <el-table-column prop="capacity" label="容纳人数" width="100" align="right" />
      <el-table-column label="收费" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="freeTagType(row.isFree)" size="small">{{ freeLabel(row.isFree) }}</el-tag>
        </template>
      </el-table-column>
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
      :title="dialogMode === 'create' ? '新增设施' : '编辑设施'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设施编码" prop="facilityCode">
              <el-input
                v-model="form.facilityCode"
                placeholder="业务编码（同机构下唯一）"
                maxlength="50"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设施名称" prop="facilityName">
              <el-input v-model="form.facilityName" placeholder="设施名称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设施类别">
              <el-select v-model="form.facilityCategory" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in FACILITY_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="楼栋名称">
              <el-input v-model="form.buildingName" placeholder="楼栋名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="楼层">
              <el-input v-model="form.floor" placeholder="楼层" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="面积(㎡)">
              <el-input-number v-model="form.area" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="容纳人数">
              <el-input-number v-model="form.capacity" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开放时间">
              <el-input v-model="form.openTime" placeholder="如 08:00-22:00" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否免费">
              <el-switch v-model="form.isFree" :active-value="1" :inactive-value="0" />
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
            <el-form-item label="收费说明">
              <el-input v-model="form.feeDescription" placeholder="收费说明" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="park" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图片(JSON)">
              <el-input v-model="form.images" type="textarea" :rows="2" placeholder="图片列表 JSON 原文" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="设施描述">
              <el-input v-model="form.facilityDescription" type="textarea" :rows="2" placeholder="设施描述" />
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
.facility-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
