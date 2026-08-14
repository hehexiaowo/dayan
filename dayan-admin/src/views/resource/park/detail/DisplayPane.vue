<script setup lang="ts">
/**
 * 机构详情页 - 展示板块 tab。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{parkCode}）。
 * 搜索：blockType + status。
 *
 * 核心场景：一个机构 = N 个展示板块（品牌介绍/缴费方式/居住环境/文娱生活 等）。
 * 每个板块 = 类型 + 标题 + 富文本正文(textarea) + 多图上传(FileUploader multiple)。
 * images / imageDescriptions 在 DB 存为 JSON 数组字符串（TEXT 列），
 * 前端提交前 JSON.stringify，回显时 JSON.parse。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageDisplayBlocks,
  createDisplayBlock,
  updateDisplayBlock,
  deleteDisplayBlock
} from '@/api/park-display'
import type { ParkDisplayBlock, ParkDisplayBlockQuery } from '@/types/park'
import {
  DISPLAY_BLOCK_TYPE_OPTIONS,
  displayBlockTypeLabel,
  NETWORK_TYPE_OPTIONS,
  networkTypeLabel,
  networkTagsToList
} from '@/types/park'
import FileUploader from '@/components/FileUploader/index.vue'
import RichEditor from '@/components/RichEditor/index.vue'
import { formatFileUrl } from '@/utils/file'

const props = defineProps<{
  parkCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkDisplayBlock,
  ParkDisplayBlockQuery,
  number
>(
  {
    page: pageDisplayBlocks,
    create: createDisplayBlock,
    update: (id, data) => updateDisplayBlock(id, data),
    remove: deleteDisplayBlock
  },
  {
    initialQuery: { blockType: '', status: undefined },
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

// imagesArr / imageDescsArr 是前端操作的数组形态，提交时 stringify 为 images / imageDescriptions
const imagesArr = ref<string[]>([])
const imageDescsArr = ref<string[]>([])

/** 业态多选数组态：提交时 join 为 form.networkTags，回显时 split */
const networkTagsArr = ref<string[]>([])

const form = reactive({
  id: undefined as number | undefined,
  parkCode: '',
  blockType: '',
  blockTitle: '',
  content: '',
  images: '',
  imageDescriptions: '',
  sortOrder: 0,
  status: 1,
  networkTags: ''
})

const rules: FormRules = {
  blockType: [{ required: true, message: '请选择板块类型', trigger: 'change' }]
}

function resetForm() {
  form.id = undefined
  form.parkCode = ''
  form.blockType = ''
  form.blockTitle = ''
  form.content = ''
  form.images = ''
  form.imageDescriptions = ''
  form.sortOrder = 0
  form.status = 1
  form.networkTags = ''
  imagesArr.value = []
  imageDescsArr.value = []
  networkTagsArr.value = []
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.parkCode = props.parkCode
  dialogVisible.value = true
}

function openEdit(row: ParkDisplayBlock) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.parkCode = row.parkCode || ''
  form.blockType = row.blockType
  form.blockTitle = row.blockTitle || ''
  form.content = row.content || ''
  form.sortOrder = row.sortOrder ?? 0
  form.status = row.status ?? 1
  form.networkTags = row.networkTags || ''
  networkTagsArr.value = networkTagsToList(row.networkTags)
  // 解析 JSON 数组
  imagesArr.value = parseJsonArr(row.images)
  imageDescsArr.value = parseJsonArr(row.imageDescriptions)
  dialogVisible.value = true
}

function parseJsonArr(s?: string): string[] {
  if (!s) return []
  try {
    const arr = JSON.parse(s)
    return Array.isArray(arr) ? arr.map(String) : []
  } catch {
    return []
  }
}

