<script setup lang="ts">
/**
 * 场景详情页 - 所需资源 tab。
 *
 * 架构（useCrud 分页 + 弹窗 CRUD）：
 * 1. 搜索条（resourceName + resourceType + status）+ 新增按钮
 * 2. 主表格：useCrud（idKey:'id', fixedParams:{sceneCode}）分页加载
 * 3. 新增/编辑 el-dialog：resourceName 必填，resourceType/status 用 el-select + OPTIONS
 *
 * 红线遵守：
 * - 主键 Long id，useCrud 传 idKey:'id'
 * - resourceType / status 用 el-select + OPTIONS
 * - unitCost 是金额字段：el-input-number，min=0、precision=2
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageSceneResources,
  createSceneResource,
  updateSceneResource,
  deleteSceneResource
} from '@/api/scene-sub'
import {
  SCENE_RESOURCE_TYPE_OPTIONS,
  COMMON_ENABLE_STATUS_OPTIONS
} from '@/types/scene'
import type { SceneResource, SceneResourceQuery } from '@/types/scene'

const props = defineProps<{
  /** 场景编码（从详情页 prop 带入，create 表单自动携带） */
  sceneCode: string
}>()

// ---------- 列表（useCrud，主键 id） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  SceneResource,
  SceneResourceQuery,
  number
>(
  {
    page: pageSceneResources,
    create: createSceneResource,
    update: (id, data) => updateSceneResource(id, data),
    remove: deleteSceneResource
  },
  {
    initialQuery: { resourceName: '', resourceType: undefined, status: undefined },
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

const form = reactive<SceneResource>({
  id: undefined,
  sceneCode: '',
  resourceType: 1,
  resourceName: '',
  resourceDescription: '',
  quantity: undefined,
  unit: '',
  unitCost: undefined,
  isProvided: 0,
  sortOrder: 0,
  status: 1
})

const rules: FormRules<SceneResource> = {
  resourceName: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
  resourceType: [{ required: true, message: '请选择资源类型', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    sceneCode: '',
    resourceType: 1,
    resourceName: '',
    resourceDescription: '',
    quantity: undefined,
    unit: '',
    unitCost: undefined,
    isProvided: 0,
    sortOrder: 0,
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.sceneCode = props.sceneCode
  dialogVisible.value = true
}

function openEdit(row: SceneResource) {
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
      await createSceneResource(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateSceneResource(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: SceneResource) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除资源「${row.resourceName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteSceneResource(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function resourceTypeLabel(v?: number): string {
  const found = SCENE_RESOURCE_TYPE_OPTIONS.find((o) => o.value === v)
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
  <div class="resource-tab">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input
        v-model="query.resourceName"
        placeholder="资源名称"
        clearable
        style="width: 160px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.resourceType" placeholder="资源类型" clearable style="width: 140px">
        <el-option v-for="o in SCENE_RESOURCE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option v-for="o in COMMON_ENABLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增资源</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="resourceName" label="资源名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="resourceType" label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ resourceTypeLabel(row.resourceType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="90" align="center">
        <template #default="{ row }">{{ row.quantity != null ? row.quantity : '--' }}</template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="90" align="center">
        <template #default="{ row }">{{ row.unit || '--' }}</template>
      </el-table-column>
      <el-table-column prop="unitCost" label="单位成本" width="110" align="right">
        <template #default="{ row }">{{ row.unitCost != null ? row.unitCost : '--' }}</template>
      </el-table-column>
      <el-table-column prop="isProvided" label="是否提供" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isProvided === 1" type="success" size="small">机构提供</el-tag>
          <el-tag v-else type="info" size="small">需自备</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="resourceDescription" label="描述" min-width="200" show-overflow-tooltip />
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
      :title="dialogMode === 'create' ? '新增资源' : '编辑资源'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="资源名称" prop="resourceName">
              <el-input v-model="form.resourceName" placeholder="资源名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资源类型" prop="resourceType">
              <el-select v-model="form.resourceType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in SCENE_RESOURCE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数量">
              <el-input-number v-model="form.quantity" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位">
              <el-input v-model="form.unit" placeholder="如 把/份/小时" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位成本">
              <el-input-number
                v-model="form.unitCost"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否提供">
              <el-radio-group v-model="form.isProvided">
                <el-radio :value="1">机构提供</el-radio>
                <el-radio :value="0">需自备</el-radio>
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
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="资源描述">
              <el-input v-model="form.resourceDescription" type="textarea" :rows="3" placeholder="资源描述" />
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
