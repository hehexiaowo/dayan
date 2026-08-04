<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getPermissionTree,
  listAllPermissions,
  createPermission,
  updatePermission,
  deletePermission
} from '@/api/permission'
import {
  PermissionType,
  PERMISSION_TYPE_OPTIONS,
  PERMISSION_STATUS_OPTIONS,
  buildPermissionTree,
  type Permission
} from '@/types/permission'

/**
 * 权限管理页（树形表格）。
 *
 * - 列表用 el-table tree-props 展示层级；
 * - 数据源优先用 /permissions/tree（后端组树），失败/降级则用 /permissions/all + 前端 buildPermissionTree；
 * - 新增时 parentCode 用 tree-select 从平铺全量权限中选父级。
 */

const loading = ref(false)
const treeData = ref<Permission[]>([])
/** 全量平铺权限（供父级选择） */
const flatList = ref<Permission[]>([])

async function loadTree() {
  loading.value = true
  try {
    const [tree, all] = await Promise.all([getPermissionTree(), listAllPermissions()])
    treeData.value = tree
    flatList.value = all
  } catch {
    // 降级：尝试单独拉 tree
    try {
      treeData.value = await getPermissionTree()
    } catch {
      treeData.value = []
    }
  } finally {
    loading.value = false
  }
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Permission>({
  permissionCode: '',
  permissionName: '',
  parentCode: null,
  permissionType: PermissionType.MENU,
  path: '',
  method: '',
  icon: '',
  sortOrder: 0,
  status: 1,
  remark: ''
})

const rules: FormRules = {
  permissionCode: [
    { required: true, message: '请输入权限编码', trigger: 'blur' },
    { max: 100, message: '权限编码长度不能超过 100', trigger: 'blur' }
  ],
  permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  permissionType: [{ required: true, message: '请选择权限类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

/** 供父级选择用的树（基于平铺列表组树） */
const parentTreeOptions = ref<Permission[]>([])

function buildParentOptions(excludeCode?: string): Permission[] {
  const tree = buildPermissionTree(flatList.value)
  // 编辑态排除自身及其子孙，避免循环引用
  if (excludeCode) {
    const filterNode = (nodes: Permission[]): Permission[] => {
      const result: Permission[] = []
      for (const n of nodes) {
        if (n.permissionCode === excludeCode) continue
        const children = n.children ? filterNode(n.children) : undefined
        result.push(children ? { ...n, children } : { ...n, children: undefined })
      }
      return result
    }
    return filterNode(tree)
  }
  return tree
}

function resetForm() {
  Object.assign(form, {
    permissionCode: '',
    permissionName: '',
    parentCode: null,
    permissionType: PermissionType.MENU,
    path: '',
    method: '',
    icon: '',
    sortOrder: 0,
    status: 1,
    remark: ''
  })
}

function openCreate(parent?: Permission) {
  dialogType.value = 'create'
  resetForm()
  if (parent) {
    form.parentCode = parent.permissionCode
    form.permissionType = parent.permissionType
  }
  parentTreeOptions.value = buildParentOptions()
  dialogVisible.value = true
}

function openEdit(row: Permission) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, {
    permissionCode: row.permissionCode,
    permissionName: row.permissionName,
    parentCode: row.parentCode,
    permissionType: row.permissionType,
    path: row.path,
    method: row.method,
    icon: row.icon,
    sortOrder: row.sortOrder,
    status: row.status,
    remark: row.remark
  })
  parentTreeOptions.value = buildParentOptions(row.permissionCode)
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
      await createPermission(form)
      ElMessage.success('新增成功')
    } else {
      await updatePermission(form.permissionCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadTree()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: Permission) {
  await ElMessageBox.confirm(`确定删除权限「${row.permissionName}」吗？若存在子权限将一并影响。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deletePermission(row.permissionCode)
  ElMessage.success('删除成功')
  loadTree()
}

function typeText(t: number) {
  return PERMISSION_TYPE_OPTIONS.find((o) => o.value === t)?.label ?? t
}

function typeTagType(t: number): 'primary' | 'success' | 'warning' {
  if (t === PermissionType.MENU) return 'primary'
  if (t === PermissionType.BUTTON) return 'success'
  return 'warning'
}

// 初始化加载
loadTree()
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>权限列表</span>
          <div>
            <el-button :icon="'Refresh'" @click="loadTree">刷新</el-button>
            <el-button type="primary" :icon="'Plus'" @click="openCreate()">新增权限</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="treeData"
        border
        stripe
        row-key="permissionCode"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        default-expand-all
      >
        <el-table-column prop="permissionName" label="权限名称" min-width="180" />
        <el-table-column prop="permissionCode" label="权限编码" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.permissionType)">{{ typeText(row.permissionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="资源路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="method" label="方法" width="90" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openCreate(row)">新增子级</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增权限' : '编辑权限'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="权限编码" prop="permissionCode">
              <el-input
                v-model="form.permissionCode"
                placeholder="权限编码（如 organ:account:list）"
                :disabled="dialogType === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限名称" prop="permissionName">
              <el-input v-model="form.permissionName" placeholder="权限名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父级权限">
              <el-tree-select
                v-model="form.parentCode"
                :data="parentTreeOptions"
                :props="{ label: 'permissionName', value: 'permissionCode', children: 'children' }"
                check-strictly
                clearable
                placeholder="顶级（不选）"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限类型" prop="permissionType">
              <el-select v-model="form.permissionType" style="width: 100%">
                <el-option v-for="o in PERMISSION_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资源路径">
              <el-input v-model="form.path" placeholder="/admin-api/xxx" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="请求方法">
              <el-select v-model="form.method" clearable placeholder="接口类权限填写" style="width: 100%">
                <el-option label="GET" value="GET" />
                <el-option label="POST" value="POST" />
                <el-option label="PUT" value="PUT" />
                <el-option label="DELETE" value="DELETE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in PERMISSION_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
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
