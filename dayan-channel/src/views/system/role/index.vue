<script setup lang="ts">
/**
 * 渠道端 - 角色管理页。
 *
 * - 列表/增/删/改：useCrud 分页 + 直接调 createChannelRole/updateChannelRole/deleteChannelRole；
 * - 权限分配弹窗：el-tree 勾选权限，提交 PUT /channel-roles/{roleCode}/permissions（全量覆盖）。
 *
 * 关键约束：
 * - 业务键 roleCode（服务端生成 RL 前缀，渠道内唯一），路径参数用 roleCode；
 * - 本渠道角色由后端按 ContextHolder.channelCode 隔离，前端查询不传 channelCode；
 * - createChannelRole DTO 需带 channelCode，从 useUserStore().userInfo.channelCode 取；
 * - update 时 channelCode/roleCode 不可改（roleCode 走 path，channelCode 不传）；
 * - 权限授权提交 checked + halfChecked（父级权限码也要分配，权限码是扁平的）。
 */
import { nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageChannelRoles,
  createChannelRole,
  updateChannelRole,
  deleteChannelRole,
  getChannelRolePermissions,
  assignChannelRolePermissions,
  getChannelPermissionTree
} from '@/api/channel-sub'
import { useUserStore } from '@/stores/user'
import {
  CHANNEL_ROLE_TYPE_OPTIONS,
  CHANNEL_ROLE_STATUS_OPTIONS
} from '@/types/channel'
import type { ChannelRole, ChannelRoleQuery, ChannelPermission } from '@/types/channel'

const userStore = useUserStore()

// ---------- 角色列表（useCrud 分页；本渠道角色由后端按 channelCode 隔离，前端无需传） ----------
const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<ChannelRole, ChannelRoleQuery>(
  { page: pageChannelRoles },
  {
    initialQuery: { roleName: '', roleType: undefined, status: undefined }
  }
)

loadPage()

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ChannelRole>({
  id: undefined,
  channelCode: '',
  roleCode: '',
  roleName: '',
  roleType: 2,
  description: '',
  status: 1,
  sortOrder: 0
})

const rules: FormRules<ChannelRole> = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    channelCode: '',
    roleCode: '',
    roleName: '',
    roleType: 2,
    description: '',
    status: 1,
    sortOrder: 0
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  // 新增 DTO 需带 channelCode，从当前登录信息取
  form.channelCode = userStore.userInfo?.channelCode ?? ''
  dialogVisible.value = true
}

function openEdit(row: ChannelRole) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    channelCode: row.channelCode,
    roleCode: row.roleCode ?? '',
    roleName: row.roleName ?? '',
    roleType: row.roleType ?? 2,
    description: row.description ?? '',
    status: row.status ?? 1,
    sortOrder: row.sortOrder ?? 0
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
    if (dialogMode.value === 'create') {
      await createChannelRole(form)
      ElMessage.success('新增成功')
    } else if (form.roleCode) {
      // UpdateDTO 不含 channelCode（roleCode 走 path，channelCode 不可改）
      const { channelCode: _c, ...rest } = form
      void _c
      await updateChannelRole(form.roleCode, rest)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: ChannelRole) {
  if (!row.roleCode) return
  await ElMessageBox.confirm(`确定删除角色「${row.roleName || row.roleCode}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteChannelRole(row.roleCode)
  ElMessage.success('删除成功')
  loadPage()
}

function handleReset() {
  query.roleName = ''
  query.roleType = undefined
  query.status = undefined
  handleSearch()
}

// ---------- 分配权限弹窗（重点，参考 admin basic/role） ----------
const permDialogVisible = ref(false)
const permLoading = ref(false)
const permSubmitLoading = ref(false)
const permTreeRef = ref()
const permTreeData = ref<ChannelPermission[]>([])
const currentRoleCode = ref('')

/**
 * 归一化后端权限树字段。
 *
 * 前端 ChannelPermission 类型约定字段为 permissionName / permissionCode，
 * 后端 /channel-permissions/tree 大概率已对齐；但若后端返回 name/code 字段
 * （或分组节点 permissionCode 为 null），这里递归补齐，保证树节点有可读名称、
 * 每个节点都有稳定 key，避免勾选/回显失败。实现兼容，以实际返回为准。
 */
type PermissionLike = Partial<ChannelPermission> & {
  name?: string
  code?: string
  children?: PermissionLike[]
}

function normalizePermissionTree(nodes: PermissionLike[] | undefined): ChannelPermission[] {
  if (!Array.isArray(nodes)) return []
  return nodes.map((n) => {
    const permissionName = n.permissionName || n.name || ''
    const permissionCode = n.permissionCode || n.code || ''
    const childrenRaw = n.children ?? []
    const children = childrenRaw.length > 0 ? normalizePermissionTree(childrenRaw) : undefined
    return {
      ...(n as ChannelPermission),
      permissionName,
      permissionCode,
      children
    } as ChannelPermission
  })
}

async function openAssignPermission(row: ChannelRole) {
  if (!row.roleCode) return
  currentRoleCode.value = row.roleCode
  permDialogVisible.value = true
  permLoading.value = true
  try {
    // 并行加载权限树 + 当前角色已分配权限
    const [tree, checked] = await Promise.all([
      getChannelPermissionTree(),
      getChannelRolePermissions(row.roleCode)
    ])
    // 兼容后端返回 name/code 的情况
    permTreeData.value = normalizePermissionTree(tree as unknown as PermissionLike[])
    // 等待树渲染后回显勾选（check-strictly 模式下 false=不联动）
    await nextTick()
    permTreeRef.value?.setCheckedKeys(checked, false)
  } finally {
    permLoading.value = false
  }
}

async function handleAssignSubmit() {
  const checkedKeys = permTreeRef.value?.getCheckedKeys(false) as string[]
  const halfCheckedKeys = permTreeRef.value?.getHalfCheckedKeys() as string[]
  // 半选（父级）一并提交：权限码是扁平的，父级权限码也要分配
  const codes = Array.from(new Set([...(checkedKeys ?? []), ...(halfCheckedKeys ?? [])]))

  permSubmitLoading.value = true
  try {
    await assignChannelRolePermissions(currentRoleCode.value, codes)
    ElMessage.success('权限保存成功')
    permDialogVisible.value = false
  } finally {
    permSubmitLoading.value = false
  }
}

// ---------- 辅助渲染 ----------
function roleTypeLabel(v?: number): string {
  const found = CHANNEL_ROLE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function roleTypeTagType(v?: number): '' | 'info' {
  return v === 1 ? 'info' : ''
}

function statusLabel(v?: number): string {
  const found = CHANNEL_ROLE_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}

function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="角色名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.roleType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in CHANNEL_ROLE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in CHANNEL_ROLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
        <el-table-column prop="roleName" label="角色名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTypeTagType(row.roleType)" size="small">{{ roleTypeLabel(row.roleType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="150" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openAssignPermission(row)">分配权限</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
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
      :title="dialogMode === 'create' ? '新增角色' : '编辑角色'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col v-if="dialogMode === 'edit'" :span="12">
            <el-form-item label="角色编码">
              <el-input v-model="form.roleCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="form.roleName" placeholder="角色名称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色类型">
              <el-select v-model="form.roleType" style="width: 100%">
                <el-option v-for="o in CHANNEL_ROLE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in CHANNEL_ROLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色描述">
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
