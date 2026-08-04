<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageEmployees,
  createEmployee,
  updateEmployee,
  deleteEmployee
} from '@/api/employee'
import { listDepartments } from '@/api/department'
import type { Employee, EmployeeQuery } from '@/types/employee'
import { EmployeeStatus, EMPLOYEE_STATUS_OPTIONS } from '@/types/employee'
import { buildDepartmentTree, type Department } from '@/types/department'

/**
 * 员工管理页。
 *
 * - CRUD 标准模式（useCrud）；
 * - 部门 deptCode 选择用 el-tree-select，数据来自部门列表（按 organCode 拉取并组树）；
 * - 主键为 (organCode, employeeCode) 联合键，update/delete 均带 organCode 路径段。
 */

const DEFAULT_ORGAN_CODE = 'DAYAN'

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  Employee,
  EmployeeQuery
>(
  {
    page: pageEmployees
  },
  {
    initialQuery: {
      organCode: DEFAULT_ORGAN_CODE,
      deptCode: '',
      realName: '',
      phone: '',
      employeeStatus: undefined
    }
  }
)

// ---------- 部门树（供筛选与表单选择） ----------
const deptTree = ref<Department[]>([])
async function loadDeptTree() {
  try {
    const list = await listDepartments(DEFAULT_ORGAN_CODE)
    deptTree.value = buildDepartmentTree(list)
  } catch {
    deptTree.value = []
  }
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Employee>({
  organCode: DEFAULT_ORGAN_CODE,
  employeeCode: undefined,
  accountCode: '',
  deptCode: '',
  realName: '',
  gender: 0,
  phone: '',
  email: '',
  idCard: '',
  position: '',
  entryDate: '',
  leaveDate: '',
  avatar: '',
  employeeStatus: EmployeeStatus.ACTIVE,
  remark: ''
})

const rules: FormRules<Employee> = {
  organCode: [{ required: true, message: '请输入机构编码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  employeeStatus: [{ required: true, message: '请选择员工状态', trigger: 'change' }],
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
    organCode: DEFAULT_ORGAN_CODE,
    employeeCode: undefined,
    accountCode: '',
    deptCode: '',
    realName: '',
    gender: 0,
    phone: '',
    email: '',
    idCard: '',
    position: '',
    entryDate: '',
    leaveDate: '',
    avatar: '',
    employeeStatus: EmployeeStatus.ACTIVE,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Employee) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, {
    organCode: row.organCode,
    employeeCode: row.employeeCode,
    accountCode: row.accountCode,
    deptCode: row.deptCode,
    realName: row.realName,
    gender: row.gender,
    phone: row.phone,
    email: row.email,
    idCard: row.idCard,
    position: row.position,
    entryDate: row.entryDate,
    leaveDate: row.leaveDate,
    avatar: row.avatar,
    employeeStatus: row.employeeStatus,
    remark: row.remark
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
    if (dialogType.value === 'create') {
      await createEmployee(form)
      ElMessage.success('新增成功')
    } else if (form.employeeCode) {
      await updateEmployee(form.organCode, form.employeeCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: Employee) {
  if (!row.employeeCode) return
  await ElMessageBox.confirm(`确定删除员工「${row.realName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteEmployee(row.organCode, row.employeeCode)
  ElMessage.success('删除成功')
  loadPage()
}

function handleReset() {
  query.realName = ''
  query.phone = ''
  query.deptCode = ''
  query.employeeStatus = undefined
  query.organCode = DEFAULT_ORGAN_CODE
  handleSearch()
}

function genderText(v?: number) {
  if (v === 1) return '男'
  if (v === 2) return '女'
  return '未知'
}

onMounted(() => {
  loadDeptTree()
  loadPage()
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="机构">
          <el-input v-model="query.organCode" placeholder="机构编码" clearable />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="query.deptCode"
            :data="deptTree"
            :props="{ label: 'deptName', value: 'deptCode', children: 'children' }"
            check-strictly
            clearable
            placeholder="全部部门"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="真实姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="手机号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.employeeStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in EMPLOYEE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>员工列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增员工</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="employeeCode">
        <el-table-column prop="employeeCode" label="员工编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="deptCode" label="部门编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="position" label="职位" min-width="120" />
        <el-table-column prop="entryDate" label="入职日期" width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.employeeStatus === 1 ? 'success' : 'info'">
              {{ row.employeeStatus === 1 ? '在职' : '离职' }}
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
      :title="dialogType === 'create' ? '新增员工' : '编辑员工'"
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
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-tree-select
                v-model="form.deptCode"
                :data="deptTree"
                :props="{ label: 'deptName', value: 'deptCode', children: 'children' }"
                check-strictly
                clearable
                placeholder="选择部门"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联账号">
              <el-input v-model="form.accountCode" placeholder="账号编码（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="性别" style="width: 100%">
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
                <el-option label="未知" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-input v-model="form.position" placeholder="职位" />
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
            <el-form-item label="入职日期">
              <el-date-picker
                v-model="form.entryDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="离职日期">
              <el-date-picker
                v-model="form.leaveDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="员工状态" prop="employeeStatus">
              <el-select v-model="form.employeeStatus" style="width: 100%">
                <el-option v-for="o in EMPLOYEE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
