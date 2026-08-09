<script setup lang="ts">
import { nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageRoles,
  createRole,
  updateRole,
  deleteRole,
  getRolePermissions,
  updateRolePermissions
} from '@/api/role'
import { getPermissionTree } from '@/api/permission'
import type { Role, RoleQuery } from '@/types/role'
import { RoleStatus, ROLE_STATUS_OPTIONS, ROLE_TYPE_OPTIONS, DATA_SCOPE_OPTIONS } from '@/types/role'
import type { Permission } from '@/types/permission'

/**
 * 角色管理页。
 *
 * - CRUD 标准模式；
 * - 额外提供「分配权限」弹窗：el-tree 勾选权限，提交 PUT /roles/{roleCode}/permissions。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  Role,
  RoleQuery
>(
  {
    page: pageRoles
  },
  {
    initialQuery: {
      organCode: '',
      roleName: '',
      roleCode: '',
      status: undefined
    }
  }
)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Role>({
  organCode: '',
  roleCode: undefined,
  roleName: '',
  roleType: 2,
  description: '',
  dataScope: 1,
  status: RoleStatus.ENABLED,
  sortOrder: 0,
  permissionCodes: []
})

const rules: FormRules<Role> = {
  organCode: [{ required: true, message: '请输入机构编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    organCode: query.organCode || '',
    roleCode: undefined,
    roleName: '',
    roleType: 2,
    description: '',
    dataScope: 1,
    status: RoleStatus.ENABLED,
    sortOrder: 0,
    permissionCodes: []
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Role) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, {
    organCode: row.organCode,
    roleCode: row.roleCode,
    roleName: row.roleName,
    roleType: row.roleType,
    description: row.description,
    dataScope: row.dataScope,
    status: row.status,
    sortOrder: row.sortOrder,
    permissionCodes: row.permissionCodes ?? []
  })
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
      await createRole(form)
      ElMessage.success('新增成功')
    } else if (form.roleCode) {
      await updateRole(form.roleCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: Role) {
  if (!row.roleCode) return
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteRole(row.roleCode)
  ElMessage.success('删除成功')
  loadPage()
}

function handleReset() {
  query.roleName = ''
  query.roleCode = ''
  query.status = undefined
  query.organCode = ''
  handleSearch()
}

// ---------- 权限分配弹窗 ----------
const permDialogVisible = ref(false)
const permLoading = ref(false)
const permSubmitLoading = ref(false)
const permTreeRef = ref()
const permTreeData = ref<Permission[]>([])
const currentRoleCode = ref('')

/**
 * 归一化后端权限树字段。
 *
 * 后端 /permissions/tree 返回 { name, code, children } 结构（且分组节点
 * permissionCode 为 null），而前端 Permission 类型与 el-tree 默认期望
 * permissionName / permissionCode。这里递归补齐，保证树节点有可读名称、
 * 每个节点都有稳定 key，避免分组节点因 key 为 null 而无法勾选/回显。
 */
type PermissionLike = Partial<Permission> & {
  name?: string
  code?: string
  children?: PermissionLike[]
}

function normalizePermissionTree(nodes: PermissionLike[] | undefined): Permission[] {
  if (!Array.isArray(nodes)) return []
  return nodes.map((n) => {
    const permissionName = n.permissionName || n.name || ''
    const permissionCode = n.permissionCode || n.code || ''
    const childrenRaw = n.children ?? []
    const children = childrenRaw.length > 0 ? normalizePermissionTree(childrenRaw) : undefined
    return {
      ...(n as Permission),
      permissionName,
      permissionCode,
      children
    } as Permission
  })
}

async function openAssignPermission(row: Role) {
  if (!row.roleCode) return
  currentRoleCode.value = row.roleCode
  permDialogVisible.value = true
  permLoading.value = true
  try {
    // 并行加载权限树 + 当前角色已分配权限
    const [tree, checked] = await Promise.all([
      getPermissionTree(),
      getRolePermissions(row.roleCode)
    ])
    // 后端 tree 接口实际返回 name/code 字段（与前端 Permission 类型的
    // permissionName/permissionCode 不一致），此处做一次归一化映射，
    // 让 el-tree 的 label/node-key 能正确命中，同时保留前端类型契约。
    permTreeData.value = normalizePermissionTree(tree as unknown as PermissionLike[])
    // 等待树渲染后回显勾选
    await nextTick()
    permTreeRef.value?.setCheckedKeys(checked, false)
  } finally {
    permLoading.value = false
  }
}

async function handleAssignSubmit() {
  // check-strictly 模式下父子独立勾选，直接取已勾选项即可
  const codes = permTreeRef.value?.getCheckedKeys(false) as string[]

  permSubmitLoading.value = true
  try {
    await updateRolePermissions(currentRoleCode.value, codes)
    ElMessage.success('权限保存成功')
    permDialogVisible.value = false
  } finally {
    permSubmitLoading.value = false
  }
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="机构">
          <el-input v-model="query.organCode" placeholder="机构编码" clearable />
        </el-form-item>
        <el-form-item label="角色名">
          <el-input v-model="query.roleName" placeholder="角色名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="query.roleCode" placeholder="角色编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in ROLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>角色列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增角色</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="roleCode">
        <el-table-column prop="roleCode" label="角色编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleType" label="角色类型" width="110">
          <template #default="{ row }">
            {{ ROLE_TYPE_OPTIONS.find((o) => o.value === row.roleType)?.label ?? row.roleType }}
          </template>
        </el-table-column>
        <el-table-column prop="dataScope" label="数据范围" width="120">
          <template #default="{ row }">
            {{ DATA_SCOPE_OPTIONS.find((o) => o.value === row.dataScope)?.label ?? row.dataScope }}
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openAssignPermission(row)">分配权限</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
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
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增角色' : '编辑角色'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="机构编码" prop="organCode">
              <el-input v-model="form.organCode" placeholder="机构编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="form.roleName" placeholder="角色名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色类型">
              <el-select v-model="form.roleType" placeholder="角色类型" style="width: 100%">
                <el-option v-for="o in ROLE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据范围">
              <el-select v-model="form.dataScope" placeholder="数据范围" style="width: 100%">
                <el-option v-for="o in DATA_SCOPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in ROLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="角色描述" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="480px" :close-on-click-modal="false">
      <div v-loading="permLoading" class="perm-tree-wrap">
        <el-tree
          ref="permTreeRef"
          :data="permTreeData"
          show-checkbox
          node-key="permissionCode"
          :props="{ label: 'permissionName', children: 'children' }"
          default-expand-all
          check-strictly
        />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSubmitLoading" @click="handleAssignSubmit">保存</el-button>
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

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.perm-tree-wrap {
  max-height: 420px;
  overflow: auto;
}
</style>
