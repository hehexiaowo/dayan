<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageAccounts,
  createAccount,
  updateAccount,
  deleteAccount,
  resetPassword,
  switchAccountStatus
} from '@/api/account'
import type { Account, AccountQuery } from '@/types/account'
import { AccountStatus, ACCOUNT_STATUS_OPTIONS, GENDER_OPTIONS } from '@/types/account'
import { formatDateTime } from '@/utils/format'

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
  accountStatus: AccountStatus.ENABLED,
  isAdmin: 0,
  remark: ''
})

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
    accountStatus: AccountStatus.ENABLED,
    isAdmin: 0,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Account) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, {
    organCode: row.organCode,
    accountCode: row.accountCode,
    username: row.username,
    realName: row.realName,
    gender: row.gender,
    phone: row.phone,
    email: row.email,
    accountStatus: row.accountStatus,
    isAdmin: row.isAdmin,
    remark: row.remark
  })
  form.password = '' // 编辑时密码留空表示不修改
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
    // 编辑态且密码留空时，剔除 password 字段避免误传空串
    const payload: Account = { ...form }
    if (dialogType.value === 'edit' && !payload.password) {
      delete payload.password
    }

    if (dialogType.value === 'create') {
      await createAccount(payload)
      ElMessage.success('新增成功')
    } else if (form.accountCode) {
      await updateAccount(form.accountCode, payload)
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

/** 切换账号启用状态 */
async function handleStatusChange(row: Account) {
  if (!row.accountCode) return
  const nextStatus =
    row.accountStatus === AccountStatus.ENABLED ? AccountStatus.DISABLED : AccountStatus.ENABLED
  try {
    await switchAccountStatus(row.accountCode, nextStatus)
    row.accountStatus = nextStatus
    ElMessage.success(nextStatus === AccountStatus.ENABLED ? '已启用' : '已禁用')
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
        <el-form-item label="账号">
          <el-input v-model="query.username" placeholder="登录账号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="真实姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.accountStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in ACCOUNT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>账号列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增账号</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="accountCode">
        <el-table-column prop="accountCode" label="账号编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="username" label="登录账号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" min-width="100" />
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
            <el-switch
              :model-value="row.accountStatus === 1"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
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
      :title="dialogType === 'create' ? '新增账号' : '编辑账号'"
      width="640px"
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
</style>
