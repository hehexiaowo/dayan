<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageClients, getClient, createClient, updateClient, deleteClient } from '@/api/client'
import { listChannels } from '@/api/channel'
import {
  ClientLevel,
  CLIENT_LEVEL_OPTIONS,
  ClientStatus,
  CLIENT_STATUS_OPTIONS,
  clientStatusLabel,
  clientStatusTagType,
  Gender,
  GENDER_OPTIONS,
  EDUCATION_OPTIONS,
  VipFlag,
  VIP_OPTIONS,
  type ClientInfo,
  type ClientInfoQuery
} from '@/types/client'
import { buildChannelTree, type ChannelInfo } from '@/types/channel'
import RegionSelect from '@/components/RegionSelect.vue'

/**
 * 客户管理页。
 *
 * - 标准 CRUD（useCrud + 搜索 + 表格 + 分页 + 弹窗）；
 * - list 接口 url 为 /admin-api/clients（无 /page 后缀），返回 PageResult；
 * - 主键 clientCode 由服务端生成，新增表单不含 clientCode；
 * - 表单仅取 15 个核心字段（fullName/gender/phone/birthday/idCard/provinceCode/cityCode/
 *   districtCode/address/education/clientLevel/isVip/status/channelCode/email/remark）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ClientInfo,
  ClientInfoQuery
>(
  { page: pageClients },
  {
    initialQuery: {
      channelCode: '',
      clientCode: '',
      fullName: '',
      phone: '',
      gender: undefined,
      clientLevel: undefined,
      isVip: undefined,
      status: undefined
    }
  }
)

// ---------- 渠道树（供筛选与表单选择） ----------
const channelTree = ref<ChannelInfo[]>([])

const router = useRouter()
/** 跳转到客户详情页（携带 clientCode） */
function goDetail(row: ClientInfo) {
  if (!row.clientCode) return
  router.push({ path: `/channel/client/detail/${row.clientCode}` })
}
async function loadChannelTree() {
  try {
    const list = await listChannels()
    channelTree.value = buildChannelTree(list)
  } catch {
    channelTree.value = []
  }
}

/** 渠道编码→名称映射（递归渠道树，列表回显用；后端 VO 不带 channelName） */
const channelNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  const walk = (nodes: ChannelInfo[]) => {
    for (const n of nodes) {
      if (n.channelCode) map[n.channelCode] = n.fullName || n.shortName || n.channelCode
      if (n.children) walk(n.children)
    }
  }
  walk(channelTree.value)
  return map
})

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ClientInfo>({
  clientCode: undefined,
  channelCode: '',
  fullName: '',
  gender: Gender.UNKNOWN,
  avatar: '',
  birthday: '',
  idCard: '',
  phone: '',
  email: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  education: undefined,
  clientLevel: ClientLevel.NORMAL,
  isVip: VipFlag.NO,
  status: ClientStatus.ENABLED,
  remark: ''
})

