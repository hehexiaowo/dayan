<script setup lang="ts">
/**
 * 渠道详情页 - 账户 tab。
 *
 * 分页模式：useCrud（主键 accountCode，传 idKey:'accountCode'，fixedParams:{channelCode}）。
 *
 * 关键约束：
 * - 业务键 accountCode（服务端生成 CA 前缀），路径参数用 accountCode（非自增 id）。
 * - username 编辑时不可改（UpdateDTO 不含 channelCode/username/accountCode/password）。
 * - VO 不含 password；新建时 password 可填（留空服务端默认）；编辑改走重置密码按钮。
 * - accountStatus 3 态（0锁定/1正常/2禁用），isAdmin 2 态（0否/1是）。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageChannelAccounts,
  createChannelAccount,
  updateChannelAccount,
  deleteChannelAccount,
  resetChannelAccountPassword
} from '@/api/channel-sub'
import { CHANNEL_ACCOUNT_STATUS_OPTIONS, CHANNEL_IS_ADMIN_OPTIONS } from '@/types/channel'
import type { ChannelAccount, ChannelAccountQuery } from '@/types/channel'

const props = defineProps<{
  /** 渠道编码（路由参数） */
  channelCode: string
}>()

// ---------- 账号列表（useCrud，主键 accountCode） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ChannelAccount,
  ChannelAccountQuery
>(
  {
    page: pageChannelAccounts,
    create: createChannelAccount,
    update: (accountCode, data) => updateChannelAccount(accountCode, data),
    remove: (accountCode) => deleteChannelAccount(accountCode)
  },
  {
    initialQuery: { username: '', realName: '', accountStatus: undefined },
    idKey: 'accountCode',
    fixedParams: { channelCode: props.channelCode }
  }
)

loadPage()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 账号表单（含 password 仅 create 时使用，编辑时不展示密码） */
const form = reactive<ChannelAccount & { password?: string }>({
  id: undefined,
  channelCode: '',
  accountCode: '',
  username: '',
  realName: '',
  avatar: '',
  phone: '',
  openId: '',
  unionId: '',
  email: '',
  position: '',
  accountStatus: 1,
  isAdmin: 0,
  password: ''
})

const rules: FormRules<ChannelAccount & { password?: string }> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    channelCode: '',
    accountCode: '',
    username: '',
    realName: '',
    avatar: '',
    phone: '',
    openId: '',
    unionId: '',
    email: '',
    position: '',
    accountStatus: 1,
    isAdmin: 0,
    password: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.channelCode = props.channelCode
  dialogVisible.value = true
}

function openEdit(row: ChannelAccount) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    channelCode: row.channelCode,
    accountCode: row.accountCode,
    username: row.username ?? '',
    realName: row.realName ?? '',
    avatar: row.avatar ?? '',
    phone: row.phone ?? '',
    openId: row.openId ?? '',
    unionId: row.unionId ?? '',
    email: row.email ?? '',
    position: row.position ?? '',
    accountStatus: row.accountStatus ?? 1,
    isAdmin: row.isAdmin ?? 0,
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
      await createChannelAccount(form)
      ElMessage.success('新增成功')
    } else if (form.accountCode) {
      // 编辑时不提交 password（密码走重置按钮）；UpdateDTO 不含 username/channelCode/accountCode
      const { password: _password, username: _u, channelCode: _c, accountCode: _a, ...rest } = form
      void _password
      void _u
      void _c
      void _a
      await updateChannelAccount(form.accountCode, rest)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ChannelAccount) {
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

async function handleResetPassword(row: ChannelAccount) {
  if (!row.accountCode) return
  await ElMessageBox.confirm(
    `确定重置账号「${row.username || row.accountCode}」的密码？重置后为默认密码。`,
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

// ---------- 辅助渲染 ----------
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

function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

defineExpose({ loadPage })
</script>

<template>
  <div class="account-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="用户名" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="query.realName" placeholder="真实姓名" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.accountStatus" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in CHANNEL_ACCOUNT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增账号</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="accountCode">
      <el-table-column prop="accountCode" label="账号编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="username" label="用户名" min-width="130" show-overflow-tooltip />
      <el-table-column prop="realName" label="姓名" min-width="100" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
      <el-table-column prop="position" label="职位" min-width="100" show-overflow-tooltip />
      <el-table-column label="管理员" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isAdmin === 1" type="danger" size="small">是</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="loginCount" label="登录次数" width="90" align="center" />
      <el-table-column prop="lastLoginTime" label="最后登录" width="150" align="center">
        <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="accountStatusTagType(row.accountStatus)" size="small">
            {{ accountStatusLabel(row.accountStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增账号' : '编辑账号'"
      width="720px"
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
          <el-col :span="12">
            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" placeholder="真实姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="邮箱" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-input v-model="form.position" placeholder="职位" maxlength="50" />
            </el-form-item>
          </el-col>
          <!-- 仅新增时展示密码，编辑改走重置按钮 -->
          <el-col v-if="dialogMode === 'create'" :span="12">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                placeholder="留空使用默认密码"
                maxlength="64"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-select v-model="form.accountStatus" style="width: 100%">
                <el-option v-for="o in CHANNEL_ACCOUNT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否管理员">
              <el-select v-model="form.isAdmin" style="width: 100%">
                <el-option v-for="o in CHANNEL_IS_ADMIN_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <el-col :span="12">
            <el-form-item label="头像URL">
              <el-input v-model="form.avatar" placeholder="头像 URL" />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="24">
            <el-alert
              type="info"
              :closable="false"
              title="用户名不可改；密码不可直接修改，请使用操作列的【重置密码】按钮。"
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
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
