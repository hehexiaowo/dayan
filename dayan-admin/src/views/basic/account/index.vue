<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageAccounts,
  createAccount,
  updateAccount,
  deleteAccount,
  resetPassword,
  switchAccountStatus,
  getAccountRoles,
  assignAccountRoles
} from '@/api/account'
import { listAllOrgans } from '@/api/organ'
import { pageRoles } from '@/api/role'
import type { Account, AccountQuery } from '@/types/account'
import type { OrganSimple } from '@/types/organ'
import type { Role } from '@/types/role'
import { AccountStatus, ACCOUNT_STATUS_OPTIONS, GENDER_OPTIONS } from '@/types/account'
import { formatDateTime } from '@/utils/format'
import FileUploader from '@/components/FileUploader/index.vue'

/**
 * 账号管理页。
 *
 * - 列表不展示 password / salt / idCard / openId / unionId（后端详情亦不返回）；
 * - create 表单 password 必填；update 表单 password 留空表示不修改；
 * - organCode 为可选过滤参数：留空时后端返回全部机构账号（超管视角），
 *   填入具体机构编码则仅查该机构。新增账号时 organCode 必填（需归属某机构）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  Account,
  AccountQuery
>(
  {
    page: pageAccounts
  },
  {
    initialQuery: {
      organCode: '',
      username: '',
      realName: '',
      accountStatus: undefined
    }
  }
)

/** 当前是否处于"新增"模式（否则为"编辑"） */
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 表单数据（含 password 字段，仅在 create 必填） */
const form = reactive<Account>({
  organCode: '',
  accountCode: undefined,
  username: '',
  password: '',
  realName: '',
  gender: 0,
  phone: '',
  email: '',
  avatar: '',
  accountStatus: AccountStatus.NORMAL,
  isAdmin: 0,
  remark: '',
  roleCodes: []
})

/** 机构下拉选项（/organs/all） */
const organOptions = ref<OrganSimple[]>([])
/** 当前机构下可选角色（随 form.organCode 变化重载） */
const roleOptions = ref<Role[]>([])

async function loadOrgans() {
  try {
    organOptions.value = await listAllOrgans()
  } catch {
    organOptions.value = []
  }
}

/** 加载某机构下的角色列表（供表单多选） */
async function loadRoles(organCode: string) {
  if (!organCode) {
    roleOptions.value = []
    return
  }
  try {
    const res = await pageRoles({ organCode, current: 1, size: 1000 })
    roleOptions.value = res.records
  } catch {
    roleOptions.value = []
  }
}

/** 编辑回显时临时关闭 watch 的清空逻辑，避免覆盖刚加载的已分配角色 */
const suppressRoleWatch = ref(false)

/** 切换机构时重载角色选项并清空已选角色（程序化回显时仅重载不清空） */
watch(
  () => form.organCode,
  (code) => {
    if (!suppressRoleWatch.value) {
      form.roleCodes = []
    }
    loadRoles(code)
  }
)

