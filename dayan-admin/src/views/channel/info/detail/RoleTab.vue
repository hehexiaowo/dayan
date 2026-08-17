<script setup lang="ts">
/**
 * 渠道详情页 - 角色 tab。
 *
 * 分页模式：useCrud（主键 roleCode，传 idKey:'roleCode'，fixedParams:{channelCode}）。
 *
 * 关键约束：
 * - 业务键 roleCode（服务端生成 RL 前缀，渠道内唯一），路径参数用 roleCode。
 * - roleCode/channelCode 不可改；UpdateDTO 不含 channelCode。
 * - roleType 2 态（1系统预置/2自定义），status 2 态（0禁用/1启用）。
 * - 权限分配端点（/permissions）本次不实现，仅做角色 CRUD。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageChannelRoles,
  createChannelRole,
  updateChannelRole,
  deleteChannelRole,
  listAllChannelPermissions,
  getChannelRolePermissions,
  updateChannelRolePermissions,
  type ChannelPermissionOption
} from '@/api/channel-sub'
import { CHANNEL_ROLE_TYPE_OPTIONS, CHANNEL_ROLE_STATUS_OPTIONS } from '@/types/channel'
import type { ChannelRole, ChannelRoleQuery } from '@/types/channel'

const props = defineProps<{
  /** 渠道编码（路由参数） */
  channelCode: string
}>()

// ---------- 角色列表（useCrud，主键 roleCode） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ChannelRole,
  ChannelRoleQuery
>(
  {
    page: pageChannelRoles,
    create: createChannelRole,
    update: (roleCode, data) => updateChannelRole(roleCode, data),
    remove: (roleCode) => deleteChannelRole(roleCode)
  },
  {
    initialQuery: { roleName: '', roleType: undefined, status: undefined },
    idKey: 'roleCode',
    fixedParams: { channelCode: props.channelCode }
  }
)

loadPage()

// ---------- 新增/编辑弹窗 ----------
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
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
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
  form.channelCode = props.channelCode
  dialogVisible.value = true
}

function openEdit(row: ChannelRole) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    channelCode: row.channelCode,
    roleCode: row.roleCode,
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
      // UpdateDTO 不含 channelCode（roleCode 走 path）
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

async function handleDelete(row: ChannelRole) {
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

// ---------- 分配权限 ----------
const permDialogVisible = ref(false)
const permLoading = ref(false)
const permSubmitLoading = ref(false)
const permOptions = ref<ChannelPermissionOption[]>([])
const checkedPermCodes = ref<string[]>([])
const currentRoleCode = ref('')

async function openAssignPerm(row: ChannelRole) {
  if (!row.roleCode) return
  currentRoleCode.value = row.roleCode
  permDialogVisible.value = true
  permLoading.value = true
  try {
    const [all, granted] = await Promise.all([
      listAllChannelPermissions(),
      getChannelRolePermissions(row.roleCode)
    ])
    permOptions.value = all
    checkedPermCodes.value = granted
  } catch {
    permOptions.value = []
    checkedPermCodes.value = []
  } finally {
    permLoading.value = false
  }
}

async function handlePermSubmit() {
  permSubmitLoading.value = true
  try {
    await updateChannelRolePermissions(currentRoleCode.value, checkedPermCodes.value)
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

defineExpose({ loadPage })
</script>

<template>
  <div class="role-tab">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input v-model="query.roleName" placeholder="角色名称" clearable style="width: 160px" @keyup.enter="handleSearch" />
      <el-select v-model="query.roleType" placeholder="类型" clearable style="width: 120px">
        <el-option v-for="o in CHANNEL_ROLE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option v-for="o in CHANNEL_ROLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增角色</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="roleCode">
      <el-table-column prop="roleCode" label="角色编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="roleName" label="角色名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="roleTypeTagType(row.roleType)" size="small">{{ roleTypeLabel(row.roleType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="150" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <!-- 内置角色（roleType=1）全渠道共用：平台仅可分配权限，不可编辑/删除 -->
          <el-button v-if="row.roleType === 1" link type="success" size="small" @click="openAssignPerm(row)">分配权限</el-button>
          <template v-else>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" size="small" @click="openAssignPerm(row)">分配权限</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
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
      :title="dialogMode === 'create' ? '新增角色' : '编辑角色'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属渠道">
              <el-input v-model="form.channelCode" disabled />
            </el-form-item>
          </el-col>
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
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in CHANNEL_ROLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="角色描述" />
            </el-form-item>
          </el-col>
          <!-- 权限分配已移至列表「分配权限」按钮 -->
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="560px" :close-on-click-modal="false">
      <div v-loading="permLoading" class="perm-list-wrap">
        <el-checkbox-group v-model="checkedPermCodes">
          <div v-for="p in permOptions" :key="p.permissionCode" style="padding: 4px 0">
            <el-checkbox :value="p.permissionCode">
              {{ p.permissionName }}（{{ p.permissionCode }}）
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSubmitLoading" @click="handlePermSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.role-tab {
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
}
</style>
