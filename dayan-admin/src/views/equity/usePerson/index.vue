<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageEquityUsePersons,
  getEquityUsePerson,
  createEquityUsePerson,
  updateEquityUsePerson,
  deleteEquityUsePerson,
  setDefaultEquityUsePerson
} from '@/api/equity'
import type { EquityUsePerson, EquityUsePersonQuery } from '@/types/equity'
import { RELATION_WITH_HOLDER_OPTIONS, relationLabel } from '@/types/equity'
import { Gender, GENDER_OPTIONS } from '@/types/client'
import { formatDate, formatDateTime, formatOption } from '@/utils/format'

/**
 * 权益人员管理页（CRUD + 设为默认，菜单原名「权益使用人」）。
 *
 * - 搜索 + 表格 + 分页 + 新增/编辑/删除弹窗；
 * - id 为雪花ID，后端序列化为字符串，前端统一按 string 处理（避免精度丢失）；
 * - 「设为默认」走 POST /use-person/set-default，入参 { id, equityCode }，
 *   同 equity_code 下其它使用人置 0；
 * - usePersonGender 复用 Gender（0未知 1男 2女）；
 * - 编辑时 equityCode / clientCode 不可改（后端 UpdateDTO 不含），默认权益人通过「设为默认」切换。
 */

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<EquityUsePerson, EquityUsePersonQuery, string>(
  { page: pageEquityUsePersons },
  {
    idKey: 'id',
    initialQuery: {
      equityCode: '',
      clientCode: '',
      usePersonName: '',
      isDefaultHolder: undefined
    }
  }
)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<EquityUsePerson>({
  id: undefined,
  equityCode: '',
  clientCode: '',
  usePersonName: '',
  usePersonGender: Gender.UNKNOWN,
  usePersonBirthday: '',
  usePersonAge: undefined,
  usePersonPhone: '',
  usePersonIdCard: '',
  relationWithHolder: '',
  healthStatus: '',
  careNeed: '',
  isDefaultHolder: 0,
  remark: ''
})

