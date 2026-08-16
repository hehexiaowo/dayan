<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getChannelInfoTree,
  getChannelInfoCurrent,
  createChannelInfo,
  updateChannelInfo,
  deleteChannelInfo
} from '@/api/channel-sub'
import {
  ChannelType,
  CHANNEL_TYPE_OPTIONS,
  ChannelStatus,
  CHANNEL_STATUS_OPTIONS,
  type ChannelInfo,
  type ChannelInfoQuery
} from '@/types/channel'

/**
 * 渠道架构管理页（树形表格）。
 *
 * - 后端 /channel-api/channel-infos/tree 已按当前登录账号所属渠道下钻，
 *   返回「本渠道及其子渠道」的树结构，前端直接喂给 el-table（tree-props.children）；
 * - 当前登录账号所属渠道的 canManage（0否/1是）来自 /current 接口：
 *   - 1 管理型：顶部展示「新增子渠道」按钮，行内展示「编辑/删除」操作；
 *   - 0 业务型：顶部展示「仅可查看」提示，隐藏所有增删改入口（降级展示）；
 * - 行内按钮显隐进一步按行 canManage 控制（后端逐行下钻）；
 * - 主键 channelCode 由服务端生成，新增表单不含 channelCode；
 * - 不使用 useCrud（树形展示 + 独立 CRUD API，非标准分页四件套），直接 ref + loadTree 管理。
 */

/** 列表 loading */
const loading = ref(false)
/** 树形表格数据（/tree 接口直接返回） */
const treeData = ref<ChannelInfo[]>([])
/** 当前登录账号所属渠道（用于取 canManage / channelCode） */
const currentChannel = ref<ChannelInfo>({} as ChannelInfo)

/** 当前渠道是否有管理权限（1 管理 / 0 业务） */
const canManageAll = ref<number>(0)

const query = reactive<ChannelInfoQuery>({
  fullName: '',
  channelType: undefined,
  status: undefined
})

/**
 * 拉取架构树 + 当前登录渠道信息。
 *
 * 两接口并发，失败时降级（空树，控制台 warn，不弹 toast），与 dayan-channel 其它页一致。
 */
async function loadTree() {
  loading.value = true
  try {
    const [tree, current] = await Promise.all([getChannelInfoTree(query), getChannelInfoCurrent()])
    treeData.value = tree ?? []
    currentChannel.value = current ?? ({} as ChannelInfo)
    canManageAll.value = current?.canManage ?? 0
  } catch (err) {
    // 后端端点未实现或鉴权失败时降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[organ] 加载渠道架构失败（接口可能未实现）:', err)
    treeData.value = []
    canManageAll.value = 0
  } finally {
    loading.value = false
  }
}

/** 搜索（重置展开状态后重新拉取） */
function handleSearch() {
  loadTree()
}

/** 重置查询条件并刷新 */
function handleReset() {
  query.fullName = ''
  query.channelType = undefined
  query.status = undefined
  loadTree()
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 弹窗表单（仅含本页可编辑字段，canManage 用下拉 1/0） */
const form = reactive<{
  channelCode?: string
  fullName: string
  shortName: string
  channelType: ChannelType
  parentCode?: string | null
  unifiedCreditCode: string
  contactPerson: string
  contactPhone: string
  address: string
  canManage: number
  status: ChannelStatus
}>({
  channelCode: undefined,
  fullName: '',
  shortName: '',
  channelType: ChannelType.ENTERPRISE,
  parentCode: null,
  unifiedCreditCode: '',
  contactPerson: '',
  contactPhone: '',
  address: '',
  canManage: 0,
  status: ChannelStatus.PENDING
})

// 注：不使用 FormRules<ChannelInfo>，避免 children 自引用导致循环类型推导
const rules: FormRules = {
  fullName: [{ required: true, message: '请输入渠道全称', trigger: 'blur' }],
  channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
  canManage: [{ required: true, message: '请选择渠道属性', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  // 联系电话：选填，但填写时必须是合法手机号或座机（支持 11 位手机 / 区号-座机 / 400 号）
  contactPhone: [
    {
      pattern: /^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8}|400-?\d{3}-?\d{4})$/,
      message: '请输入正确的手机号或座机号',
      trigger: 'blur'
    }
  ]
}

/** 重置表单到默认值 */
function resetForm() {
  Object.assign(form, {
    channelCode: undefined,
    fullName: '',
    shortName: '',
    channelType: ChannelType.ENTERPRISE,
    parentCode: currentChannel.value.channelCode ?? null,
    unifiedCreditCode: '',
    contactPerson: '',
    contactPhone: '',
    address: '',
    canManage: 0,
    status: ChannelStatus.PENDING
  })
}

/**
 * 打开新增弹窗。
 *
 * - 未传 parent 时，parentCode 默认填当前登录渠道 channelCode（架构页只在本渠道下挂子节点）；
 * - 传 parent（行内「新增子级」）时，挂在指定行下。
 */
function openCreate(parent?: ChannelInfo) {
  dialogType.value = 'create'
  resetForm()
  if (parent?.channelCode) {
    form.parentCode = parent.channelCode
  } else {
    form.parentCode = currentChannel.value.channelCode ?? null
  }
  dialogVisible.value = true
}

/**
 * 打开编辑弹窗（回显行数据）。
 *
 * 编辑模式下 channelCode 不可改（输入框禁用），其余字段可改。
 */
function openEdit(row: ChannelInfo) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, {
    channelCode: row.channelCode,
    fullName: row.fullName ?? '',
    shortName: row.shortName ?? '',
    channelType: row.channelType ?? ChannelType.ENTERPRISE,
    parentCode: row.parentCode ?? null,
    unifiedCreditCode: row.unifiedCreditCode ?? '',
    contactPerson: row.contactPerson ?? '',
    contactPhone: row.contactPhone ?? '',
    address: row.address ?? '',
    canManage: row.canManage ?? 0,
    status: row.status ?? ChannelStatus.PENDING
  })
  dialogVisible.value = true
}

