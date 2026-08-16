<script setup lang="ts">
/**
 * 客户详情页 - 账号 tab。
 *
 * 分页模式：useCrud（主键 clientCode，传 idKey:'clientCode'，fixedParams:{clientCode}）。
 *
 * 关键约束：
 * - 主键是 clientCode（业务编码），不是自增 id；update/delete/reset-password 都用 clientCode。
 * - VO 不含 password（不返回）。
 * - 新建时 password 可填（留空服务端用默认值 dayan@123）；编辑时不改密码（单独走重置按钮）。
 * - accountStatus 是 3 态枚举（0锁定 1正常 2禁用），后端 DTO 文档有，列表用 tag 展示，表单用 select。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageClientAccounts,
  createClientAccount,
  updateClientAccount,
  deleteClientAccount,
  resetClientAccountPassword
} from '@/api/client-sub'
import type { ClientAccount, ClientAccountQuery } from '@/types/client'

const props = defineProps<{
  /** 客户编码（路由参数） */
  clientCode: string
  /** 所属渠道编码（新建账号时必填，从客户主信息带入） */
  channelCode?: string
}>()

// ---------- 账号列表（useCrud，主键 clientCode） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ClientAccount,
  ClientAccountQuery
>(
  {
    page: pageClientAccounts,
    create: createClientAccount,
    update: (clientCode, data) => updateClientAccount(clientCode, data),
    remove: (clientCode) => deleteClientAccount(clientCode)
  },
  {
    initialQuery: { username: '', phone: '', accountStatus: undefined },
    // 主键字段名为 clientCode（业务编码），非默认的 'code'，也非自增 id
    idKey: 'clientCode',
    fixedParams: { clientCode: props.clientCode }
  }
)

loadPage()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 账号表单（含 password 仅 create 时使用，编辑时不展示密码） */
const form = reactive<ClientAccount & { password?: string }>({
  id: undefined,
  clientCode: '',
  channelCode: '',
  username: '',
  phone: '',
  openId: '',
  unionId: '',
  alipayId: '',
  extAccountNo: '',
  accountStatus: 1,
  password: ''
})

const rules: FormRules<ClientAccount & { password?: string }> = {
  clientCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }],
  password: [
    { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }
  ]
}

// accountStatus 选项（3 态，对齐后端 ClientAccountCreateDTO 文档）
const ACCOUNT_STATUS_OPTIONS = [
  { label: '正常', value: 1 },
  { label: '锁定', value: 0 },
  { label: '禁用', value: 2 }
] as const

function resetForm() {
  Object.assign(form, {
    id: undefined,
    clientCode: '',
    channelCode: '',
    username: '',
    phone: '',
    openId: '',
    unionId: '',
    alipayId: '',
    extAccountNo: '',
    accountStatus: 1,
    password: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  // clientCode + channelCode 从上下文带入（新建账号关联到当前客户）
  form.clientCode = props.clientCode
  form.channelCode = props.channelCode ?? ''
  dialogVisible.value = true
}

function openEdit(row: ClientAccount) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    clientCode: row.clientCode,
    channelCode: row.channelCode,
    username: row.username ?? '',
    phone: row.phone ?? '',
    openId: row.openId ?? '',
    unionId: row.unionId ?? '',
    alipayId: row.alipayId ?? '',
    extAccountNo: row.extAccountNo ?? '',
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
      await createClientAccount(form)
      ElMessage.success('新增成功')
    } else if (form.clientCode) {
      // 编辑时不提交 password（密码走重置按钮）
      const { password: _password, ...rest } = form
      void _password
      await updateClientAccount(form.clientCode, rest)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ClientAccount) {
  if (!row.clientCode) return
  await ElMessageBox.confirm(`确定删除账号「${row.username || row.clientCode}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteClientAccount(row.clientCode)
  ElMessage.success('删除成功')
  loadPage()
}

async function handleResetPassword(row: ClientAccount) {
  if (!row.clientCode) return
  await ElMessageBox.confirm(
    `确定重置账号「${row.username || row.clientCode}」的密码？重置后为默认密码 dayan@123。`,
    '重置密码',
    {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await resetClientAccountPassword(row.clientCode)
  ElMessage.success('密码已重置为 dayan@123')
}

// ---------- 辅助渲染 ----------
function accountStatusLabel(v?: number): string {
  const found = ACCOUNT_STATUS_OPTIONS.find((o) => o.value === v)
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
    <div class="toolbar">
      <el-input v-model="query.username" placeholder="用户名" clearable style="width: 160px" @keyup.enter="handleSearch" />
      <el-input v-model="query.phone" placeholder="手机号" clearable style="width: 140px" @keyup.enter="handleSearch" />
      <el-select v-model="query.accountStatus" placeholder="状态" clearable style="width: 120px">
        <el-option v-for="o in ACCOUNT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增账号</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="clientCode">
      <el-table-column prop="clientCode" label="客户编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="username" label="用户名" min-width="140" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column prop="channelCode" label="所属渠道" min-width="140" show-overflow-tooltip />
      <el-table-column prop="loginCount" label="登录次数" width="100" align="center" />
      <el-table-column prop="lastLoginTime" label="最后登录时间" width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
      </el-table-column>
      <el-table-column prop="lastLoginIp" label="最后登录IP" width="140" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="accountStatusTagType(row.accountStatus)" size="small">
            {{ accountStatusLabel(row.accountStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="120" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt).slice(0, 10) }}</template>
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户编码">
              <el-input v-model="form.clientCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属渠道">
              <el-input v-model="form.channelCode" disabled placeholder="所属渠道编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="form.username" placeholder="登录用户名" maxlength="64" />
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
                <el-option v-for="o in ACCOUNT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
            <el-form-item label="支付宝账号">
              <el-input v-model="form.alipayId" placeholder="alipayId" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="外部账号">
              <el-input v-model="form.extAccountNo" placeholder="外部账号" maxlength="128" />
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
