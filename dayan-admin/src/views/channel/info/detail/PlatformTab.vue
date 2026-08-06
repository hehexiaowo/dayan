<script setup lang="ts">
/**
 * 渠道详情页 - 开放平台对接 tab。
 *
 * 分页模式：useCrud（主键 id 自增 Long，传 idKey:'id'，fixedParams:{channelCode}）。
 *
 * 关键约束（与 account/role 子表不同）：
 * - 主键是自增 id（Long），**路径参数用 id，非业务编码字符串**。
 * - appSecret 出参为脱敏占位 `***`（明文不回传）；编辑时留空=不改，填值=轮换。
 * - UpdateDTO 不含 channelCode（id 走 path）。
 * - dockType DDL 权威（1H5嵌入/2API对接/3SDK集成），DTO 注释矛盾已采信 DDL。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageOpenPlatforms,
  createOpenPlatform,
  updateOpenPlatform,
  deleteOpenPlatform
} from '@/api/channel-sub'
import {
  CHANNEL_DOCK_TYPE_OPTIONS,
  CHANNEL_AUTH_TYPE_OPTIONS,
  CHANNEL_PLATFORM_STATUS_OPTIONS
} from '@/types/channel'
import type { ChannelOpenPlatform, ChannelOpenPlatformQuery } from '@/types/channel'

const props = defineProps<{
  /** 渠道编码（路由参数） */
  channelCode: string
}>()

// ---------- 开放平台列表（useCrud，主键 id） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ChannelOpenPlatform,
  ChannelOpenPlatformQuery,
  number
