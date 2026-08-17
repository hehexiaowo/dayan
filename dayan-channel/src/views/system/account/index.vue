<script setup lang="ts">
/**
 * 账号管理页（渠道端）。
 *
 * CRUD 本渠道（及子渠道）账号 + 重置密码 + 分配角色。
 *
 * - useCrud：主键 accountCode，仅注入 page（增删改均在本页直接调用 API，便于细控）。
 * - 搜索栏：username / realName / phone（后端 QueryDTO 暂未含，作为透传 query 参数）/ accountStatus；
 * - el-table 列：accountCode / username / realName / phone / isAdmin / accountStatus / lastLoginTime / 操作；
 * - 新增弹窗：channelCode（从 getChannelInfoTree 取渠道下拉）/ username / password / realName /
 *   phone / email / position / isAdmin / accountStatus；
 * - 编辑弹窗：密码字段隐藏（密码留空不改，与 admin basic/account 一致），username 不可改，
 *   其余字段可改；
 * - 操作列：编辑 / 删除（二次确认） / 重置密码（二次确认） / 分配角色（el-checkbox-group，
 *   选项来自 pageChannelRoles({current:1,size:999})，回显 getChannelAccountRoles，
 *   提交 assignChannelAccountRoles）。
 *
 * 字段约束对齐后端 ChannelAccountVO / ChannelAccountQueryDTO：
 * - accountStatus 3 态（0锁定 / 1正常 / 2禁用）；
 * - isAdmin 2 态（0否 / 1是）。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageChannelAccounts,
  createChannelAccount,
  updateChannelAccount,
  deleteChannelAccount,
  resetChannelAccountPassword,
  getChannelAccountRoles,
  assignChannelAccountRoles,
  getChannelInfoTree,
  pageChannelRoles
} from '@/api/channel-sub'
import type {
  ChannelAccount,
  ChannelAccountQuery,
  ChannelInfo,
  ChannelRole
} from '@/types/channel'
import { CHANNEL_ACCOUNT_STATUS_OPTIONS, CHANNEL_IS_ADMIN_OPTIONS } from '@/types/channel'
import { formatDateTime } from '@/utils/format'

/**
 * 本页使用的查询参数类型。
 *
 * 在后端 ChannelAccountQueryDTO 基础上额外保留 phone（后端可能用于过滤，前端透传）。
 */
type AccountQueryLocal = ChannelAccountQuery & { phone?: string }

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<ChannelAccount, AccountQueryLocal>(
    { page: pageChannelAccounts },
    {
      initialQuery: {
        username: '',
        realName: '',
        phone: '',
        accountStatus: undefined
      }
    }
  )

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/**
 * 账号表单（编辑时不含 password；新增时 password 必填）。
 *
 * username 编辑态不可改；编辑态隐藏密码字段，密码走"重置密码"按钮。
 */
const form = reactive<{
  channelCode: string
  accountCode?: string
  username: string
  password?: string
  realName?: string
  phone?: string
  email?: string
  position?: string
  isAdmin: number
  accountStatus: number
}>({
  channelCode: '',
  accountCode: undefined,
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  position: '',
  isAdmin: 0,
  accountStatus: 1
})

