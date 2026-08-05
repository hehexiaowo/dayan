<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageAgents, getAgent, createAgent, updateAgent, deleteAgent } from '@/api/agent'
import { listChannels } from '@/api/channel'
import {
  AgentLevel,
  AGENT_LEVEL_OPTIONS,
  AgentStatus,
  AGENT_STATUS_OPTIONS,
  Gender,
  GENDER_OPTIONS,
  CertifiedFlag,
  CERTIFIED_OPTIONS,
  type AgentInfo,
  type AgentInfoQuery
} from '@/types/agent'
import { buildChannelTree, type ChannelInfo } from '@/types/channel'
import RegionSelect from '@/components/RegionSelect.vue'

/**
 * 代理人管理页。
 *
 * - 标准 CRUD（useCrud + 搜索 + 表格 + 分页 + 弹窗）；
 * - list 接口 url 为 /admin-api/agents（无 /page 后缀），返回 PageResult；
 * - 主键 agentCode 由服务端生成，新增表单不含 agentCode；
 * - 所属渠道 channelCode 用 el-tree-select 选择（数据来自渠道树）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  AgentInfo,
  AgentInfoQuery
>(
  { page: pageAgents },
  {
    initialQuery: {
      agentCode: '',
      channelCode: '',
      fullName: '',
      phone: '',
      agentLevel: undefined,
      isCertified: undefined,
      status: undefined
    }
  }
)

// ---------- 渠道树（供筛选与表单选择） ----------
const channelTree = ref<ChannelInfo[]>([])
async function loadChannelTree() {
  try {
    const list = await listChannels()
    channelTree.value = buildChannelTree(list)
  } catch {
    channelTree.value = []
  }
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<AgentInfo>({
  agentCode: undefined,
  fullName: '',
  gender: Gender.UNKNOWN,
  avatar: '',
  phone: '',
  email: '',
  idCard: '',
  channelCode: '',
  companyName: '',
  branchName: '',
  department: '',
  position: '',
  employeeNo: '',
  licenseNo: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  serviceIntro: '',
  agentLevel: AgentLevel.INTERN,
  isCertified: CertifiedFlag.NO,
  status: AgentStatus.ENABLED,
  remark: ''
})

const rules: FormRules<AgentInfo> = {
  fullName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { pattern: /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/, message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

function resetForm() {
  Object.assign(form, {
    agentCode: undefined,
    fullName: '',
    gender: Gender.UNKNOWN,
    avatar: '',
    phone: '',
    email: '',
    idCard: '',
    channelCode: '',
    companyName: '',
    branchName: '',
    department: '',
    position: '',
    employeeNo: '',
    licenseNo: '',
    provinceCode: '',
    cityCode: '',
    districtCode: '',
    address: '',
    serviceIntro: '',
    agentLevel: AgentLevel.INTERN,
    isCertified: CertifiedFlag.NO,
    status: AgentStatus.ENABLED,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: AgentInfo) {
  if (!row.agentCode) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getAgent(row.agentCode)
    Object.assign(form, {
      agentCode: detail.agentCode,
      fullName: detail.fullName ?? '',
      gender: detail.gender ?? Gender.UNKNOWN,
      avatar: detail.avatar ?? '',
      phone: detail.phone ?? '',
      email: detail.email ?? '',
      idCard: detail.idCard ?? '',
      channelCode: detail.channelCode ?? '',
      companyName: detail.companyName ?? '',
      branchName: detail.branchName ?? '',
      department: detail.department ?? '',
      position: detail.position ?? '',
      employeeNo: detail.employeeNo ?? '',
      licenseNo: detail.licenseNo ?? '',
      provinceCode: detail.provinceCode ?? '',
      cityCode: detail.cityCode ?? '',
      districtCode: detail.districtCode ?? '',
      address: detail.address ?? '',
      serviceIntro: detail.serviceIntro ?? '',
      agentLevel: detail.agentLevel ?? AgentLevel.INTERN,
      isCertified: detail.isCertified ?? CertifiedFlag.NO,
      status: detail.status ?? AgentStatus.ENABLED,
      remark: detail.remark ?? ''
    })
  } catch {
    Object.assign(form, {
      agentCode: row.agentCode,
      fullName: row.fullName ?? '',
      gender: row.gender ?? Gender.UNKNOWN,
      avatar: row.avatar ?? '',
      phone: row.phone ?? '',
      email: row.email ?? '',
      idCard: row.idCard ?? '',
      channelCode: row.channelCode ?? '',
      companyName: row.companyName ?? '',
      branchName: row.branchName ?? '',
      department: row.department ?? '',
      position: row.position ?? '',
      employeeNo: row.employeeNo ?? '',
      licenseNo: row.licenseNo ?? '',
      provinceCode: row.provinceCode ?? '',
      cityCode: row.cityCode ?? '',
      districtCode: row.districtCode ?? '',
      address: row.address ?? '',
      serviceIntro: row.serviceIntro ?? '',
      agentLevel: row.agentLevel ?? AgentLevel.INTERN,
      isCertified: row.isCertified ?? CertifiedFlag.NO,
      status: row.status ?? AgentStatus.ENABLED,
      remark: row.remark ?? ''
    })
  }
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
    if (dialogType.value === 'create') {
      await createAgent(form)
      ElMessage.success('新增成功')
    } else if (form.agentCode) {
      await updateAgent(form.agentCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: AgentInfo) {
  if (!row.agentCode) return
  await ElMessageBox.confirm(`确定删除代理人「${row.fullName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteAgent(row.agentCode)
  ElMessage.success('删除成功')
  loadPage()
}

function handleReset() {
  query.agentCode = ''
  query.channelCode = ''
  query.fullName = ''
  query.phone = ''
  query.agentLevel = undefined
  query.isCertified = undefined
  query.status = undefined
  handleSearch()
}

// ---------- 辅助渲染 ----------
function genderText(v?: number): string {
  if (v === Gender.MALE) return '男'
  if (v === Gender.FEMALE) return '女'
  return '未知'
}

function agentLevelLabel(l?: number): string {
  const found = AGENT_LEVEL_OPTIONS.find((o) => o.value === l)
  return found ? found.label : l != null ? String(l) : '--'
}

function agentLevelTagType(l?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (l) {
    case AgentLevel.GOLD:
      return 'danger'
    case AgentLevel.SENIOR:
      return 'warning'
    case AgentLevel.REGULAR:
      return 'success'
    case AgentLevel.INTERN:
    default:
      return 'info'
  }
}

onMounted(() => {
  loadChannelTree()
  loadPage()
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="所属渠道">
          <el-tree-select
            v-model="query.channelCode"
            :data="channelTree"
            :props="{ label: 'fullName', value: 'channelCode', children: 'children' }"
            check-strictly
            clearable
            placeholder="全部渠道"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.fullName" placeholder="姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="手机号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="query.agentLevel" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in AGENT_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证">
          <el-select v-model="query.isCertified" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in CERTIFIED_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in AGENT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>代理人列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增代理人</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="agentCode">
        <el-table-column prop="agentCode" label="代理人编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="fullName" label="姓名" min-width="100" />
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="channelCode" label="所属渠道" min-width="140" show-overflow-tooltip />
        <el-table-column prop="companyName" label="保险公司" min-width="140" show-overflow-tooltip />
        <el-table-column prop="employeeNo" label="工号" min-width="100" show-overflow-tooltip />
        <el-table-column prop="clientCount" label="客户数" width="80" align="center" />
        <el-table-column prop="totalOrderCount" label="订单数" width="80" align="center" />
        <el-table-column label="等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="agentLevelTagType(row.agentLevel)">{{ agentLevelLabel(row.agentLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="认证" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isCertified === 1 ? 'success' : 'info'">
              {{ row.isCertified === 1 ? '已认证' : '未认证' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
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
      :title="dialogType === 'create' ? '新增代理人' : '编辑代理人'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="fullName">
              <el-input v-model="form.fullName" placeholder="姓名" maxlength="50" />
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
              <el-input v-model="form.phone" placeholder="手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.idCard" placeholder="身份证号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="头像 URL">
              <el-input v-model="form.avatar" placeholder="头像地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属渠道">
              <el-tree-select
                v-model="form.channelCode"
                :data="channelTree"
                :props="{ label: 'fullName', value: 'channelCode', children: 'children' }"
                check-strictly
                clearable
                placeholder="选择渠道"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保险公司">
              <el-input v-model="form.companyName" placeholder="保险公司名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分公司">
              <el-input v-model="form.branchName" placeholder="分公司名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="form.department" placeholder="部门" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-input v-model="form.position" placeholder="职位" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保险公司工号">
              <el-input v-model="form.employeeNo" placeholder="工号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资格证号">
              <el-input v-model="form.licenseNo" placeholder="从业资格证号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所在地区">
              <RegionSelect
                v-model:province-code="form.provinceCode"
                v-model:city-code="form.cityCode"
                v-model:district-code="form.districtCode"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址">
              <el-input v-model="form.address" placeholder="详细地址" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="代理人等级">
              <el-select v-model="form.agentLevel" placeholder="等级" style="width: 100%">
                <el-option v-for="o in AGENT_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否认证">
              <el-select v-model="form.isCertified" style="width: 100%">
                <el-option v-for="o in CERTIFIED_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in AGENT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="服务介绍">
              <el-input v-model="form.serviceIntro" type="textarea" :rows="2" placeholder="服务介绍" />
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
