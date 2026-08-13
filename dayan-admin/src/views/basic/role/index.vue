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
import type { Role, RoleQuery } from '@/types/role'
import { RoleStatus, ROLE_STATUS_OPTIONS, ROLE_TYPE_OPTIONS, DATA_SCOPE_OPTIONS } from '@/types/role'
import { getGrantTree } from '@/api/menu'
import type { GrantTreeNode } from '@/types/permission'
import type { RoleGrants } from '@/types/role'

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
const permTreeData = ref<GrantTreeNode[]>([])
const currentRoleCode = ref('')

/**
 * 计算回显叶子 keys。
 *
 * el-tree 父子联动模式下 setCheckedKeys 父节点会连带全选子节点，
 * 故只回显叶子：PERM 权限叶子 + 没有任何 PERM 子节点的 MENU 叶子；
 * 父级勾选/半选由联动自动计算。
 */
function buildCheckedLeafKeys(tree: GrantTreeNode[], grants: RoleGrants): string[] {
  const keys: string[] = []
  const walk = (nodes: GrantTreeNode[]) => {
    for (const n of nodes) {
      if (n.nodeType === 'PERM') {
        if (grants.permissionCodes.includes(n.nodeKey.slice(5))) keys.push(n.nodeKey)
        continue
      }
      if (n.nodeType === 'MENU') {
        const hasPermChildren = (n.children ?? []).some((c) => c.nodeType === 'PERM')
        if (!hasPermChildren && grants.menuCodes.includes(n.nodeKey.slice(5))) keys.push(n.nodeKey)
      }
      walk(n.children ?? [])
    }
  }
  walk(tree)
  return keys
}

/**
 * 找出无法在树中回显的已授权菜单：树上有 PERM 子节点的菜单只能靠
 * 「其下已授权权限叶子」联动回显；若角色拥有该菜单但该菜单下零权限，
 * 弹窗中它将显示未勾选，直接保存会被静默移除——须显式告知管理员。
 */
function collectUnrenderableMenuNames(tree: GrantTreeNode[], grants: RoleGrants): string[] {
  const names: string[] = []
  const walk = (nodes: GrantTreeNode[]) => {
    for (const n of nodes) {
      if (n.nodeType === 'MENU' && grants.menuCodes.includes(n.nodeKey.slice(5))) {
        const perms = (n.children ?? []).filter((c) => c.nodeType === 'PERM')
        const grantedCount = perms.filter((c) => grants.permissionCodes.includes(c.nodeKey.slice(5))).length
        if (perms.length > 0 && grantedCount === 0) names.push(n.name)
      }
      walk(n.children ?? [])
    }
  }
  walk(tree)
  return names
}

async function openAssignPermission(row: Role) {
  if (!row.roleCode) return
  currentRoleCode.value = row.roleCode
  permDialogVisible.value = true
  permLoading.value = true
  try {
    // 并行加载授权树 + 当前角色已授权（菜单+权限）
    const [tree, grants] = await Promise.all([getGrantTree(), getRolePermissions(row.roleCode)])
    permTreeData.value = tree
    await nextTick()
    permTreeRef.value?.setCheckedKeys(buildCheckedLeafKeys(tree, grants), false)
    const lost = collectUnrenderableMenuNames(tree, grants)
    if (lost.length > 0) {
      ElMessage.warning(`菜单「${lost.join('、')}」下无任何已授权操作权限，无法在树中勾选；直接保存将移除这些菜单的授权`)
    }
  } catch (e) {
    // 树加载失败时关闭弹窗，防止空树状态下误保存清空全部授权
    permDialogVisible.value = false
    throw e
  } finally {
    permLoading.value = false
  }
}

async function handleAssignSubmit() {
  const checked = (permTreeRef.value?.getCheckedKeys(false) ?? []) as string[]
  const half = (permTreeRef.value?.getHalfCheckedKeys() ?? []) as string[]
  // 父子联动：勾了权限的菜单必在 checked/half 中 → 自动入 menu_rel，不会出现"有权限没菜单"；
  // 'group:other' 虚拟组 key 天然被 menu:/perm: 前缀过滤丢弃
  const menuCodes = [...new Set(
    [...checked, ...half].filter((k) => typeof k === 'string' && k.startsWith('menu:')).map((k) => k.slice(5))
  )]
  const permissionCodes = checked
    .filter((k) => typeof k === 'string' && k.startsWith('perm:'))
    .map((k) => k.slice(5))

  permSubmitLoading.value = true
  try {
    await updateRolePermissions(currentRoleCode.value, { menuCodes, permissionCodes })
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
    <el-dialog v-model="permDialogVisible" title="分配权限" width="640px" :close-on-click-modal="false">
      <div v-loading="permLoading" class="perm-tree-wrap">
        <el-tree
          ref="permTreeRef"
          :data="permTreeData"
          show-checkbox
          node-key="nodeKey"
          :props="{ label: 'name', children: 'children' }"
          default-expand-all
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