>(
  {
    page: pageOpenPlatforms,
    create: createOpenPlatform,
    update: (id, data) => updateOpenPlatform(id, data),
    remove: (id) => deleteOpenPlatform(id)
  },
  {
    initialQuery: { platformName: '', dockType: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { channelCode: props.channelCode }
  }
)

loadPage()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ChannelOpenPlatform>({
  id: undefined,
  channelCode: '',
  platformName: '',
  dockType: 1,
  apiBaseUrl: '',
  appKey: '',
  appSecret: '',
  callbackUrl: '',
  h5Domain: '',
  h5Theme: '',
  authType: 1,
  ipWhitelist: '',
  rateLimit: undefined,
  timeout: undefined,
  extraConfig: '',
  status: 1
})

const rules: FormRules<ChannelOpenPlatform> = {
  platformName: [{ required: true, message: '请输入平台名称', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    channelCode: '',
    platformName: '',
    dockType: 1,
    apiBaseUrl: '',
    appKey: '',
    appSecret: '',
    callbackUrl: '',
    h5Domain: '',
    h5Theme: '',
    authType: 1,
    ipWhitelist: '',
    rateLimit: undefined,
    timeout: undefined,
    extraConfig: '',
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.channelCode = props.channelCode
  dialogVisible.value = true
}

function openEdit(row: ChannelOpenPlatform) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    channelCode: row.channelCode,
    platformName: row.platformName ?? '',
    dockType: row.dockType ?? 1,
    apiBaseUrl: row.apiBaseUrl ?? '',
    appKey: row.appKey ?? '',
    // appSecret 编辑时清空（脱敏占位不回填，留空表示不改）
    appSecret: '',
    callbackUrl: row.callbackUrl ?? '',
    h5Domain: row.h5Domain ?? '',
    h5Theme: row.h5Theme ?? '',
    authType: row.authType ?? 1,
    ipWhitelist: row.ipWhitelist ?? '',
    rateLimit: row.rateLimit,
    timeout: row.timeout,
    extraConfig: row.extraConfig ?? '',
    status: row.status ?? 1
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
      await createOpenPlatform(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      // UpdateDTO 不含 channelCode（id 走 path）；appSecret 空=不改，填值=轮换
      const { channelCode: _c, ...rest } = form
      void _c
      await updateOpenPlatform(form.id, rest)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ChannelOpenPlatform) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除开放平台「${row.platformName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteOpenPlatform(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function dockTypeLabel(v?: number): string {
  const found = CHANNEL_DOCK_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function authTypeLabel(v?: number): string {
  const found = CHANNEL_AUTH_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusLabel(v?: number): string {
  const found = CHANNEL_PLATFORM_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}

/** appSecret 列表脱敏（后端已返回 ***，这里二次防护） */
function maskSecret(v?: string): string {
  if (!v) return '--'
  if (v === '***') return '***'
  // 兜底：若意外返回明文，只显示前2后2
  return v.length > 6 ? v.slice(0, 2) + '****' + v.slice(-2) : '****'
}

function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

defineExpose({ loadPage })
</script>

<template>
  <div class="platform-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="平台名称">
        <el-input v-model="query.platformName" placeholder="平台名称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="对接类型">
        <el-select v-model="query.dockType" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="o in CHANNEL_DOCK_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in CHANNEL_PLATFORM_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增平台</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="platformName" label="平台名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="对接类型" width="110" align="center">
        <template #default="{ row }">{{ dockTypeLabel(row.dockType) }}</template>
      </el-table-column>
      <el-table-column prop="appKey" label="AppKey" min-width="160" show-overflow-tooltip />
      <el-table-column label="AppSecret" width="120" align="center">
        <template #default="{ row }">
          <span class="secret-cell">{{ maskSecret(row.appSecret) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="认证方式" width="100" align="center">
        <template #default="{ row }">{{ authTypeLabel(row.authType) }}</template>
      </el-table-column>
      <el-table-column prop="apiBaseUrl" label="API基础地址" min-width="200" show-overflow-tooltip />
      <el-table-column prop="callbackUrl" label="回调地址" min-width="200" show-overflow-tooltip />
      <el-table-column prop="rateLimit" label="限频(次/分)" width="110" align="center" />
      <el-table-column prop="timeout" label="超时(秒)" width="90" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="150" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
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
      :title="dialogMode === 'create' ? '新增开放平台' : '编辑开放平台'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属渠道">
              <el-input v-model="form.channelCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平台名称" prop="platformName">
              <el-input v-model="form.platformName" placeholder="平台名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对接类型">
              <el-select v-model="form.dockType" style="width: 100%">
                <el-option v-for="o in CHANNEL_DOCK_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="认证方式">
              <el-select v-model="form.authType" style="width: 100%">
                <el-option v-for="o in CHANNEL_AUTH_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="AppKey">
              <el-input v-model="form.appKey" placeholder="应用 Key" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="AppSecret">
              <el-input
                v-model="form.appSecret"
                type="password"
                show-password
                :placeholder="dialogMode === 'edit' ? '留空不修改，填值轮换密钥' : '明文密钥（存储前加密）'"
                maxlength="256"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="API基础地址">
              <el-input v-model="form.apiBaseUrl" placeholder="https://..." maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="回调地址">
              <el-input v-model="form.callbackUrl" placeholder="https://..." maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="H5域名">
              <el-input v-model="form.h5Domain" placeholder="H5 域名" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="H5主题">
              <el-input v-model="form.h5Theme" placeholder="H5 主题配置" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="限频(次/分)">
              <el-input-number v-model="form.rateLimit" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="超时(秒)">
              <el-input-number v-model="form.timeout" :min="1" :max="300" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in CHANNEL_PLATFORM_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="IP白名单">
              <el-input v-model="form.ipWhitelist" type="textarea" :rows="2" placeholder='JSON 数组，如 ["1.2.3.4","10.0.0.0/8"]' />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="扩展配置">
              <el-input v-model="form.extraConfig" type="textarea" :rows="2" placeholder="扩展配置 JSON" />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="24">
            <el-alert
              type="info"
              :closable="false"
              title="AppSecret 明文不再回传；留空保持原密钥不变，填值则轮换为新密钥。"
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
.platform-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
  .secret-cell {
    color: var(--el-text-color-secondary);
    font-family: monospace;
  }
}
</style>
