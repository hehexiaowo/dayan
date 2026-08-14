<script setup lang="ts">
/**
 * 管家详情页 - 账号 tab。
 *
 * 分上下两区：
 * 1. 后台登录账号（organ_account）：管家登录 admin 使用的账号。
 *    - 未开通：提示 + 「开通后台账号」弹窗（username + password，密码留空用系统默认）。
 *    - 已开通：el-descriptions 展示 + 「重置密码」（走 organ 账号接口）。
 * 2. 管家独立账号（butler_account，分页 CRUD）：面向未来管家端的独立账号体系，维持不变。
 *
 * 管家独立账号关键约束（与 client 域 account 不同，client account 主键是 clientCode）：
 * - 主键是自增 id（number），update/delete/reset-password 都用 id。
 * - VO 不含 password（不返回）。
 * - 新建时 password 可填（留空服务端用默认值 dayan@123）；编辑时不改密码（单独走重置按钮）。
 * - username 创建后不可改（UpdateDTO 不含 username）。
 * - accountStatus 是 3 态枚举（0锁定 1正常 2禁用），列表用 tag 展示，表单用 select。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  getButler,
  openButlerAccount,
  pageButlerAccounts,
  createButlerAccount,
  updateButlerAccount,
  deleteButlerAccount,
  resetButlerAccountPassword
} from '@/api/service'
import { getAccount, resetPassword as resetOrganAccountPassword } from '@/api/account'
import {
  BUTLER_ACCOUNT_STATUS_OPTIONS,
  butlerAccountStatusLabel,
  butlerAccountStatusTagType
} from '@/types/service'
import type { ButlerAccount, ButlerAccountQuery } from '@/types/service'
import type { Account } from '@/types/account'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 管家编码（路由参数） */
  butlerCode: string
}>()

// ---------- 后台登录账号（organ_account） ----------
const organLoading = ref(false)
const organAccount = ref<Account | null>(null)
/** 管家已关联的后台账号编码（空串 = 未开通） */
const organAccountCode = ref('')

async function loadOrganAccount() {
  organLoading.value = true
  try {
    const butler = await getButler(props.butlerCode)
    organAccountCode.value = butler.accountCode ?? ''
    organAccount.value = organAccountCode.value ? await getAccount(organAccountCode.value) : null
  } catch {
    organAccount.value = null
  } finally {
    organLoading.value = false
  }
}

loadOrganAccount()

// 开通后台账号弹窗
const organDialogVisible = ref(false)
const organSubmitLoading = ref(false)
const organFormRef = ref<FormInstance>()
const organForm = reactive({ username: '', password: '' })
const organRules: FormRules = {
  username: [{ required: true, message: '请输入登录用户名', trigger: 'blur' }],
  password: [{ min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }]
}

function openOrganCreate() {
  organForm.username = ''
  organForm.password = ''
  organDialogVisible.value = true
}

async function handleOrganSubmit() {
  if (!organFormRef.value) return
  try {
    await organFormRef.value.validate()
  } catch {
    return
  }
  organSubmitLoading.value = true
  try {
    await openButlerAccount(props.butlerCode, {
      username: organForm.username,
      password: organForm.password || undefined
    })
    ElMessage.success('后台账号已开通')
    organDialogVisible.value = false
    loadOrganAccount()
  } finally {
    organSubmitLoading.value = false
  }
}

async function handleResetOrganPassword() {
  if (!organAccountCode.value || !organAccount.value) return
  await ElMessageBox.confirm(
    `确定将后台账号「${organAccount.value.username}」的密码重置为默认密码吗？`,
    '重置密码',
    { confirmButtonText: '确定重置', cancelButtonText: '取消', type: 'warning' }
  )
  await resetOrganAccountPassword(organAccountCode.value)
  ElMessage.success('密码已重置')
}

// ---------- 管家独立账号（butler_account，主键 id 自增 number） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<ButlerAccount, ButlerAccountQuery, number>(
    {
      page: pageButlerAccounts,
      create: createButlerAccount,
      update: (id, data) => updateButlerAccount(id, data),
      remove: (id) => deleteButlerAccount(id)
    },
    {
      initialQuery: { username: '', phone: '', accountStatus: undefined },
      idKey: 'id',
      fixedParams: { butlerCode: props.butlerCode }
    }
  )

loadPage()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 账号表单（含 password 仅 create 时使用，编辑时不展示密码） */
const form = reactive<ButlerAccount & { password?: string }>({
  id: undefined,
  butlerCode: '',
  username: '',
  phone: '',
  openId: '',
  unionId: '',
  lastLoginTime: '',
  accountStatus: 1,
  password: ''
})

const rules: FormRules<ButlerAccount & { password?: string }> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    butlerCode: '',
    username: '',
    phone: '',
    openId: '',
    unionId: '',
    lastLoginTime: '',
    accountStatus: 1,
    password: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  // butlerCode 从上下文带入（新建账号关联到当前管家）
  form.butlerCode = props.butlerCode
  dialogVisible.value = true
}