const rules: FormRules<EquityUsePerson> = {
  equityCode: [{ required: true, message: '请输入权益编码', trigger: 'blur' }],
  clientCode: [{ required: true, message: '请输入权益持有人编码', trigger: 'blur' }],
  usePersonName: [{ required: true, message: '请输入使用人姓名', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    equityCode: '',
    clientCode: '',
    usePersonName: '',
    usePersonGender: Gender.UNKNOWN,
    usePersonBirthday: '',
    usePersonAge: undefined,
    usePersonPhone: '',
    usePersonIdCard: '',
    relationWithHolder: '',
    healthStatus: '',
    careNeed: '',
    isDefaultHolder: 0,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: EquityUsePerson) {
  if (!row.id) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getEquityUsePerson(row.id)
    Object.assign(form, {
      id: detail.id ?? row.id,
      equityCode: detail.equityCode ?? row.equityCode ?? '',
      clientCode: detail.clientCode ?? row.clientCode ?? '',
      usePersonName: detail.usePersonName ?? '',
      usePersonGender: detail.usePersonGender ?? Gender.UNKNOWN,
      usePersonBirthday: detail.usePersonBirthday ?? '',
      usePersonAge: detail.usePersonAge,
      usePersonPhone: detail.usePersonPhone ?? '',
      usePersonIdCard: detail.usePersonIdCard ?? '',
      relationWithHolder: detail.relationWithHolder ?? '',
      healthStatus: detail.healthStatus ?? '',
      careNeed: detail.careNeed ?? '',
      isDefaultHolder: detail.isDefaultHolder ?? 0,
      remark: detail.remark ?? ''
    })
  } catch {
    Object.assign(form, row)
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
      await createEquityUsePerson(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      // 编辑：仅提交 UpdateDTO 包含的可改字段（不含 equityCode/clientCode/isDefaultHolder）
      await updateEquityUsePerson(form.id, {
        usePersonName: form.usePersonName,
        usePersonGender: form.usePersonGender,
        usePersonBirthday: form.usePersonBirthday,
        usePersonAge: form.usePersonAge,
        usePersonPhone: form.usePersonPhone,
        usePersonIdCard: form.usePersonIdCard,
        relationWithHolder: form.relationWithHolder,
        healthStatus: form.healthStatus,
        careNeed: form.careNeed,
        remark: form.remark
      })
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  query.equityCode = ''
  query.clientCode = ''
  query.usePersonName = ''
  query.isDefaultHolder = undefined
  handleSearch()
}

async function handleDeleteRow(row: EquityUsePerson) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除使用人「${row.usePersonName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteEquityUsePerson(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 设为默认 ----------
async function handleSetDefault(row: EquityUsePerson) {
  if (!row.id || !row.equityCode) return
  await ElMessageBox.confirm(
    `确定将「${row.usePersonName}」设为默认权益人吗？（同权益下其它使用人将取消默认）`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await setDefaultEquityUsePerson({ id: row.id, equityCode: row.equityCode })
  ElMessage.success('设置成功')
  loadPage()
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const detail = ref<EquityUsePerson>({})

async function openDetail(row: EquityUsePerson) {
  if (!row.id) {
    detail.value = row
    detailVisible.value = true
    return
  }
  try {
    detail.value = await getEquityUsePerson(row.id)
  } catch {
    detail.value = row
  }
  detailVisible.value = true
}

// ---------- 辅助渲染 ----------
function genderLabel(g?: number): string {
  return g != null ? formatOption(g, GENDER_OPTIONS) : '--'
}

function defaultHolderLabel(v?: number): string {
  return v === 1 ? '是' : v === 0 ? '否' : '--'
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="权益编码">
          <el-input v-model="query.equityCode" placeholder="权益编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="持有人编码">
          <el-input v-model="query.clientCode" placeholder="持有人编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="使用人姓名">
          <el-input v-model="query.usePersonName" placeholder="使用人姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="是否默认">
          <el-select v-model="query.isDefaultHolder" placeholder="全部" clearable style="width: 120px">
            <el-option label="是" :value="1" />
            <el-option label="否" :value="0" />
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
          <span>权益人员列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增权益人员</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="equityCode" label="权益编码" min-width="150" show-overflow-tooltip fixed="left" />
        <el-table-column prop="clientCode" label="持有人编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="usePersonName" label="使用人姓名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="usePersonGender" label="性别" width="80" align="center">
          <template #default="{ row }">{{ genderLabel(row.usePersonGender) }}</template>
        </el-table-column>
        <el-table-column prop="usePersonAge" label="年龄" width="80" align="center" />
        <el-table-column prop="usePersonBirthday" label="出生日期" width="120" align="center">
          <template #default="{ row }">{{ formatDate(row.usePersonBirthday) }}</template>
        </el-table-column>
        <el-table-column prop="usePersonPhone" label="手机号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="usePersonIdCard" label="身份证号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="relationWithHolder" label="与持有人关系" min-width="140" align="center">
          <template #default="{ row }">{{ relationLabel(row.relationWithHolder) }}</template>
        </el-table-column>
        <el-table-column prop="isDefaultHolder" label="默认权益人" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefaultHolder === 1" type="success" size="small">默认</el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.isDefaultHolder !== 1"
              link
              type="success"
              size="small"
              @click="handleSetDefault(row)"
            >
              设为默认
            </el-button>
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
      :title="dialogType === 'create' ? '新增权益使用人' : '编辑权益使用人'"
      width="780px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="权益编码" prop="equityCode">
              <el-input
                v-model="form.equityCode"
                placeholder="权益编码（equityCode）"
                maxlength="50"
                :disabled="dialogType === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="持有人编码" prop="clientCode">
              <el-input
                v-model="form.clientCode"
                placeholder="权益持有人编码（clientCode）"
                maxlength="50"
                :disabled="dialogType === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="使用人姓名" prop="usePersonName">
              <el-input v-model="form.usePersonName" placeholder="使用人姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.usePersonGender" placeholder="性别" style="width: 100%">
                <el-option v-for="o in GENDER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="form.usePersonBirthday"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="出生日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input-number v-model="form.usePersonAge" :min="0" :max="150" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.usePersonPhone" placeholder="手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.usePersonIdCard" placeholder="身份证号（明文，后端加密存储）" maxlength="18" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="与持有人关系">
              <el-select v-model="form.relationWithHolder" placeholder="选择与持有人的关系" style="width: 100%">
                <el-option v-for="o in RELATION_WITH_HOLDER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <div class="form-hint">按权益的权益人构成规则校验席位（配偶≤1、父母≤4、本人唯一）</div>
            </el-form-item>
          </el-col>
          <el-col v-if="dialogType === 'create'" :span="12">
            <el-form-item label="是否默认">
              <el-select v-model="form.isDefaultHolder" placeholder="是否默认权益人" style="width: 100%">
                <el-option label="是" :value="1" />
                <el-option label="否" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="健康状况">
              <el-input v-model="form.healthStatus" placeholder="健康状况简述" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="照护需求">
              <el-input v-model="form.careNeed" placeholder="照护需求简述" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="内部备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="权益使用人详情" width="780px" :close-on-click-modal="false">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="记录ID">{{ detail.id ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="权益编码">{{ detail.equityCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="持有人编码">{{ detail.clientCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="使用人姓名">{{ detail.usePersonName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ genderLabel(detail.usePersonGender) }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ detail.usePersonAge ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ formatDate(detail.usePersonBirthday) }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.usePersonPhone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号" :span="2">{{ detail.usePersonIdCard || '--' }}</el-descriptions-item>
        <el-descriptions-item label="与持有人关系">{{ relationLabel(detail.relationWithHolder) }}</el-descriptions-item>
        <el-descriptions-item label="默认权益人">{{ defaultHolderLabel(detail.isDefaultHolder) }}</el-descriptions-item>
        <el-descriptions-item label="健康状况" :span="2">{{ detail.healthStatus || '--' }}</el-descriptions-item>
        <el-descriptions-item label="照护需求" :span="2">{{ detail.careNeed || '--' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
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

.form-hint {
  font-size: 12px;
  color: #999;
  line-height: 1.4;
}
</style>
