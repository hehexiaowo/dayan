<script setup lang="ts">
/**
 * 场景详情页 - 价格档位 tab。
 *
 * 架构（useCrud 分页 + 弹窗 CRUD）：
 * 1. 搜索条（priceType + status）+ 新增按钮
 * 2. 主表格：useCrud（idKey:'id', fixedParams:{sceneCode}）分页加载
 * 3. 新增/编辑 el-dialog：
 *    - sceneItemCode 必填，用 el-select 列出当前场景的 items（onMounted 调 listSceneItems 拉取）
 *    - 编辑时 sceneItemCode disabled（外键不可改）
 *    - originalPrice/salePrice/channelPrice 全部 el-input-number，min=0、precision=2
 *
 * 红线遵守：
 * - 主键 Long id，useCrud 传 idKey:'id'
 * - sceneItemCode 新增可选，编辑 disabled
 * - priceType / status 用 el-select + OPTIONS
 * - 金额字段 precision=2、min=0
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageSceneItemPrices,
  createSceneItemPrice,
  updateSceneItemPrice,
  deleteSceneItemPrice,
  listSceneItems
} from '@/api/scene-sub'
import {
  SCENE_PRICE_TYPE_OPTIONS,
  COMMON_ENABLE_STATUS_OPTIONS
} from '@/types/scene'
import type { SceneItemPrice, SceneItemPriceQuery, SceneItem } from '@/types/scene'

const props = defineProps<{
  /** 场景编码（从详情页 prop 带入，create 表单自动携带） */
  sceneCode: string
}>()

// ---------- 列表（useCrud，主键 id） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  SceneItemPrice,
  SceneItemPriceQuery,
  number
>(
  {
    page: pageSceneItemPrices,
    create: createSceneItemPrice,
    update: (id, data) => updateSceneItemPrice(id, data),
    remove: deleteSceneItemPrice
  },
  {
    initialQuery: { sceneItemCode: '', priceType: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { sceneCode: props.sceneCode }
  }
)

loadPage()

// ---------- 当前场景的 items（用于 sceneItemCode el-select 选项） ----------
const itemOptions = ref<SceneItem[]>([])
const itemOptionsLoading = ref(false)

async function loadItemOptions() {
  if (!props.sceneCode) return
  itemOptionsLoading.value = true
  try {
    itemOptions.value = await listSceneItems(props.sceneCode)
  } catch {
    itemOptions.value = []
  } finally {
    itemOptionsLoading.value = false
  }
}

onMounted(loadItemOptions)

/** 把 sceneItemCode 渲染成 itemCode + itemName（便于在表格/只读时辨识） */
function itemCodeLabel(code?: string): string {
  if (!code) return '--'
  const found = itemOptions.value.find((i) => i.itemCode === code)
  return found ? `${found.itemName}（${code}）` : code
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SceneItemPrice>({
  id: undefined,
  sceneCode: '',
  sceneItemCode: '',
  priceType: 1,
  originalPrice: undefined,
  salePrice: undefined,
  channelPrice: undefined,
  priceDescription: '',
  effectiveDate: '',
  expireDate: '',
  status: 1
})

const rules: FormRules<SceneItemPrice> = {
  sceneItemCode: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  priceType: [{ required: true, message: '请选择定价类型', trigger: 'change' }],
  originalPrice: [{ required: true, message: '请输入原价', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    sceneCode: '',
    sceneItemCode: '',
    priceType: 1,
    originalPrice: undefined,
    salePrice: undefined,
    channelPrice: undefined,
    priceDescription: '',
    effectiveDate: '',
    expireDate: '',
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.sceneCode = props.sceneCode
  dialogVisible.value = true
}

function openEdit(row: SceneItemPrice) {
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
      await createSceneItemPrice(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateSceneItemPrice(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: SceneItemPrice) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该价格记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteSceneItemPrice(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function priceTypeLabel(v?: number): string {
  const found = SCENE_PRICE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusLabel(v?: number): string {
  const found = COMMON_ENABLE_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
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
  <div class="price-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="定价类型">
        <el-select v-model="query.priceType" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in SCENE_PRICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in COMMON_ENABLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增价格</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column label="关联项目" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ itemCodeLabel(row.sceneItemCode) }}</template>
      </el-table-column>
      <el-table-column prop="priceType" label="定价类型" width="100" align="center">
        <template #default="{ row }">{{ priceTypeLabel(row.priceType) }}</template>
      </el-table-column>
      <el-table-column prop="originalPrice" label="原价" width="100" align="right" />
      <el-table-column prop="salePrice" label="售价" width="100" align="right" />
      <el-table-column prop="channelPrice" label="渠道价" width="100" align="right">
        <template #default="{ row }">{{ row.channelPrice != null ? row.channelPrice : '--' }}</template>
      </el-table-column>
      <el-table-column prop="effectiveDate" label="生效日期" width="110" align="center">
        <template #default="{ row }">{{ formatDate(row.effectiveDate) }}</template>
      </el-table-column>
      <el-table-column prop="expireDate" label="失效日期" width="110" align="center">
        <template #default="{ row }">{{ formatDate(row.expireDate) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="priceDescription" label="价格说明" min-width="200" show-overflow-tooltip />
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增价格' : '编辑价格'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="关联项目" prop="sceneItemCode">
              <el-select
                v-model="form.sceneItemCode"
                placeholder="请选择当前场景下的项目"
                filterable
                :disabled="dialogMode === 'edit'"
                :loading="itemOptionsLoading"
                style="width: 100%"
              >
                <el-option
                  v-for="it in itemOptions"
                  :key="it.itemCode"
                  :label="`${it.itemName}（${it.itemCode}）`"
                  :value="it.itemCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="定价类型" prop="priceType">
              <el-select v-model="form.priceType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in SCENE_PRICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="o in COMMON_ENABLE_STATUS_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价" prop="originalPrice">
              <el-input-number
                v-model="form.originalPrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价" prop="salePrice">
              <el-input-number
                v-model="form.salePrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="渠道价">
              <el-input-number
                v-model="form.channelPrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生效日期">
              <el-date-picker
                v-model="form.effectiveDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="失效日期">
              <el-date-picker
                v-model="form.expireDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="价格说明">
              <el-input v-model="form.priceDescription" type="textarea" :rows="2" placeholder="价格说明" />
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

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
