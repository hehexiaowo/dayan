<script setup lang="ts">
/**
 * 机构详情页 - 媒体库/视频子面板。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{parkCode}）。
 * 搜索：videoType + status；videoUrl 必填。
 * duration（秒）表格展示，fileSize 字节。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageMediaVideos,
  createMediaVideo,
  updateMediaVideo,
  deleteMediaVideo
} from '@/api/park-media'
import { VIDEO_TYPE_OPTIONS, videoTypeLabel, fileSizeLabel } from '@/types/park'
import type { ParkMediaVideo, ParkMediaVideoQuery } from '@/types/park'

const props = defineProps<{
  parkCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkMediaVideo,
  ParkMediaVideoQuery,
  number
>(
  {
    page: pageMediaVideos,
    create: createMediaVideo,
    update: (id, data) => updateMediaVideo(id, data),
    remove: deleteMediaVideo
  },
  {
    initialQuery: { videoType: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { parkCode: props.parkCode }
  }
)

loadPage()

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ParkMediaVideo>({
  id: undefined,
  parkCode: '',
  videoUrl: '',
  coverUrl: '',
  videoName: '',
  videoType: undefined,
  videoDescription: '',
  duration: undefined,
  fileSize: undefined,
  sortOrder: 0,
  status: 1
})

const rules: FormRules<ParkMediaVideo> = {
  videoUrl: [{ required: true, message: '请输入视频 URL', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    parkCode: '',
    videoUrl: '',
    coverUrl: '',
    videoName: '',
    videoType: undefined,
    videoDescription: '',
    duration: undefined,
    fileSize: undefined,
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

function openEdit(row: ParkMediaVideo) {
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
      await createMediaVideo(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateMediaVideo(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParkMediaVideo) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该视频记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteMediaVideo(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

function statusLabel(v?: number): string {
  return v === 1 ? '启用' : '停用'
}
function statusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}
/** duration（秒）格式化为 mm:ss */
function formatDuration(s?: number): string {
  if (s == null) return '--'
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${sec.toString().padStart(2, '0')}`
}
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}
defineExpose({ loadPage })
</script>

<template>
  <div class="media-video-pane">
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="视频类型">
        <el-input-number
          v-model="query.videoType"
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
        <el-button :icon="'Plus'" @click="openCreate">新增视频</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column label="封面" min-width="120" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.coverUrl"
            :src="row.coverUrl"
            :preview-src-list="[row.coverUrl]"
            preview-teleported
            fit="cover"
            style="width: 64px; height: 48px"
          />
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="videoName" label="视频名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="videoType" label="类型" width="90" align="center">
        <template #default="{ row }">{{ videoTypeLabel(row.videoType) }}</template>
      </el-table-column>
      <el-table-column label="时长" width="100" align="center">
        <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="110" align="right">
        <template #default="{ row }">{{ fileSizeLabel(row.fileSize) }}</template>
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
      :title="dialogMode === 'create' ? '新增视频' : '编辑视频'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="视频URL" prop="videoUrl">
              <el-input v-model="form.videoUrl" placeholder="视频 URL（必填）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面URL">
              <el-input v-model="form.coverUrl" placeholder="封面图 URL" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="视频名称">
              <el-input v-model="form.videoName" placeholder="视频名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="视频类型">
              <el-select v-model="form.videoType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in VIDEO_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时长(秒)">
              <el-input-number v-model="form.duration" :min="0" controls-position="right" style="width: 100%" />
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
            <el-form-item label="视频描述">
              <el-input v-model="form.videoDescription" type="textarea" :rows="2" placeholder="视频描述" />
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
.media-video-pane {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
