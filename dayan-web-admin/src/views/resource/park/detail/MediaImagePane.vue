<script setup lang="ts">
/**
 * 机构详情页 - 媒体库/图片子面板。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{parkCode}）。
 * 搜索：imageType + isCover + status；imageUrl 必填（@NotBlank）。
 * isCover/isFree 等布尔字段提交 0/1。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageMediaImages,
  createMediaImage,
  updateMediaImage,
  deleteMediaImage
} from '@/api/park-media'
import type { ParkMediaImage, ParkMediaImageQuery } from '@/types/park'

const props = defineProps<{
  parkCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkMediaImage,
  ParkMediaImageQuery,
  number
>(
  {
    page: pageMediaImages,
    create: createMediaImage,
    update: (id, data) => updateMediaImage(id, data),
    remove: deleteMediaImage
  },
  {
    initialQuery: { imageType: undefined, isCover: undefined, status: undefined },
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

const form = reactive<ParkMediaImage>({
  id: undefined,
  parkCode: '',
  imageUrl: '',
  imageName: '',
  imageType: undefined,
  imageDescription: '',
  width: undefined,
  height: undefined,
  fileSize: undefined,
  sortOrder: 0,
  isCover: 0,
  status: 1
})

const rules: FormRules<ParkMediaImage> = {
  imageUrl: [{ required: true, message: '请输入图片 URL', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    parkCode: '',
    imageUrl: '',
    imageName: '',
    imageType: undefined,
    imageDescription: '',
    width: undefined,
    height: undefined,
    fileSize: undefined,
    sortOrder: 0,
    isCover: 0,
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.parkCode = props.parkCode
  dialogVisible.value = true
}

function openEdit(row: ParkMediaImage) {
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
      await createMediaImage(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateMediaImage(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParkMediaImage) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该图片记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteMediaImage(row.id)
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
  <div class="media-image-pane">
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="图片类型">
        <el-input-number
          v-model="query.imageType"
          :min="0"
          controls-position="right"
          placeholder="类型"
          style="width: 120px"
        />
      </el-form-item>
      <el-form-item label="是否封面">
        <el-select v-model="query.isCover" placeholder="全部" clearable style="width: 120px">
          <el-option label="封面" :value="1" />
          <el-option label="非封面" :value="0" />
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
        <el-button :icon="'Plus'" @click="openCreate">新增图片</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column label="图片" min-width="120" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.imageUrl"
            :src="row.imageUrl"
            :preview-src-list="[row.imageUrl]"
            preview-teleported
            fit="cover"
            style="width: 64px; height: 48px"
          />
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="imageName" label="图片名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="imageType" label="类型" width="90" align="center" />
      <el-table-column label="尺寸" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.width || row.height">{{ row.width }}×{{ row.height }}</span>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小(B)" width="110" align="right" />
      <el-table-column label="封面" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isCover === 1" type="warning" size="small">封面</el-tag>
          <span v-else>—</span>
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
      :title="dialogMode === 'create' ? '新增图片' : '编辑图片'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="图片URL" prop="imageUrl">
              <el-input v-model="form.imageUrl" placeholder="图片 URL（必填）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图片名称">
              <el-input v-model="form.imageName" placeholder="图片名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图片类型">
              <el-input-number v-model="form.imageType" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="宽度(px)">
              <el-input-number v-model="form.width" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="高度(px)">
              <el-input-number v-model="form.height" :min="0" controls-position="right" style="width: 100%" />
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
            <el-form-item label="是否封面">
              <el-switch v-model="form.isCover" :active-value="1" :inactive-value="0" />
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
            <el-form-item label="图片描述">
              <el-input v-model="form.imageDescription" type="textarea" :rows="2" placeholder="图片描述" />
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
.media-image-pane {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
