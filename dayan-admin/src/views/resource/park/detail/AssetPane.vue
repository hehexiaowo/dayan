<script setup lang="ts">
/**
 * 素材仓库面板（锁定模式传 refType1+refCode / 全局模式不传）：图片/视频/文档/VR 四类素材管理，复用本组件。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{refType1, refCode, assetType}）。
 * 通过 assetType prop 区分图片(1)/视频(2)/文件(3)/VR(4)，复用同一组件。
 * storageType 区分本地 OSS（上传得 key）与外链（手填完整 http(s) URL）；
 * refType1/refType2/refCode 为冗余分类三元组（业务维度/细分分类/关联编码），
 * 真实引用关系由各业务表持有，删除受引用保护。
 */
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { useBusinessDictOptions } from '@/composables/useBusinessDict'
import FileUploader from '@/components/FileUploader/index.vue'
import { formatFileUrl } from '@/utils/file'
import {
  pageAssets,
  createAsset,
  updateAsset,
  deleteAsset
} from '@/api/system-asset'
import {
  STORAGE_TYPE_OPTIONS,
  REF_TYPE1_OPTIONS,
  refType1Label,
  refType2Label,
  storageTypeLabel,
  fileSizeLabel
} from '@/types/asset'
import type { SystemAsset, SystemAssetQuery } from '@/types/asset'

