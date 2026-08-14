<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listContentCategories,
  createContentCategory,
  updateContentCategory,
  deleteContentCategory
} from '@/api/content-sub'
import type { ContentCategory } from '@/types/content'

/**
 * 内容分类管理页（树形表格）。全局资源，独立菜单。
 * 后端 list 返回平铺，前端组树。update 用 query id。
 */

const loading = ref(false)
const flatList = ref<ContentCategory[]>([])
const treeData = ref<ContentCategory[]>([])

/** 平铺 → 树 */
function buildTree(list: ContentCategory[], parentCode?: string): ContentCategory[] {
  const nodes = list
    .filter((n) => (parentCode ? n.parentCode === parentCode : !n.parentCode))
    .map((n) => ({ ...n }))
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
  for (const n of nodes) {
    const children = buildTree(list, n.categoryCode)
    if (children.length) n.children = children
  }
  return nodes
}

async function loadTree() {
  loading.value = true
  try {
    flatList.value = await listContentCategories()
    treeData.value = buildTree(flatList.value)
  } finally {
    loading.value = false
  }
}

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ContentCategory>({
  categoryName: '',
  parentCode: undefined,
  categoryType: undefined,
  icon: '',
  coverImage: '',
  description: '',
  sortOrder: 0,
  isVisible: 1,
  status: 1
})

const rules: FormRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

/** 父级树选项（新增/编辑时排除自身及其子树） */
const parentOptions = ref<ContentCategory[]>([])
function buildParentOptions(excludeCode?: string): ContentCategory[] {
  const filterNode = (nodes: ContentCategory[]): ContentCategory[] => {
    const result: ContentCategory[] = []
    for (const n of nodes) {
      if (n.categoryCode === excludeCode) continue
      const children = n.children ? filterNode(n.children) : undefined
      result.push(children ? { ...n, children } : { ...n, children: undefined })
    }
    return result
  }
  return filterNode(buildTree(flatList.value))
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    categoryCode: undefined,
    categoryName: '',
    parentCode: undefined,
    categoryType: undefined,
    icon: '',
    coverImage: '',
    description: '',
    sortOrder: 0,
    isVisible: 1,
    status: 1
  })
}

function openCreate(parent?: ContentCategory) {
  dialogType.value = 'create'
  resetForm()
  if (parent) form.parentCode = parent.categoryCode
  parentOptions.value = buildParentOptions()
  dialogVisible.value = true
}

function openEdit(row: ContentCategory) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, row)
  parentOptions.value = buildParentOptions(row.categoryCode)
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
      await createContentCategory(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateContentCategory(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadTree()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ContentCategory) {
  await ElMessageBox.confirm(`确定删除分类「${row.categoryName}」吗？子分类将一并影响。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  if (!row.id) return
  await deleteContentCategory(row.id)
  ElMessage.success('删除成功')
  loadTree()
}

onMounted(loadTree)
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>内容分类</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate()">新增分类</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="treeData"
        border
        stripe
        row-key="categoryCode"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="categoryName" label="分类名称" min-width="200" />
        <el-table-column prop="categoryCode" label="分类编码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="contentCount" label="内容数" width="90" align="center" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="可见" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isVisible === 1 ? 'success' : 'info'">{{ row.isVisible === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openCreate(row)">新增子级</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增分类' : '编辑分类'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类名称" prop="categoryName">
              <el-input v-model="form.categoryName" placeholder="分类名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父级分类">
              <el-tree-select
                v-model="form.parentCode"
                :data="parentOptions"
                :props="{ label: 'categoryName', value: 'categoryCode', children: 'children' }"
                check-strictly
                clearable
                placeholder="顶级（不选）"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否可见">
              <el-switch :model-value="form.isVisible === 1" @change="(v: boolean) => (form.isVisible = v ? 1 : 0)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标">
              <el-input v-model="form.icon" placeholder="图标（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="分类描述" />
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

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
