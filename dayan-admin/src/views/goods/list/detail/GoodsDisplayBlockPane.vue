<script setup lang="ts">
/**
 * 商品详情页 - 展示板块（goods_display_block CRUD）。
 *
 * useCrud 仅做分页查询（idKey:'id', fixedParams:{goodsCode}）；增删改由本组件直调 API。
 * 搜索：blockType + status。
 *
 * 核心场景：一个商品 = N 个展示板块（产品介绍/权益详解/服务流程/常见问题/购买须知 等），
 * C/Agent 端详情页按此渲染 tab。每个板块 = 类型 + 标题 + 富文本正文 + 多图上传。
 * images / imageDescriptions 在 DB 存为 JSON 数组字符串（TEXT 列），
 * 前端提交前 JSON.stringify，回显时 JSON.parse。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageGoodsDisplayBlocks,
  createGoodsDisplayBlock,
  updateGoodsDisplayBlock,
  deleteGoodsDisplayBlock
} from '@/api/goods-display'
import type { GoodsDisplayBlock, GoodsDisplayBlockQuery } from '@/types/goods-display'
import { GOODS_DISPLAY_BLOCK_TYPE_OPTIONS, goodsDisplayBlockTypeLabel } from '@/types/goods-display'
import FileUploader from '@/components/FileUploader/index.vue'
import RichEditor from '@/components/RichEditor/index.vue'
import { formatFileUrl } from '@/utils/file'

const props = defineProps<{
  goodsCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  GoodsDisplayBlock,
  GoodsDisplayBlockQuery,
  number
>(
  {
    page: pageGoodsDisplayBlocks
  },
  {
    initialQuery: { blockType: '', status: undefined },
    idKey: 'id',
    fixedParams: { goodsCode: props.goodsCode }
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

const form = reactive({
  id: undefined as number | undefined,
  goodsCode: '',
  blockType: '',
  blockTitle: '',
  content: '',
  images: '',
  imageDescriptions: '',
  sortOrder: 0,
  status: 1
})

const rules: FormRules = {
  blockType: [{ required: true, message: '请选择板块类型', trigger: 'change' }]
}

function resetForm() {
  form.id = undefined
  form.goodsCode = ''
  form.blockType = ''
  form.blockTitle = ''
  form.content = ''
  form.images = ''
  form.imageDescriptions = ''
  form.sortOrder = 0
  form.status = 1
  imagesArr.value = []
  imageDescsArr.value = []
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.goodsCode = props.goodsCode
  dialogVisible.value = true
}

function openEdit(row: GoodsDisplayBlock) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.goodsCode = row.goodsCode || ''
  form.blockType = row.blockType
  form.blockTitle = row.blockTitle || ''
  form.content = row.content || ''
  form.sortOrder = row.sortOrder ?? 0
  form.status = row.status ?? 1
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
    form.images = JSON.stringify(imagesArr.value)
    form.imageDescriptions = JSON.stringify(imageDescsArr.value)
    if (dialogMode.value === 'create') {
      await createGoodsDisplayBlock(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateGoodsDisplayBlock(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: GoodsDisplayBlock) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该展示板块？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteGoodsDisplayBlock(row.id)
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
function firstImage(row: GoodsDisplayBlock): string | null {
  const arr = parseJsonArr(row.images)
  return arr.length > 0 ? arr[0] : null
}
function imageCount(row: GoodsDisplayBlock): number {
  return parseJsonArr(row.images).length
}
function contentPreview(s?: string): string {
  if (!s) return '--'
  return s.replace(/<[^>]+>/g, '').slice(0, 50) + (s.length > 50 ? '...' : '')
}

defineExpose({ loadPage })
</script>

<template>
  <div class="display-block-pane">
    <div class="toolbar">
      <el-select v-model="query.blockType" placeholder="板块类型" clearable style="width: 160px">
        <el-option
          v-for="opt in GOODS_DISPLAY_BLOCK_TYPE_OPTIONS"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="显示" :value="1" />
        <el-option label="隐藏" :value="0" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增板块</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      展示板块用于 C/Agent 端商品详情页：每个板块 = 类型 + 标题 + 正文 + 配图，渲染为详情页的一个 tab。
    </el-alert>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="blockType" label="板块类型" width="130" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ goodsDisplayBlockTypeLabel(row.blockType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="blockTitle" label="板块标题" min-width="150" show-overflow-tooltip />
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
                  v-for="opt in GOODS_DISPLAY_BLOCK_TYPE_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="板块标题">
              <el-input v-model="form.blockTitle" placeholder="如：产品介绍" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="正文内容">
              <RichEditor
                v-model="form.content"
                module="goods"
                register-asset
                asset-ref-type1="goods"
                :asset-ref-code="props.goodsCode"
                asset-ref-type2="display_block"
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
                module="goods"
                register-asset
                asset-ref-type1="goods"
                :asset-ref-code="props.goodsCode"
                asset-ref-type2="display_block"
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
.display-block-pane {
  .toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;

    .toolbar-actions {
      display: flex;
      gap: 8px;
      margin-left: auto;
    }
  }
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
}
</style>