const rules: FormRules<ClientInfo> = {
  fullName: [{ required: true, message: '请输入客户姓名', trigger: 'blur' }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { pattern: /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/, message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

function resetForm() {
  Object.assign(form, {
    clientCode: undefined,
    channelCode: '',
    fullName: '',
    gender: Gender.UNKNOWN,
    avatar: '',
    birthday: '',
    idCard: '',
    phone: '',
    email: '',
    provinceCode: '',
    cityCode: '',
    districtCode: '',
    address: '',
    education: undefined,
    clientLevel: ClientLevel.NORMAL,
    isVip: VipFlag.NO,
    status: ClientStatus.ENABLED,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: ClientInfo) {
  if (!row.clientCode) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getClient(row.clientCode)
    Object.assign(form, {
      clientCode: detail.clientCode,
      channelCode: detail.channelCode ?? '',
      fullName: detail.fullName ?? '',
      gender: detail.gender ?? Gender.UNKNOWN,
      avatar: detail.avatar ?? '',
      birthday: detail.birthday ?? '',
      idCard: detail.idCard ?? '',
      phone: detail.phone ?? '',
      email: detail.email ?? '',
      provinceCode: detail.provinceCode ?? '',
      cityCode: detail.cityCode ?? '',
      districtCode: detail.districtCode ?? '',
      address: detail.address ?? '',
      education: detail.education,
      clientLevel: detail.clientLevel ?? ClientLevel.NORMAL,
      isVip: detail.isVip ?? VipFlag.NO,
      status: detail.status ?? ClientStatus.ENABLED,
      remark: detail.remark ?? ''
    })
  } catch {
    Object.assign(form, {
      clientCode: row.clientCode,
      channelCode: row.channelCode ?? '',
      fullName: row.fullName ?? '',
      gender: row.gender ?? Gender.UNKNOWN,
      avatar: row.avatar ?? '',
      birthday: row.birthday ?? '',
      idCard: row.idCard ?? '',
      phone: row.phone ?? '',
      email: row.email ?? '',
      provinceCode: row.provinceCode ?? '',
      cityCode: row.cityCode ?? '',
      districtCode: row.districtCode ?? '',
      address: row.address ?? '',
      education: row.education,
      clientLevel: row.clientLevel ?? ClientLevel.NORMAL,
      isVip: row.isVip ?? VipFlag.NO,
      status: row.status ?? ClientStatus.ENABLED,
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
      await createClient(form)
      ElMessage.success('新增成功')
    } else if (form.clientCode) {
      await updateClient(form.clientCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: ClientInfo) {
  if (!row.clientCode) return
  await ElMessageBox.confirm(`确定删除客户「${row.fullName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteClient(row.clientCode)
  ElMessage.success('删除成功')
  loadPage()
}

function handleReset() {
  query.channelCode = ''
  query.clientCode = ''
  query.fullName = ''
  query.phone = ''
  query.gender = undefined
  query.clientLevel = undefined
  query.isVip = undefined
  query.status = undefined
  handleSearch()
}

// ---------- 辅助渲染 ----------
function genderText(v?: number): string {
  if (v === Gender.MALE) return '男'
  if (v === Gender.FEMALE) return '女'
  return '未知'
}

function clientLevelLabel(l?: number): string {
  const found = CLIENT_LEVEL_OPTIONS.find((o) => o.value === l)
  return found ? found.label : l != null ? String(l) : '--'
}

function clientLevelTagType(l?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (l) {
    case ClientLevel.DIAMOND:
      return 'danger'
    case ClientLevel.GOLD:
      return 'warning'
    case ClientLevel.SILVER:
      return 'success'
    case ClientLevel.NORMAL:
    default:
      return 'info'
  }
}

function educationLabel(e?: number): string {
  const found = EDUCATION_OPTIONS.find((o) => o.value === e)
  return found ? found.label : e != null ? String(e) : '--'
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
          <el-input v-model="query.fullName" placeholder="客户姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="手机号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="query.gender" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="o in GENDER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="query.clientLevel" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in CLIENT_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="VIP">
          <el-select v-model="query.isVip" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="o in VIP_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in CLIENT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>客户列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增客户</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="clientCode">
        <el-table-column prop="clientCode" label="客户编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="fullName" label="姓名" min-width="100" />
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column label="所属渠道" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ channelNameMap[row.channelCode] || row.channelCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="70" align="center" />
        <el-table-column prop="birthday" label="生日" width="110" align="center" />
        <el-table-column label="学历" width="90" align="center">
          <template #default="{ row }">{{ educationLabel(row.education) }}</template>
        </el-table-column>
        <el-table-column label="等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="clientLevelTagType(row.clientLevel)">{{ clientLevelLabel(row.clientLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="VIP" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isVip === 1 ? 'danger' : 'info'">
              {{ row.isVip === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="equityCount" label="权益" width="70" align="center" />
        <el-table-column prop="serviceCount" label="服务" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="clientStatusTagType(row.status)">
              {{ clientStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
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

    <!-- 新增 / 编辑弹窗（15 核心字段） -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增客户' : '编辑客户'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="fullName">
              <el-input v-model="form.fullName" placeholder="客户姓名" maxlength="50" />
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
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="性别" style="width: 100%">
                <el-option v-for="o in GENDER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生日">
              <el-date-picker
                v-model="form.birthday"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择生日"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.idCard" placeholder="身份证号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历">
              <el-select v-model="form.education" placeholder="学历" clearable style="width: 100%">
                <el-option v-for="o in EDUCATION_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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
          <el-col :span="8">
            <el-form-item label="客户等级">
              <el-select v-model="form.clientLevel" style="width: 100%">
                <el-option v-for="o in CLIENT_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否 VIP">
              <el-select v-model="form.isVip" style="width: 100%">
                <el-option v-for="o in VIP_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in CLIENT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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
