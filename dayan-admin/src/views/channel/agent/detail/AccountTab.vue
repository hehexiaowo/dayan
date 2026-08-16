<script setup lang="ts">
/**
 * 代理人详情页 - 账号 tab。
 *
 * 特殊：一代理人一账号（1:1 强约束），非列表。
 * - 先调 getAgentAccount(agentCode)：
 *   - 无账号（返回 null 或 404）：显示 el-empty + "开通账号"按钮 → 打开新增弹窗。
 *   - 有账号：el-descriptions 展示 + "编辑"按钮 + "重置密码"按钮 + "删除账号"按钮。
 * - 主键用 agentCode（不是 id）。手动管理（useCrud 不适合 1:1）。
 *
 * 关键约束：
 * - username 渠道内唯一，create 后不可改（UpdateDTO 无 username，编辑时 disabled）。
 * - VO 不含 password。lastLoginTime/lastLoginIp 只读。
 * - accountStatus 3 态（0锁定 1正常 2禁用）。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAgentAccount,
  createAgentAccount,
  updateAgentAccount,
  deleteAgentAccount,
  resetAgentAccountPassword
} from '@/api/agent'
import {
  ACCOUNT_STATUS_OPTIONS,
  accountStatusLabel,
  accountStatusTagType
} from '@/types/agent'
import type { AgentAccount } from '@/types/agent'

const props = defineProps<{
  /** 代理人编码（路由参数） */
  agentCode: string
  /** 所属渠道编码（新建账号时带入） */
  channelCode?: string
}>()

// ---------- 加载单条账号（1:1） ----------
const loading = ref(false)
const account = ref<AgentAccount | null>(null)

async function loadAccount() {
  if (!props.agentCode) return
  loading.value = true
  try {
    account.value = await getAgentAccount(props.agentCode)
  } catch {
    // 不存在账号时后端可能 404，统一按无账号处理
    account.value = null
  } finally {
    loading.value = false
  }
}

loadAccount()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 账号表单（含 password 仅 create 时使用，编辑时不展示密码） */
const form = reactive<AgentAccount & { password?: string }>({
  id: undefined,
  agentCode: '',
  channelCode: '',
  username: '',
  phone: '',
  openId: '',
  unionId: '',
  extAccountNo: '',
  accountStatus: 1,
  password: ''
})

const rules: FormRules<AgentAccount & { password?: string }> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    agentCode: '',
    channelCode: '',
    username: '',
    phone: '',
    openId: '',
    unionId: '',
    extAccountNo: '',
    accountStatus: 1,
    password: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  // agentCode + channelCode 从上下文带入（新建账号关联到当前代理人）
  form.agentCode = props.agentCode
  form.channelCode = props.channelCode ?? ''
  dialogVisible.value = true
}

function openEdit() {
  if (!account.value) return
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: account.value.id,
    agentCode: account.value.agentCode,
    channelCode: account.value.channelCode,
    username: account.value.username ?? '',
    phone: account.value.phone ?? '',
    openId: account.value.openId ?? '',
    unionId: account.value.unionId ?? '',
    extAccountNo: account.value.extAccountNo ?? '',
    accountStatus: account.value.accountStatus ?? 1,
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
      await createAgentAccount(form)
      ElMessage.success('开通成功')
    } else if (form.agentCode) {
      // 编辑时不提交 password（密码走重置按钮），username 不可改也不提交
      const { password: _password, username: _username, ...rest } = form
      void _password
      void _username
      await updateAgentAccount(form.agentCode, rest)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    await loadAccount()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete() {
  if (!account.value?.agentCode) return
  await ElMessageBox.confirm(
    `确定删除账号「${account.value.username || account.value.agentCode}」吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteAgentAccount(account.value.agentCode)
  ElMessage.success('删除成功')
  await loadAccount()
}

async function handleResetPassword() {
  if (!account.value?.agentCode) return
  await ElMessageBox.confirm(
    `确定重置账号「${account.value.username || account.value.agentCode}」的密码？重置后为默认密码 dayan@123。`,
    '重置密码',
    {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await resetAgentAccountPassword(account.value.agentCode)
  ElMessage.success('密码已重置为 dayan@123')
}

// ---------- 辅助渲染 ----------
function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

defineExpose({ loadAccount })
</script>

<template>
  <div v-loading="loading">
    <!-- 有账号：展示详情 + 操作 -->
    <template v-if="account">
      <div class="toolbar">
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑账号</el-button>
          <el-button type="warning" :icon="'Key'" plain @click="handleResetPassword">重置密码</el-button>
          <el-button type="danger" :icon="'Delete'" plain @click="handleDelete">删除账号</el-button>
        </div>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="代理人编码">{{ account.agentCode }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ account.username ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="所属渠道">{{ account.channelCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ account.phone ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="外部账号">{{ account.extAccountNo ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="accountStatusTagType(account.accountStatus)" size="small">
            {{ accountStatusLabel(account.accountStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="微信 openId">{{ account.openId ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="微信 unionId">{{ account.unionId ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="最后登录时间">{{ formatDateTime(account.lastLoginTime) }}</el-descriptions-item>
        <el-descriptions-item label="最后登录IP">{{ account.lastLoginIp ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(account.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(account.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </template>

    <!-- 无账号：开通 -->
    <template v-else>
      <el-empty description="该代理人暂未开通账号">
        <el-button type="primary" :icon="'Plus'" @click="openCreate">开通账号</el-button>
      </el-empty>
    </template>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '开通账号' : '编辑账号'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="代理人编码">
              <el-input v-model="form.agentCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属渠道">
              <el-input v-model="form.channelCode" disabled placeholder="所属渠道编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <!-- username 渠道内唯一，create 后不可改（UpdateDTO 无 username） -->
              <el-input
                v-model="form.username"
                :disabled="dialogMode === 'edit'"
                placeholder="登录用户名（渠道内唯一）"
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
            <el-form-item label="外部账号">
              <el-input v-model="form.extAccountNo" placeholder="外部账号" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="24">
            <el-alert
              type="info"
              :closable="false"
              title="用户名创建后不可修改；密码不可直接修改，请使用【重置密码】按钮（将重置为默认密码 dayan@123）。"
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

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.toolbar .toolbar-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}
</style>