/**
 * 提交新增 / 编辑。
 *
 * - create：POST /channel-api/channel-infos，后端返回 channelCode；
 * - edit：PUT /channel-api/channel-infos/{channelCode}，channelCode 取自表单（不可改）。
 */
async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    if (dialogType.value === 'create') {
      await createChannelInfo(form)
      ElMessage.success('新增成功')
    } else if (form.channelCode) {
      await updateChannelInfo(form.channelCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadTree()
  } finally {
    submitLoading.value = false
  }
}

/**
 * 删除渠道（二次确认）。
 *
 * 存在子渠道时后端会拒绝，前端在提示文案中说明。
 */
async function handleDeleteRow(row: ChannelInfo) {
  if (!row.channelCode) return
  await ElMessageBox.confirm(
    `确定删除渠道「${row.fullName}」吗？若存在子渠道后端将拒绝删除。`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteChannelInfo(row.channelCode)
  ElMessage.success('删除成功')
  loadTree()
}

// ---------- 辅助渲染 ----------
/** 渠道类型文案 */
function channelTypeLabel(t?: number): string {
  const found = CHANNEL_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

/** 渠道类型 tag 颜色 */
function channelTypeTagType(t?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (t) {
    case ChannelType.INSURANCE:
      return 'danger'
    case ChannelType.BANK:
      return 'warning'
    case ChannelType.INTERMEDIARY:
      return 'success'
    case ChannelType.ENTERPRISE:
      return 'info'
    default:
      return 'info'
  }
}

/** 是否为管理渠道 tag 颜色：1 管理型 success / 0 业务型 info */
function canManageTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}

/** 是否为管理渠道文案 */
function canManageText(v?: number): string {
  return v === 1 ? '管理型' : '业务型'
}

/** 状态 tag 颜色（DDL 四态：0待审核/1合作中/2已暂停/3已终止） */
function statusTagType(v?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (v) {
    case ChannelStatus.PENDING:
      return 'warning'
    case ChannelStatus.COOPERATING:
      return 'success'
    case ChannelStatus.PAUSED:
      return 'danger'
    case ChannelStatus.TERMINATED:
      return 'info'
    default:
      return 'info'
  }
}

/** 状态文案 */
function statusText(v?: number): string {
  const found = CHANNEL_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

onMounted(() => {
  loadTree()
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="渠道全称">
          <el-input
            v-model="query.fullName"
            placeholder="渠道全称关键字"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="渠道类型">
          <el-select v-model="query.channelType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in CHANNEL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in CHANNEL_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 无管理权限时降级提示 -->
    <el-alert
      v-if="canManageAll !== 1"
      title="当前渠道无配置权限，仅可查看"
      type="warning"
      :closable="false"
      show-icon
    />

    <!-- 树形表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>渠道列表</span>
          <div>
            <el-button :icon="'Refresh'" @click="loadTree">刷新</el-button>
            <el-button v-if="canManageAll === 1" type="primary" :icon="'Plus'" @click="openCreate()">
              新增子渠道
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="treeData"
        border
        stripe
        row-key="channelCode"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="fullName" label="渠道全称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="shortName" label="简称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="channelCode" label="渠道编码" min-width="140" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="channelTypeTagType(row.channelType)">
              {{ channelTypeLabel(row.channelType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="层级" width="80" align="center" />
        <el-table-column label="渠道属性" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="canManageTagType(row.canManage)">
              {{ canManageText(row.canManage) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="canManageAll === 1 && row.canManage === 1">
              <el-button link type="primary" size="small" @click="openCreate(row)">新增子级</el-button>
              <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
            </template>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增子渠道' : '编辑渠道'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="渠道全称" prop="fullName">
              <el-input v-model="form.fullName" placeholder="渠道全称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道简称">
              <el-input v-model="form.shortName" placeholder="渠道简称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道编码">
              <!-- 编辑时 channelCode 不可改，仅展示 -->
              <el-input
                :model-value="form.channelCode"
                placeholder="保存后由系统生成"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道类型" prop="channelType">
              <el-select v-model="form.channelType" placeholder="渠道类型" style="width: 100%">
                <el-option v-for="o in CHANNEL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="社会信用代码">
              <el-input v-model="form.unifiedCreditCode" placeholder="统一社会信用代码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上级渠道">
              <!-- parentCode 默认填当前登录渠道 channelCode，只读展示（架构页仅在本渠道下挂子节点） -->
              <el-input
                :model-value="form.parentCode || '(顶级)'"
                placeholder="上级渠道编码"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactPerson" placeholder="联系人" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="联系电话" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址">
              <el-input v-model="form.address" placeholder="详细地址" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道属性" prop="canManage">
              <el-select v-model="form.canManage" placeholder="渠道属性" style="width: 100%">
                <el-option label="管理型（可管理下级子渠道）" :value="1" />
                <el-option label="业务型（仅业务，无下级管理）" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in CHANNEL_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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

  div {
    display: flex;
    gap: 8px;
  }
}

.text-muted {
  color: var(--el-text-color-placeholder);
}
</style>
