<script setup lang="ts">
/**
 * 机构详情页 - 顾问 tab。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{parkCode}）。
 * 搜索：adviserName + isPrimary + status；adviserName 必填。
 * isPrimary=1 首席，同机构唯一（后端自动互斥）——前端在 submit 前弹 confirm 提示用户。
 *
 * 红线：主键 Long id；parkCode 从 prop 带入 create 表单隐藏；isPrimary 布尔提交 0/1。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageAdvisers,
  createAdviser,
  updateAdviser,
  deleteAdviser
} from '@/api/park-adviser'
import type { ParkAdviser, ParkAdviserQuery } from '@/types/park'
import FileUploader from '@/components/FileUploader/index.vue'

const props = defineProps<{
  parkCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkAdviser,
  ParkAdviserQuery,
  number
>(
  {
    page: pageAdvisers,
    create: createAdviser,
    update: (id, data) => updateAdviser(id, data),
    remove: deleteAdviser
  },
  {
    initialQuery: { adviserName: '', isPrimary: undefined, status: undefined },
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

const form = reactive<ParkAdviser>({
  id: undefined,
  parkCode: '',
  adviserName: '',
  adviserTitle: '',
  adviserImage: '',
  adviserContent: '',
  contactPhone: '',
  isPrimary: 0,
  sortOrder: 0,
  status: 1
})

const rules: FormRules<ParkAdviser> = {
  adviserName: [
    { required: true, message: '请输入顾问姓名', trigger: 'blur' },
    { max: 200, message: '不超过 200 个字符', trigger: 'blur' }
  ]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    parkCode: '',
    adviserName: '',
    adviserTitle: '',
    adviserImage: '',
    adviserContent: '',
    contactPhone: '',
    isPrimary: 0,
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

function openEdit(row: ParkAdviser) {
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
  // isPrimary 互斥提示：设为首席时弹 confirm 提醒将自动取消本机构其他首席
  if (form.isPrimary === 1) {
    try {
      await ElMessageBox.confirm(
        '设为首席将自动取消本机构其他首席，是否继续？',
        '首席互斥提示',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      return
    }
  }
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createAdviser(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateAdviser(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParkAdviser) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该顾问记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteAdviser(row.id)
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
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}
defineExpose({ loadPage })
</script>

<template>
  <div class="adviser-tab">
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="顾问姓名">
        <el-input v-model="query.adviserName" placeholder="顾问姓名" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="是否首席">
        <el-select v-model="query.isPrimary" placeholder="全部" clearable style="width: 120px">
          <el-option label="首席" :value="1" />
          <el-option label="普通" :value="0" />
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
        <el-button :icon="'Plus'" @click="openCreate">新增顾问</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="adviserName" label="顾问姓名" min-width="140" show-overflow-tooltip />
      <el-table-column prop="adviserTitle" label="头衔" min-width="140" show-overflow-tooltip />
      <el-table-column prop="contactPhone" label="联系电话" width="150" show-overflow-tooltip />
      <el-table-column label="首席" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isPrimary === 1" type="success" size="small">首席</el-tag>
          <span v-else>普通</span>
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
      :title="dialogMode === 'create' ? '新增顾问' : '编辑顾问'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="顾问姓名" prop="adviserName">
              <el-input v-model="form.adviserName" placeholder="顾问姓名" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="头衔">
              <el-input v-model="form.adviserTitle" placeholder="顾问头衔" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否首席">
              <div class="primary-row">
                <el-switch v-model="form.isPrimary" :active-value="1" :inactive-value="0" />
                <span class="primary-tip">设为首席将自动取消本机构其他首席</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="头像">
              <FileUploader v-model="form.adviserImage" type="image" module="park" register-asset :asset-park-code="props.parkCode" asset-source-type="adviser" :asset-source-ref="form.adviserName" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="顾问介绍">
              <el-input v-model="form.adviserContent" type="textarea" :rows="3" placeholder="顾问介绍" />
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
.adviser-tab {
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
