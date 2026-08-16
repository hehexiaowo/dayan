<script setup lang="ts">
/**
 * 场景详情页 - 项目明细 tab。
 *
 * 架构（useCrud 分页 + 弹窗 CRUD）：
 * 1. 搜索条（itemName 模糊 + itemType + status）+ 新增按钮
 * 2. 主表格：useCrud（idKey:'id', fixedParams:{sceneCode}）分页加载
 * 3. 新增/编辑 el-dialog：itemCode 必填（同场景唯一），编辑时 disabled（UpdateDTO 不含 itemCode）
 *
 * 红线遵守：
 * - 主键 Long id，useCrud 传 idKey:'id'（number，非业务 code）
 * - itemCode 新增可输入、编辑 disabled
 * - itemType / status 用 el-select + OPTIONS（不用 input-number 显示枚举）
 * - 金额/数量类字段（durationMinutes/sortOrder）才用 el-input-number
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageSceneItems,
  createSceneItem,
  updateSceneItem,
  deleteSceneItem
} from '@/api/scene-sub'
import {
  SCENE_ITEM_TYPE_OPTIONS,
  COMMON_ENABLE_STATUS_OPTIONS
} from '@/types/scene'
import type { SceneItem, SceneItemQuery } from '@/types/scene'

const props = defineProps<{
  /** 场景编码（从详情页 prop 带入，create 表单自动携带） */
  sceneCode: string
}>()

// ---------- 列表（useCrud，主键 id） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  SceneItem,
  SceneItemQuery,
  number
>(
  {
    page: pageSceneItems,
    create: createSceneItem,
    update: (id, data) => updateSceneItem(id, data),
    remove: deleteSceneItem
  },
  {
    initialQuery: { itemName: '', itemType: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { sceneCode: props.sceneCode }
  }
)

loadPage()

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SceneItem>({
  id: undefined,
  sceneCode: '',
  itemCode: '',
  itemName: '',
  itemType: 1,
  itemDescription: '',
  durationMinutes: undefined,
  sortOrder: 0,
  isRequired: 1,
  status: 1
})

const rules: FormRules<SceneItem> = {
  itemCode: [
    { required: true, message: '请输入项目编码', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  itemName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  itemType: [{ required: true, message: '请选择项目类型', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    sceneCode: '',
    itemCode: '',
    itemName: '',
    itemType: 1,
    itemDescription: '',
    durationMinutes: undefined,
    sortOrder: 0,
    isRequired: 1,
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.sceneCode = props.sceneCode
  dialogVisible.value = true
}

function openEdit(row: SceneItem) {
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
      await createSceneItem(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateSceneItem(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: SceneItem) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除项目「${row.itemName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteSceneItem(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function itemTypeLabel(v?: number): string {
  const found = SCENE_ITEM_TYPE_OPTIONS.find((o) => o.value === v)
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
  <div class="item-tab">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input
        v-model="query.itemName"
        placeholder="项目名称"
        clearable
        style="width: 160px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.itemType" placeholder="项目类型" clearable style="width: 140px">
        <el-option v-for="o in SCENE_ITEM_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option v-for="o in COMMON_ENABLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增项目</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="itemCode" label="项目编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="itemName" label="项目名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="itemType" label="类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ itemTypeLabel(row.itemType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" align="center">
        <template #default="{ row }">{{ row.durationMinutes != null ? row.durationMinutes : '--' }}</template>
      </el-table-column>
      <el-table-column prop="isRequired" label="是否必选" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isRequired === 1" type="warning" size="small">必选</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="itemDescription" label="描述" min-width="200" show-overflow-tooltip />
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
      :title="dialogMode === 'create' ? '新增项目' : '编辑项目'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目编码" prop="itemCode">
              <el-input
                v-model="form.itemCode"
                placeholder="业务编码（同场景内唯一）"
                maxlength="50"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目名称" prop="itemName">
              <el-input v-model="form.itemName" placeholder="项目名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类型" prop="itemType">
              <el-select v-model="form.itemType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in SCENE_ITEM_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时长(分钟)">
              <el-input-number v-model="form.durationMinutes" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否必选">
              <el-radio-group v-model="form.isRequired">
                <el-radio :value="1">必选</el-radio>
                <el-radio :value="0">可选</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="o in COMMON_ENABLE_STATUS_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="项目描述">
              <el-input v-model="form.itemDescription" type="textarea" :rows="3" placeholder="项目描述" />
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
</style>