/** 图片描述随图片列表变化对齐 */
function onImagesChange(v: string | string[]) {
  const keys = Array.isArray(v) ? v : []
  imagesArr.value = keys
  // 补齐描述数组长度
  while (imageDescsArr.value.length < keys.length) {
    imageDescsArr.value.push('')
  }
  imageDescsArr.value = imageDescsArr.value.slice(0, keys.length)
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
    // 数组 → JSON 字符串
    form.images = JSON.stringify(imagesArr.value)
    form.imageDescriptions = JSON.stringify(imageDescsArr.value)
    form.networkTags = networkTagsArr.value.join(',')
    if (dialogMode.value === 'create') {
      await createDisplayBlock(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateDisplayBlock(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParkDisplayBlock) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该展示板块？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteDisplayBlock(row.id)
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
/** 表格预览：取图片数组首张 */
function firstImage(row: ParkDisplayBlock): string | null {
  const arr = parseJsonArr(row.images)
  return arr.length > 0 ? arr[0] : null
}
function imageCount(row: ParkDisplayBlock): number {
  return parseJsonArr(row.images).length
}
function contentPreview(s?: string): string {
  if (!s) return '--'
  // 去掉 HTML 标签取纯文本
  return s.replace(/<[^>]+>/g, '').slice(0, 50) + (s.length > 50 ? '...' : '')
}

defineExpose({ loadPage })
</script>

<template>
  <div class="display-pane">
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="板块类型">
        <el-select v-model="query.blockType" placeholder="全部" clearable style="width: 160px">
          <el-option
            v-for="opt in DISPLAY_BLOCK_TYPE_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
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
        <el-button :icon="'Plus'" @click="openCreate">新增板块</el-button>
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      展示板块用于 C 端详情页：每个板块 = 类型 + 标题 + 正文 + 配图。替代旧的"主表加列"模式，新增板块类型无需改表结构。
    </el-alert>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="blockType" label="板块类型" width="130" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ displayBlockTypeLabel(row.blockType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="blockTitle" label="板块标题" min-width="150" show-overflow-tooltip />
      <el-table-column label="适用业态" width="220" align="center">
        <template #default="{ row }">
          <template v-if="networkTagsToList(row.networkTags).length">
            <el-tag
              v-for="t in networkTagsToList(row.networkTags)"
              :key="t"
              size="small"
              style="margin-right: 4px"
            >{{ networkTypeLabel(t) }}</el-tag>
          </template>
          <el-tag v-else size="small" type="info">全部</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="封面" width="80" align="center">
        <template #default="{ row }">
          <el-image
            v-if="firstImage(row)"
            :src="formatFileUrl(firstImage(row)!)"
            :preview-src-list="[formatFileUrl(firstImage(row)!)]"
            fit="cover"
            style="width: 50px; height: 50px; border-radius: 4px"
            preview-teleported
          />
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column label="正文预览" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ contentPreview(row.content) }}</template>
      </el-table-column>
      <el-table-column label="配图" width="80" align="center">
        <template #default="{ row }">{{ imageCount(row) }} 张</template>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增展示板块' : '编辑展示板块'"
      width="780px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="板块类型" prop="blockType">
              <el-select
                v-model="form.blockType"
                placeholder="选择板块类型"
                style="width: 100%"
                :disabled="dialogMode === 'edit'"
              >
                <el-option
                  v-for="opt in DISPLAY_BLOCK_TYPE_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="板块标题">
              <el-input v-model="form.blockTitle" placeholder="如：居住环境" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="适用业态">
              <el-checkbox-group v-model="networkTagsArr">
                <el-checkbox v-for="o in NETWORK_TYPE_OPTIONS" :key="o.value" :value="o.value">
                  {{ o.label }}
                </el-checkbox>
              </el-checkbox-group>
              <div class="form-tip">不勾选 = 三种业态详情页全部展示</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="正文内容">
              <RichEditor
                v-model="form.content"
                module="park"
                register-asset
                :asset-park-code="props.parkCode"
                asset-source-type="display_block"
                :asset-source-ref="form.blockType"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="配图">
              <FileUploader
                v-model="imagesArr"
                type="image"
                multiple
                :limit="9"
                module="park"
                register-asset
                :asset-park-code="props.parkCode"
                asset-source-type="display_block"
                :asset-source-ref="form.blockType"
                @update:model-value="onImagesChange"
              />
            </el-form-item>
          </el-col>
          <el-col v-if="imagesArr.length > 0" :span="24">
            <el-form-item label="图片描述">
              <div class="image-desc-list">
                <div v-for="(img, idx) in imagesArr" :key="idx" class="image-desc-item">
                  <el-image
                    :src="formatFileUrl(img)"
                    fit="cover"
                    style="width: 60px; height: 60px; border-radius: 4px; flex-shrink: 0"
                  />
                  <el-input
                    v-model="imageDescsArr[idx]"
                    placeholder="图片描述（选填）"
                    size="small"
                    style="flex: 1"
                  />
                </div>
              </div>
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
.display-pane {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
  .image-desc-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;
  }
  .image-desc-item {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .form-tip {
    font-size: 12px;
    color: #909399;
    line-height: 1.4;
    width: 100%;
  }
}
</style>
