<script setup lang="ts">
/**
 * 机构详情页 - 素材库统一面板。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{parkCode, assetType}）。
 * 通过 assetType prop 区分图片(1)/视频(2)/文件(3)/VR(4)，复用同一组件。
 * 表格"来源"列展示 sourceType，素材库直传的显示"素材库"。
 */
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import FileUploader from '@/components/FileUploader/index.vue'
import { formatFileUrl } from '@/utils/file'
import {
  pageAssets,
  createAsset,
  updateAsset,
  deleteAsset
} from '@/api/park-asset'
import {
  categoryOptionsByType,
  categoryLabel,
  sourceTypeLabel,
  fileSizeLabel
} from '@/types/park'
import { SOURCE_TYPE_OPTIONS } from '@/types/park'
import type { ParkAsset, ParkAssetQuery } from '@/types/park'

const props = defineProps<{
  parkCode: string
  /** 素材类型：1=图片 2=视频 3=文件 4=VR */
  assetType: number
}>()

/** FileUploader type 映射 */
const uploaderType = computed(() => {
  switch (props.assetType) {
    case 1: return 'image' as const
    case 2: return 'video' as const
    case 3: return 'file' as const
    case 4: return 'vr' as const
    default: return 'any' as const
  }
})

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkAsset,
  ParkAssetQuery,
  number
>(
  {
    page: pageAssets,
    create: createAsset,
    update: (id, data) => updateAsset(id, data),
    remove: deleteAsset
  },
  {
    initialQuery: { assetCategory: undefined, sourceType: undefined, isCover: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { parkCode: props.parkCode, assetType: props.assetType }
  }
)

loadPage()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ParkAsset>({
  id: undefined,
  parkCode: '',
  assetType: props.assetType,
  assetUrl: '',
  assetName: '',
  assetCategory: undefined,
  description: '',
  fileSize: undefined,
  width: undefined,
  height: undefined,
  isCover: 0,
  coverUrl: '',
  duration: undefined,
  fileFormat: '',
  vrProvider: '',
  thumbnailUrl: '',
  sourceType: 'media_mgmt',
  sourceRefCode: undefined,
  sortOrder: 0,
  status: 1
})

const rules: FormRules<ParkAsset> = {
  assetUrl: [{ required: true, message: '请上传文件', trigger: 'change' }]
}

const categoryOpts = computed(() => categoryOptionsByType(props.assetType))

function resetForm() {
  Object.assign(form, {
    id: undefined,
    parkCode: '',
    assetType: props.assetType,
    assetUrl: '',
    assetName: '',
    assetCategory: undefined,
    description: '',
    fileSize: undefined,
    width: undefined,
    height: undefined,
    isCover: 0,
    coverUrl: '',
    duration: undefined,
    fileFormat: '',
    vrProvider: '',
    thumbnailUrl: '',
    sourceType: 'media_mgmt',
    sourceRefCode: undefined,
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

function openEdit(row: ParkAsset) {
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
      await createAsset(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateAsset(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParkAsset) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该素材记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteAsset(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function statusLabel(v?: number): string {
  return v === 1 ? '显示' : '隐藏'
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
  <div class="asset-pane">
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="分类">
        <el-select v-model="query.assetCategory" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in categoryOpts" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源">
        <el-select v-model="query.sourceType" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in SOURCE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="assetType === 1" label="封面">
        <el-select v-model="query.isCover" placeholder="全部" clearable style="width: 120px">
          <el-option label="封面" :value="1" />
          <el-option label="非封面" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="显示" :value="1" />
          <el-option label="隐藏" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增素材</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <!-- 图片预览 -->
      <el-table-column v-if="assetType === 1 || assetType === 4" label="预览" min-width="100" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.assetUrl"
            :src="formatFileUrl(assetType === 4 ? (row.thumbnailUrl || row.assetUrl) : row.assetUrl)"
            :preview-src-list="[formatFileUrl(row.assetUrl)]"
            preview-teleported
            fit="cover"
            style="width: 64px; height: 48px"
          />
          <span v-else>--</span>
        </template>
      </el-table-column>
      <!-- 视频 -->
      <el-table-column v-else-if="assetType === 2" label="视频" min-width="160">
        <template #default="{ row }">
          <span v-if="row.assetName" class="video-name">{{ row.assetName }}</span>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <!-- 文件 -->
      <el-table-column v-else label="文件" min-width="160">
        <template #default="{ row }">
          <span v-if="row.assetName" class="file-name">{{ row.assetName }}</span>
          <span v-else>--</span>
        </template>
      </el-table-column>

      <el-table-column prop="assetName" label="名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="分类" width="100" align="center">
        <template #default="{ row }">{{ categoryLabel(assetType, row.assetCategory) }}</template>
      </el-table-column>
      <el-table-column v-if="assetType === 1" label="尺寸" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.width || row.height">{{ row.width }}×{{ row.height }}</span>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="110" align="right">
        <template #default="{ row }">{{ fileSizeLabel(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column v-if="assetType === 1" label="封面" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isCover === 1" type="warning" size="small">封面</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.sourceType === 'media_mgmt' ? 'info' : 'success'">
            {{ sourceTypeLabel(row.sourceType) }}
          </el-tag>
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
      :title="dialogMode === 'create' ? '新增素材' : '编辑素材'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="文件" prop="assetUrl">
              <FileUploader
                v-model="form.assetUrl"
                :type="uploaderType"
                module="park"
                :asset-park-code="parkCode"
                asset-source-type="media_mgmt"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称">
              <el-input v-model="form.assetName" placeholder="文件名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.assetCategory" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in categoryOpts" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>

          <!-- 图片专属 -->
          <template v-if="assetType === 1">
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
              <el-form-item label="是否封面">
                <el-switch v-model="form.isCover" :active-value="1" :inactive-value="0" />
              </el-form-item>
            </el-col>
          </template>

          <!-- 视频专属 -->
          <template v-if="assetType === 2">
            <el-col :span="24">
              <el-form-item label="封面图">
                <FileUploader v-model="form.coverUrl" type="image" module="park" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="时长(秒)">
                <el-input-number v-model="form.duration" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
          </template>

          <!-- 文件专属 -->
          <template v-if="assetType === 3">
            <el-col :span="12">
              <el-form-item label="文件格式">
                <el-input v-model="form.fileFormat" placeholder="如 pdf/doc/xls" />
              </el-form-item>
            </el-col>
          </template>

          <!-- VR 专属 -->
          <template v-if="assetType === 4">
            <el-col :span="12">
              <el-form-item label="VR提供商">
                <el-input v-model="form.vrProvider" placeholder="VR服务提供商" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="缩略图">
                <FileUploader v-model="form.thumbnailUrl" type="image" module="park" />
              </el-form-item>
            </el-col>
          </template>

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
                <el-radio :value="1">显示</el-radio>
                <el-radio :value="0">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述" />
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
.asset-pane {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
