<script setup lang="ts">
/**
 * 机构详情页 - 媒体库/VR 子面板。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{parkCode}）。
 * 搜索：vrType + status；vrUrl 必填。vrType：1全景图 / 2 3D 模型 / 3视频。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageMediaVrs,
  createMediaVr,
  updateMediaVr,
  deleteMediaVr
} from '@/api/park-media'
import type { ParkMediaVr, ParkMediaVrQuery } from '@/types/park'

/** VR 类型选项：1全景图 / 2 3D 模型 / 3视频 */
const VR_TYPE_OPTIONS = [
  { label: '全景图', value: 1 },
  { label: '3D 模型', value: 2 },
  { label: '视频', value: 3 }
] as const

function vrTypeLabel(v?: number): string {
  const found = VR_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

const props = defineProps<{
  parkCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkMediaVr,
  ParkMediaVrQuery,
  number
>(
  {
    page: pageMediaVrs,
    create: createMediaVr,
    update: (id, data) => updateMediaVr(id, data),
    remove: deleteMediaVr
  },
  {
    initialQuery: { vrType: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { parkCode: props.parkCode }
  }
)

loadPage()

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ParkMediaVr>({
  id: undefined,
  parkCode: '',
  vrUrl: '',
  vrProvider: '',
  vrName: '',
  vrType: undefined,
  thumbnailUrl: '',
  vrDescription: '',
  sortOrder: 0,
  status: 1
})

const rules: FormRules<ParkMediaVr> = {
  vrUrl: [{ required: true, message: '请输入 VR URL', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    parkCode: '',
    vrUrl: '',
    vrProvider: '',
    vrName: '',
    vrType: undefined,
    thumbnailUrl: '',
    vrDescription: '',
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

function openEdit(row: ParkMediaVr) {
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
      await createMediaVr(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateMediaVr(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParkMediaVr) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该 VR 记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteMediaVr(row.id)
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
  <div class="media-vr-pane">
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="VR 类型">
        <el-select v-model="query.vrType" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in VR_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
        <el-button :icon="'Plus'" @click="openCreate">新增 VR</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column label="缩略图" min-width="120" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.thumbnailUrl"
            :src="row.thumbnailUrl"
            :preview-src-list="[row.thumbnailUrl]"
            preview-teleported
            fit="cover"
            style="width: 64px; height: 48px"
          />
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="vrName" label="VR 名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">{{ vrTypeLabel(row.vrType) }}</template>
      </el-table-column>
      <el-table-column prop="vrProvider" label="提供方" width="130" show-overflow-tooltip />
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
      :title="dialogMode === 'create' ? '新增 VR' : '编辑 VR'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="VR URL" prop="vrUrl">
              <el-input v-model="form.vrUrl" placeholder="VR URL（必填）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="VR 名称">
              <el-input v-model="form.vrName" placeholder="VR 名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="VR 类型">
              <el-select v-model="form.vrType" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in VR_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提供方">
              <el-input v-model="form.vrProvider" placeholder="VR 提供方" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="缩略图URL">
              <el-input v-model="form.thumbnailUrl" placeholder="缩略图 URL" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="VR 描述">
              <el-input v-model="form.vrDescription" type="textarea" :rows="2" placeholder="VR 描述" />
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
.media-vr-pane {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