const rules: FormRules<typeof form> = {
  channelCode: [{ required: true, message: '请选择所属渠道', trigger: 'change' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { max: 64, message: '用户名长度不能超过 64 位', trigger: 'blur' }
  ],
  password: [
    {
      validator: (_rule, value: string, callback) => {
        if (dialogMode.value === 'create' && !value) {
          callback(new Error('请输入密码'))
        } else if (value && (value.length < 6 || value.length > 64)) {
          callback(new Error('密码长度 6-64 位'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '手机号格式不正确',
      trigger: 'blur'
    }
  ],
  email: [
    {
      pattern: /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/,
      message: '邮箱格式不正确',
      trigger: 'blur'
    }
  ]
}

function resetForm() {
  Object.assign(form, {
    channelCode: '',
    accountCode: undefined,
    username: '',
    password: '',
    realName: '',
    phone: '',
    email: '',
    position: '',
    isAdmin: 0,
    accountStatus: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ChannelAccount) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    channelCode: row.channelCode,
    accountCode: row.accountCode,
    username: row.username,
    realName: row.realName,
    phone: row.phone,
    email: row.email,
    position: row.position,
    isAdmin: row.isAdmin ?? 0,
    accountStatus: row.accountStatus ?? 1
  })
  // 编辑态密码字段隐藏：不展示、不提交（走"重置密码"按钮）
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
      await createChannelAccount(form)
      ElMessage.success('新增成功')
    } else if (form.accountCode) {
      // 编辑态：剔除 password，且 username/accountCode/channelCode 后端不会更新
      const { password: _p, username: _u, accountCode: _a, channelCode: _c, ...rest } = form
      void _p
      void _u
      void _a
      void _c
      await updateChannelAccount(form.accountCode, rest)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: ChannelAccount) {
  if (!row.accountCode) return
  await ElMessageBox.confirm(`确定删除账号「${row.username || row.accountCode}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteChannelAccount(row.accountCode)
  ElMessage.success('删除成功')
  loadPage()
}

/** 重置密码（二次确认 → 调用 resetChannelAccountPassword） */
async function handleResetPassword(row: ChannelAccount) {
  if (!row.accountCode) return
  await ElMessageBox.confirm(
    `确定将账号「${row.username || row.accountCode}」的密码重置为默认密码吗？`,
    '重置密码',
    {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await resetChannelAccountPassword(row.accountCode)
  ElMessage.success('密码已重置')
}

// ---------- 分配角色弹窗 ----------
const roleDialogVisible = ref(false)
const roleLoading = ref(false)
const roleSubmitLoading = ref(false)
/** 本渠道全量角色列表（来自 pageChannelRoles） */
const roleOptions = ref<ChannelRole[]>([])
/** 当前已勾选的角色编码集合 */
const checkedRoleCodes = ref<string[]>([])
/** 当前分配角色的目标账号编码（提交时回填） */
const roleTargetAccountCode = ref<string>('')

/**
 * 打开分配角色弹窗：
 * 1. 并行拉取本渠道全量角色 + 当前账号已分配角色编码；
 * 2. 用 checkbox-group 回显已有角色。
 */
async function openAssignRoles(row: ChannelAccount) {
  if (!row.accountCode) return
  roleTargetAccountCode.value = row.accountCode
  roleDialogVisible.value = true
  roleLoading.value = true
  checkedRoleCodes.value = []
  try {
    const [rolePage, assigned] = await Promise.all([
      pageChannelRoles({ current: 1, size: 999 }),
      getChannelAccountRoles(row.accountCode)
    ])
    roleOptions.value = rolePage.records
    checkedRoleCodes.value = Array.isArray(assigned) ? assigned : []
  } finally {
    roleLoading.value = false
  }
}

/** 提交分配（全量覆盖） */
async function handleAssignRolesSubmit() {
  if (!roleTargetAccountCode.value) return
  roleSubmitLoading.value = true
  try {
    await assignChannelAccountRoles(roleTargetAccountCode.value, [...checkedRoleCodes.value])
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
  } finally {
    roleSubmitLoading.value = false
  }
}

// ---------- 渠道下拉选项 ----------
/** 所属渠道下拉（从 getChannelInfoTree 取扁平化渠道列表） */
const channelOptions = ref<ChannelInfo[]>([])

async function loadChannelOptions() {
  try {
    channelOptions.value = (await getChannelInfoTree()) ?? []
  } catch (err) {
    // 接口异常时降级为空列表，不阻塞主页面
    console.warn('[account] 加载渠道下拉失败:', err)
    channelOptions.value = []
  }
}

// ---------- 渲染辅助 ----------
function accountStatusLabel(v?: number): string {
  const found = CHANNEL_ACCOUNT_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function accountStatusTagType(v?: number): 'success' | 'info' | 'warning' {
  if (v === 1) return 'success'
  if (v === 2) return 'info'
  if (v === 0) return 'warning'
  return 'info'
}

// ---------- 搜索重置 ----------
function handleReset() {
  query.username = ''
  query.realName = ''
  query.phone = ''
  query.accountStatus = undefined
  handleSearch()
}

onMounted(() => {
  loadChannelOptions()
  loadPage()
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input v-model="query.username" placeholder="用户名" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-input v-model="query.realName" placeholder="真实姓名" clearable style="width: 150px" @keyup.enter="handleSearch" />
        <el-input v-model="query.phone" placeholder="手机号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-select v-model="query.accountStatus" placeholder="状态" clearable style="width: 120px">
          <el-option
            v-for="o in CHANNEL_ACCOUNT_STATUS_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">账号列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增账号</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="accountCode">
        <el-table-column prop="accountCode" label="账号编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机" min-width="120" />
        <el-table-column label="是否超管" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isAdmin === 1" type="success" size="small">是</el-tag>
            <el-tag v-else type="info" size="small">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="accountStatusTagType(row.accountStatus)" size="small">
              {{ accountStatusLabel(row.accountStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="openAssignRoles(row)">分配角色</el-button>
            <el-button link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增账号' : '编辑账号'"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属渠道" prop="channelCode">
              <el-select
                v-model="form.channelCode"
                placeholder="请选择所属渠道"
                filterable
                style="width: 100%"
                :disabled="dialogMode === 'edit'"
              >
                <el-option
                  v-for="ch in channelOptions"
                  :key="ch.channelCode"
                  :label="ch.fullName"
                  :value="ch.channelCode!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="12">
            <el-form-item label="账号编码">
              <el-input v-model="form.accountCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="form.username"
                placeholder="登录用户名"
                maxlength="64"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'create'" :span="12">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                placeholder="请输入密码（6-64 位）"
                maxlength="64"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="真实姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="邮箱" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位" prop="position">
              <el-input v-model="form.position" placeholder="职位" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否超管">
              <el-select v-model="form.isAdmin" style="width: 100%">
                <el-option
                  v-for="o in CHANNEL_IS_ADMIN_OPTIONS"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-select v-model="form.accountStatus" style="width: 100%">
                <el-option
                  v-for="o in CHANNEL_ACCOUNT_STATUS_OPTIONS"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="24">
            <el-alert
              type="info"
              :closable="false"
              title="用户名不可改；密码请使用操作列的【重置密码】按钮。"
              show-icon
            />
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="560px" :close-on-click-modal="false">
      <div v-loading="roleLoading" class="role-dialog-body">
        <el-checkbox-group v-model="checkedRoleCodes">
          <el-checkbox
            v-for="role in roleOptions"
            :key="role.roleCode"
            :label="role.roleName"
            :value="role.roleCode"
          />
        </el-checkbox-group>
        <el-empty v-if="!roleLoading && roleOptions.length === 0" description="暂无可分配角色" />
      </div>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitLoading" @click="handleAssignRolesSubmit">
          保存
        </el-button>
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

.role-dialog-body {
  min-height: 80px;
  max-height: 360px;
  overflow-y: auto;

  .el-checkbox {
    display: flex;
    margin-bottom: 8px;
  }
}
</style>