const rules: FormRules<Account> = {
  organCode: [
    // 列表查询时可选，但新增/编辑账号时必填（需归属机构）
    {
      validator: (_rule, value: string, callback) => {
        if (dialogVisible.value && !value) {
          callback(new Error('请输入机构编码'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 100, message: '登录账号长度不能超过 100', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    {
      // 仅新增时必填
      validator: (_rule, value: string, callback) => {
        if (dialogType.value === 'create' && !value) {
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
    organCode: query.organCode || '',
    accountCode: undefined,
    username: '',
    password: '',
    realName: '',
    gender: 0,
    phone: '',
    email: '',
    avatar: '',
    accountStatus: AccountStatus.NORMAL,
    isAdmin: 0,
    remark: '',
    roleCodes: []
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: Account) {
  dialogType.value = 'edit'
  // 临时抑制 watch 清空，回显阶段由 openEdit 自行加载已分配角色
  suppressRoleWatch.value = true
  resetForm()
  Object.assign(form, {
    organCode: row.organCode,
    accountCode: row.accountCode,
    username: row.username,
    realName: row.realName,
    gender: row.gender,
    phone: row.phone,
    email: row.email,
    avatar: row.avatar,
    accountStatus: row.accountStatus,
    isAdmin: row.isAdmin,
    remark: row.remark,
    roleCodes: []
  })
  form.password = '' // 编辑时密码留空表示不修改
  dialogVisible.value = true
  // 加载该机构角色选项 + 回显账号已分配角色
  await loadRoles(row.organCode)
  if (row.accountCode) {
    form.roleCodes = await getAccountRoles(row.accountCode)
  }
  suppressRoleWatch.value = false
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
    // 编辑态且密码留空时，剔除 password 字段避免误传空串
    const payload: Account = { ...form }
    if (dialogType.value === 'edit' && !payload.password) {
      delete payload.password
    }
    // 角色走单独的分配接口，不随账号主体提交
    const roleCodes = payload.roleCodes ?? []
    delete payload.roleCodes

    if (dialogType.value === 'create') {
      const accountCode = await createAccount(payload)
      await assignAccountRoles(accountCode, roleCodes)
      ElMessage.success('新增成功')
    } else if (form.accountCode) {
      await updateAccount(form.accountCode, payload)
      await assignAccountRoles(form.accountCode, roleCodes)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: Account) {
  if (!row.accountCode) return
  await ElMessageBox.confirm(`确定删除账号「${row.username}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteAccount(row.accountCode)
  ElMessage.success('删除成功')
  loadPage()
}

/** 切换账号状态（正常↔禁用；锁定态不提供切换开关） */
async function handleStatusChange(row: Account) {
  if (!row.accountCode) return
  const nextStatus =
    row.accountStatus === AccountStatus.NORMAL ? AccountStatus.DISABLED : AccountStatus.NORMAL
  try {
    await switchAccountStatus(row.accountCode, nextStatus)
    row.accountStatus = nextStatus
    ElMessage.success(nextStatus === AccountStatus.NORMAL ? '已恢复正常' : '已禁用')
  } catch {
    // 失败不改变状态（响应拦截器已报错）
  }
}

/** 重置密码二次确认 */
async function handleResetPassword(row: Account) {
  if (!row.accountCode) return
  await ElMessageBox.confirm(`确定将账号「${row.username}」的密码重置为默认密码吗？`, '重置密码', {
    confirmButtonText: '确定重置',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await resetPassword(row.accountCode)
  ElMessage.success('密码已重置')
}

function handleReset() {
  query.username = ''
  query.realName = ''
  query.accountStatus = undefined
  query.organCode = ''
  handleSearch()
}

function genderText(v?: number) {
  const opt = GENDER_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '未知'
}

// 初始化加载
onMounted(() => {
  loadOrgans()
})
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-select v-model="query.organCode" placeholder="机构" clearable filterable style="width: 200px">
          <el-option
            v-for="o in organOptions"
            :key="o.organCode"
            :label="o.fullName || o.shortName || o.organCode"
            :value="o.organCode"
          />
        </el-select>
        <el-input v-model="query.username" placeholder="登录账号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-input v-model="query.realName" placeholder="真实姓名" clearable style="width: 150px" @keyup.enter="handleSearch" />
        <el-select v-model="query.accountStatus" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="o in ACCOUNT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <el-button v-permission="'organ:account:create'" type="primary" :icon="'Plus'" @click="openCreate">新增账号</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="accountCode">
        <el-table-column prop="accountCode" label="账号编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="username" label="登录账号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="organName" label="机构" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.organName || row.organCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <template v-if="row.roleNames && row.roleNames.length">
              <el-tag v-for="name in row.roleNames" :key="name" size="small" style="margin-right: 4px">
                {{ name }}
              </el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="loginCount" label="登录次数" width="90" align="center" />
        <el-table-column prop="lastLoginTime" label="最近登录" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.accountStatus === AccountStatus.LOCKED" type="warning" size="small">锁定</el-tag>
            <el-switch
              v-else
              :model-value="row.accountStatus === AccountStatus.NORMAL"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'organ:account:update'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'organ:account:reset'" link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button v-permission="'organ:account:delete'" link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
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
      :title="dialogType === 'create' ? '新增账号' : '编辑账号'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="机构" prop="organCode">
              <el-select v-model="form.organCode" placeholder="请选择机构" filterable style="width: 100%">
                <el-option
                  v-for="o in organOptions"
                  :key="o.organCode"
                  :label="o.fullName || o.shortName || o.organCode"
                  :value="o.organCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色">
              <el-select
                v-model="form.roleCodes"
                multiple
                collapse-tags
                collapse-tags-tooltip
                :placeholder="form.organCode ? '请选择角色' : '请先选择机构'"
                :disabled="!form.organCode"
                style="width: 100%"
              >
                <el-option v-for="r in roleOptions" :key="r.roleCode" :label="r.roleName" :value="r.roleCode!" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="登录账号" prop="username">
              <el-input v-model="form.username" placeholder="登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                :placeholder="dialogType === 'create' ? '请输入密码' : '留空表示不修改'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="性别" style="width: 100%">
                <el-option v-for="o in GENDER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="头像">
              <FileUploader v-model="form.avatar" type="image" module="account" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.accountStatus" style="width: 100%">
                <el-option v-for="o in ACCOUNT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否超管">
              <el-switch :model-value="form.isAdmin === 1" @change="(v: boolean) => (form.isAdmin = v ? 1 : 0)" />
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

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}

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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .card-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2329;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