const props = defineProps<{
  /** 类型1：业务维度（锁定模式传如 'park'；不传=全局模式可自由筛选） */
  refType1?: string
  /** 关联编码（锁定模式传业务实体编码） */
  refCode?: string
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

/** 是否外链模式（storageType=2） */
const isExternal = computed(() => form.storageType === 2)

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  SystemAsset,
  SystemAssetQuery,
  number
>(
  {
    page: pageAssets,
    create: createAsset,
    update: (id, data) => updateAsset(id, data),
    remove: deleteAsset
  },
  {
    initialQuery: { keyword: undefined, refType1: undefined, refType2: undefined, refCode: undefined, storageType: undefined, isCover: undefined, status: undefined },
    idKey: 'id',
    fixedParams: {
      ...(props.refType1 ? { refType1: props.refType1 } : {}),
      ...(props.refCode ? { refCode: props.refCode } : {}),
      assetType: props.assetType
    }
  }
)

loadPage()

// ---------- 字典 ----------
const { options: refType2Options } = useBusinessDictOptions('asset_ref_type2')
const { options: vrProviderOptions } = useBusinessDictOptions('vr_provider')

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SystemAsset>({
  id: undefined,
  assetType: props.assetType,
  refType1: 'platform',
  refType2: undefined,
  refCode: '',
  storageType: 1,
  assetUrl: '',
  assetName: '',
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
  sortOrder: 0,
  status: 1
})

const rules: FormRules<SystemAsset> = {
  assetUrl: [{
    required: true,
    validator: (_rule, value: string, callback) => {
      if (!value) {
        callback(new Error(form.storageType === 2 ? '请输入外链地址' : '请上传文件'))
        return
      }
      const isHttp = value.startsWith('http://') || value.startsWith('https://')
      if (form.storageType === 2 && !isHttp) {
        callback(new Error('外链地址必须以 http:// 或 https:// 开头'))
        return
      }
      if (form.storageType === 1 && isHttp) {
        callback(new Error('本地OSS 模式请上传文件；http 开头的外部地址请切换为「外链」'))
        return
      }
      callback()
    },
    trigger: 'change'
  }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    assetType: props.assetType,
    refType1: 'platform',
    refType2: undefined,
    refCode: '',
    storageType: 1,
    assetUrl: '',
    assetName: '',
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
    sortOrder: 0,
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.refType1 = props.refType1 ?? 'platform'
  form.refCode = props.refCode ?? ''
  dialogVisible.value = true
}

function openEdit(row: SystemAsset) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

/** 切换存储方式时清掉地址，避免 OSS key / 外链 URL 混用误提交 */
function onStorageTypeChange() {
  form.assetUrl = ''
  form.fileSize = undefined
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

async function handleDelete(row: SystemAsset) {
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
      <el-form-item v-if="!refType1" label="类型1">
        <el-select v-model="query.refType1" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="o in REF_TYPE1_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!refCode" label="关联编码">
        <el-input v-model="query.refCode" placeholder="机构/商品等编码" clearable style="width: 150px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="query.keyword" placeholder="名称/URL 关键字" clearable style="width: 160px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="类型2">
        <el-select v-model="query.refType2" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="d in refType2Options" :key="d.dictCode" :label="d.dictName" :value="d.dictCode" />
        </el-select>
      </el-form-item>
      <el-form-item label="存储">
        <el-select v-model="query.storageType" placeholder="全部" clearable style="width: 110px">
          <el-option v-for="o in STORAGE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="assetType === 1" label="封面">
        <el-select v-model="query.isCover" placeholder="全部" clearable style="width: 110px">
          <el-option label="封面" :value="1" />
          <el-option label="非封面" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
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
      <el-table-column v-else-if="assetType === 2" label="视频" min-width="150">
        <template #default="{ row }">
          <span v-if="row.assetName" class="video-name">{{ row.assetName }}</span>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <!-- 文件 -->
      <el-table-column v-else label="文件" min-width="150">
        <template #default="{ row }">
          <span v-if="row.assetName" class="file-name">{{ row.assetName }}</span>
          <span v-else>--</span>
        </template>
      </el-table-column>

      <el-table-column prop="assetName" label="名称" min-width="150" show-overflow-tooltip />
      <el-table-column v-if="!refType1" label="类型1" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.refType1 === 'park' ? 'success' : 'info'">
            {{ refType1Label(row.refType1) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="类型2" width="110" align="center">
        <template #default="{ row }">{{ refType2Label(refType2Options, row.refType2) }}</template>
      </el-table-column>
      <el-table-column prop="refCode" label="关联编码" width="110" align="center" show-overflow-tooltip>
        <template #default="{ row }">{{ row.refCode || '—' }}</template>
      </el-table-column>
      <el-table-column label="存储" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.storageType === 2 ? 'warning' : 'info'">
            {{ storageTypeLabel(row.storageType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="assetType === 1" label="尺寸" width="110" align="center">
        <template #default="{ row }">
          <span v-if="row.width || row.height">{{ row.width }}×{{ row.height }}</span>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="100" align="right">
        <template #default="{ row }">{{ fileSizeLabel(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column v-if="assetType === 1" label="封面" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isCover === 1" type="warning" size="small">封面</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
      <el-table-column prop="status" label="状态" width="85" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="110" align="center">
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
            <el-form-item label="存储方式" prop="storageType">
              <el-radio-group v-model="form.storageType" :disabled="dialogMode === 'edit'" @change="onStorageTypeChange">
                <el-radio :value="1">本地上传（OSS）</el-radio>
                <el-radio :value="2">外部链接</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="文件" prop="assetUrl">
              <FileUploader
                v-if="!isExternal"
                v-model="form.assetUrl"
                :type="uploaderType"
                module="system"
              />
              <el-input
                v-else
                v-model="form.assetUrl"
                placeholder="https://example.com/video.mp4（完整外链地址）"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型1">
              <el-select v-model="form.refType1" :disabled="!!refType1" style="width: 100%">
                <el-option v-for="o in REF_TYPE1_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型2">
              <el-select v-model="form.refType2" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="d in refType2Options" :key="d.dictCode" :label="d.dictName" :value="d.dictCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联编码">
              <el-input v-model="form.refCode" placeholder="机构/商品等业务编码（平台素材留空）" :disabled="!!refCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称">
              <el-input v-model="form.assetName" placeholder="文件名称" />
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
                <FileUploader v-model="form.coverUrl" type="image" module="system" />
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
                <el-select v-model="form.vrProvider" placeholder="请选择" clearable style="width: 100%">
                  <el-option
                    v-for="d in vrProviderOptions"
                    :key="d.dictCode"
                    :label="d.dictName"
                    :value="d.dictCode"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="缩略图">
                <FileUploader v-model="form.thumbnailUrl" type="image" module="system" />
              </el-form-item>
            </el-col>
          </template>

          <el-col v-if="!isExternal" :span="12">
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