function openEdit(row: ButlerAccount) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    butlerCode: row.butlerCode,
    username: row.username ?? '',
    phone: row.phone ?? '',
    openId: row.openId ?? '',
    unionId: row.unionId ?? '',
    lastLoginTime: row.lastLoginTime ?? '',
    accountStatus: row.accountStatus ?? 1,
    password: ''
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
      await createButlerAccount(form)
      ElMessage.success('新增成功')
    } else if (form.id != null) {
      // 编辑时不提交 password（密码走重置按钮）；username 不可改但提交无害（后端忽略）
      const { password: _password, ...rest } = form
      void _password
      await updateButlerAccount(form.id, rest)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ButlerAccount) {
  if (row.id == null) return
  await ElMessageBox.confirm(`确定删除账号「${row.username || row.id}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteButlerAccount(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

async function handleResetPassword(row: ButlerAccount) {
  if (row.id == null) return
  await ElMessageBox.confirm(
    `确定重置账号「${row.username || row.id}」的密码？重置后为默认密码 dayan@123。`,
    '重置密码',
    {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await resetButlerAccountPassword(row.id)
  ElMessage.success('密码已重置为 dayan@123')
}

defineExpose({ loadPage })
</script>

<template>
  <div class="account-tab">
    <!-- 后台登录账号（organ_account） -->
    <el-card shadow="never" class="organ-account-card">
      <template #header>
        <div class="card-header">
          <span>后台登录账号（admin）</span>
          <el-button
            v-if="!organAccount"
            v-permission="'butler:info:update'"
            type="primary"
            size="small"
            @click="openOrganCreate"
          >
            开通后台账号
          </el-button>
          <el-button
            v-else
            v-permission="'organ:account:reset'"
            link
            type="warning"
            size="small"
            @click="handleResetOrganPassword"
          >
            重置密码
          </el-button>
        </div>
      </template>
      <div v-loading="organLoading">
        <el-descriptions v-if="organAccount" :column="3" border size="small">
          <el-descriptions-item label="用户名">{{ organAccount.username }}</el-descriptions-item>
          <el-descriptions-item label="账号编码">{{ organAccount.accountCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="butlerAccountStatusTagType(organAccount.accountStatus)" size="small">
              {{ butlerAccountStatusLabel(organAccount.accountStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="手机号">{{ organAccount.phone || '--' }}</el-descriptions-item>
          <el-descriptions-item label="最后登录">
            {{ formatDateTime(organAccount.lastLoginTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="登录次数">{{ organAccount.loginCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <template v-if="organAccount.roleNames?.length">
              <el-tag v-for="r in organAccount.roleNames" :key="r" size="small" class="mr-4">{{ r }}</el-tag>
            </template>
            <span v-else>--</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-alert
          v-else-if="!organLoading"
          type="info"
          :closable="false"
          show-icon
          title="未开通后台账号。开通后管家可使用该账号登录 admin 后台（归属「养老管家」部门，默认「普通管家」角色）。"
        />
      </div>
    </el-card>

    <el-divider content-position="left">管家独立账号（butler_account，预留未来管家端登录）</el-divider>

    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="用户名" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="query.phone" placeholder="手机号" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.accountStatus" placeholder="全部" clearable style="width: 120px">
          <el-option
            v-for="o in BUTLER_ACCOUNT_STATUS_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增账号</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="username" label="用户名" min-width="140" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column prop="lastLoginTime" label="最后登录时间" width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="butlerAccountStatusTagType(row.accountStatus)" size="small">
            {{ butlerAccountStatusLabel(row.accountStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
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

    <!-- 开通后台账号弹窗 -->
    <el-dialog
      v-model="organDialogVisible"
      title="开通后台账号"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="将为该管家创建 admin 后台登录账号（organ_account），挂靠「养老管家」部门并授予「普通管家」角色。"
        class="mb-16"
      />
      <el-form ref="organFormRef" :model="organForm" :rules="organRules" label-width="110px">
        <el-form-item label="登录用户名" prop="username">
          <el-input v-model="organForm.username" placeholder="全平台唯一" maxlength="50" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input
            v-model="organForm.password"
            type="password"
            show-password
            placeholder="留空使用系统默认密码"
            maxlength="64"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="organDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="organSubmitLoading" @click="handleOrganSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增账号' : '编辑账号'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="管家编码">
              <el-input v-model="form.butlerCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <!-- 编辑时 username 不可改（后端 UpdateDTO 不含 username） -->
              <el-input
                v-model="form.username"
                :disabled="dialogMode === 'edit'"
                placeholder="登录用户名"
                maxlength="64"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="绑定手机号" maxlength="32" />
            </el-form-item>
          </el-col>
          <!-- 仅新增时展示密码，编辑改走重置按钮 -->
          <el-col v-if="dialogMode === 'create'" :span="12">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                placeholder="留空使用默认密码 dayan@123"
                maxlength="64"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-select v-model="form.accountStatus" style="width: 100%">
                <el-option
                  v-for="o in BUTLER_ACCOUNT_STATUS_OPTIONS"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="微信 openId">
              <el-input v-model="form.openId" placeholder="openId" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="微信 unionId">
              <el-input v-model="form.unionId" placeholder="unionId" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="24">
            <el-alert
              type="info"
              :closable="false"
              title="密码不可直接修改，请使用操作列的【重置密码】按钮（将重置为默认密码 dayan@123）。"
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
  </div>
</template>

<style scoped lang="scss">
.account-tab {
  .organ-account-card {
    margin-bottom: 8px;

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
  }

  .mr-4 {
    margin-right: 4px;
  }

  .mb-16 {
    margin-bottom: 16px;
  }

  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
