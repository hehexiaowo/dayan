<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageContentMedia,
  createContentMedia,
  updateContentMedia,
  deleteContentMedia,
  deleteContentMediaByCode
} from '@/api/content-sub'
import type { ContentMedia, ContentMediaQuery } from '@/types/content'
import { MediaType, MEDIA_TYPE_OPTIONS } from '@/types/content'
import FileUploader from '@/components/FileUploader/index.vue'

/**
 * 内容媒体资源 tab（按 contentCode 分组的 CRUD）。
 */
const props = defineProps<{ contentCode: string }>()

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<ContentMedia, ContentMediaQuery, number>(
  {
    page: pageContentMedia,
    create: createContentMedia,
    update: (id, data) => updateContentMedia(id, data),
    remove: deleteContentMedia
  },
  {
    initialQuery: { mediaType: undefined },
    idKey: 'id',
    fixedParams: { contentCode: props.contentCode }
  }
)

const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ContentMedia>({
  contentCode: props.contentCode,
  mediaType: MediaType.IMAGE,
  mediaUrl: '',
  thumbnailUrl: '',
  mediaName: '',
  fileFormat: '',
  fileSize: undefined,
  width: undefined,
  height: undefined,
  duration: undefined,
  mediaDescription: '',
  isInBody: 0,
  sortOrder: 0
})

const rules: FormRules<ContentMedia> = {
  mediaType: [{ required: true, message: '请选择媒体类型', trigger: 'change' }],
  mediaUrl: [{ required: true, message: '请上传媒体资源', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    contentCode: props.contentCode,
    mediaType: MediaType.IMAGE,
    mediaUrl: '',
    thumbnailUrl: '',
    mediaName: '',
    fileFormat: '',
    fileSize: undefined,
    width: undefined,
    height: undefined,
    duration: undefined,
    mediaDescription: '',
    isInBody: 0,
    sortOrder: 0
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ContentMedia) {
  dialogType.value = 'edit'
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
    if (dialogType.value === 'create') {
      await createContentMedia(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateContentMedia(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ContentMedia) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除媒体「${row.mediaName || row.mediaUrl}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteContentMedia(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

async function handleClearAll() {
  await ElMessageBox.confirm('确定清空该内容下所有媒体资源吗？此操作不可恢复。', '危险操作', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteContentMediaByCode(props.contentCode)
  ElMessage.success('已清空')
  loadPage()
}

function mediaTypeLabel(t?: number) {
  return MEDIA_TYPE_OPTIONS.find((o) => o.value === t)?.label ?? '-'
}

loadPage()
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 12px">
      <el-select v-model="query.mediaType" placeholder="全部类型" clearable style="width: 160px" @change="handleSearch">
        <el-option v-for="o in MEDIA_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <div>
        <el-button type="danger" plain :icon="'Delete'" @click="handleClearAll">清空全部</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增媒体</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column label="预览" width="90">
        <template #default="{ row }">
          <el-image
            v-if="row.thumbnailUrl || row.mediaType === MediaType.IMAGE"
            :src="row.thumbnailUrl || row.mediaUrl"
            fit="cover"
            style="width: 60px; height: 60px"
            :preview-src-list="[row.mediaUrl]"
            preview-teleported
          />
          <el-tag v-else type="info">{{ mediaTypeLabel(row.mediaType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="mediaName" label="名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="mediaType" label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag>{{ mediaTypeLabel(row.mediaType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileFormat" label="格式" width="90" />
      <el-table-column prop="fileSize" label="大小(KB)" width="100" align="right" />
      <el-table-column prop="isInBody" label="正文引用" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isInBody === 1 ? 'success' : 'info'">{{ row.isInBody === 1 ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 12px">
      <el-pagination
        :current-page="query.current"
        :page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增媒体' : '编辑媒体'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="媒体类型" prop="mediaType">
              <el-select v-model="form.mediaType" style="width: 100%">
                <el-option v-for="o in MEDIA_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称">
              <el-input v-model="form.mediaName" placeholder="资源名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="资源地址" prop="mediaUrl">
              <FileUploader v-model="form.mediaUrl" :type="form.mediaType === MediaType.IMAGE ? 'image' : 'file'" module="content" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.mediaType === MediaType.VIDEO || form.mediaType === MediaType.IMAGE" :span="24">
            <el-form-item label="缩略图">
              <FileUploader v-model="form.thumbnailUrl" type="image" module="content" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="格式">
              <el-input v-model="form.fileFormat" placeholder="如 mp4/jpg" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="大小(KB)">
              <el-input-number v-model="form.fileSize" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="宽(px)">
              <el-input-number v-model="form.width" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="高(px)">
              <el-input-number v-model="form.height" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时长(秒)">
              <el-input-number v-model="form.duration" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="正文引用">
              <el-switch :model-value="form.isInBody === 1" @change="(v: boolean) => (form.isInBody = v ? 1 : 0)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.mediaDescription" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
