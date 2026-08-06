<script setup lang="ts">
/**
 * 机构详情页 - 媒体库/文档子面板。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{parkCode}）。
 * 搜索：fileType + status；fileUrl 必填。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageMediaFiles,
  createMediaFile,
  updateMediaFile,
  deleteMediaFile
} from '@/api/park-media'
import { FILE_TYPE_OPTIONS, fileTypeLabel, fileSizeLabel } from '@/types/park'
import type { ParkMediaFile, ParkMediaFileQuery } from '@/types/park'

const props = defineProps<{
  parkCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkMediaFile,
  ParkMediaFileQuery,
  number
>(
  {
    page: pageMediaFiles,
    create: createMediaFile,
    update: (id, data) => updateMediaFile(id, data),
    remove: deleteMediaFile
  },
  {
    initialQuery: { fileType: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { parkCode: props.parkCode }
  }
)

loadPage()

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ParkMediaFile>({
  id: undefined,
  parkCode: '',
  fileUrl: '',
  fileName: '',
  fileType: undefined,
  fileFormat: '',
  fileSize: undefined,
  fileDescription: '',
  sortOrder: 0,
  status: 1
})

const rules: FormRules<ParkMediaFile> = {
  fileUrl: [{ required: true, message: '请输入文档 URL', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    parkCode: '',
    fileUrl: '',
    fileName: '',
    fileType: undefined,
    fileFormat: '',
    fileSize: undefined,
    fileDescription: '',
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

function openEdit(row: ParkMediaFile) {
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
      await createMediaFile(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateMediaFile(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParkMediaFile) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该文档记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteMediaFile(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

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
  <div class="media-file-pane">
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="文档类型">
        <el-input-number
          v-model="query.fileType"
          :min="0"
          controls-position="right"
          placeholder="类型"
          style="width: 120px"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增文档</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="fileName" label="文档名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="fileFormat" label="格式" width="100" align="center" />
      <el-table-column prop="fileType" label="类型" width="90" align="center">
        <template #default="{ row }">{{ fileTypeLabel(row.fileType) }}</template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="110" align="right">
        <template #default="{ row }">{{ fileSizeLabel(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="fileDescription" label="描述" min-width="160" show-overflow-tooltip />
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
      :title="dialogMode === 'create' ? '新增文档' : '编辑文档'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="文档URL" prop="fileUrl">
              <el-input v-model="form.fileUrl" placeholder="文档 URL（必填）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文档名称">
              <el-input v-model="form.fileName" placeholder="文档名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文档格式">
              <el-input v-model="form.fileFormat" placeholder="如 pdf/docx" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文档类型">
              <el-select v-model="form.fileType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in FILE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文件大小(B)">
              <el-input-number v-model="form.fileSize" :min="0" controls-position="right" style="width: 100%" />
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
            <el-form-item label="文档描述">
              <el-input v-model="form.fileDescription" type="textarea" :rows="2" placeholder="文档描述" />
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
.media-file-pane {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
