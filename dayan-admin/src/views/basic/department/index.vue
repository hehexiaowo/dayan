<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { listDepartments, createDepartment, updateDepartment, deleteDepartment } from '@/api/department'
import { listAllOrgans } from '@/api/organ'
import {
  DepartmentStatus,
  DEPARTMENT_STATUS_OPTIONS,
  DEPT_TYPE_OPTIONS,
  buildDepartmentTree,
  type Department
} from '@/types/department'
import type { OrganSimple } from '@/types/organ'

/**
 * 部门管理页（树形表格）。
 *
 * - 后端 /departments 返回平铺列表，前端用 buildDepartmentTree 组树展示；
 * - 主键为 (organCode, deptCode) 联合键，update/delete 均带 organCode 路径段；
 * - 机构编码通过下拉选择（绑定 /organs/all），留空表示查询全部机构部门。
 */

const organCode = ref('')
const loading = ref(false)
const treeData = ref<Department[]>([])
const flatList = ref<Department[]>([])
const organOptions = ref<OrganSimple[]>([])

async function loadOrgans() {
  try {
    organOptions.value = await listAllOrgans()
  } catch {
    organOptions.value = []
  }
}

async function loadTree() {
  loading.value = true
  try {
    const list = await listDepartments(organCode.value)
    flatList.value = list
    treeData.value = buildDepartmentTree(list)
  } finally {
    loading.value = false
  }
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Department>({
  organCode: '',
  deptCode: '',
  deptName: '',
  parentCode: null,
  ancestors: '',
  deptType: 2,
  leaderName: '',
  leaderPhone: '',
  sortOrder: 0,
  status: DepartmentStatus.ENABLED,
  remark: ''
})

const rules: FormRules = {
  organCode: [{ required: true, message: '请选择机构', trigger: 'change' }],
  deptCode: [
    { required: true, message: '请输入部门编码', trigger: 'blur' },
    { max: 100, message: '部门编码长度不能超过 100', trigger: 'blur' }
  ],
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

/** 供父级选择用的树（基于平铺列表组树） */
const parentTreeOptions = ref<Department[]>([])

function buildParentOptions(excludeCode?: string): Department[] {
  const tree = buildDepartmentTree(flatList.value)
  if (excludeCode) {
    const filterNode = (nodes: Department[]): Department[] => {
      const result: Department[] = []
      for (const n of nodes) {
        if (n.deptCode === excludeCode) continue
        const children = n.children ? filterNode(n.children) : undefined
        result.push(children ? { ...n, children } : { ...n, children: undefined })
      }
      return result
    }
    return filterNode(tree)
  }
  return tree
}

function resetForm() {
  Object.assign(form, {
    organCode: organCode.value,
    deptCode: '',
    deptName: '',
    parentCode: null,
    ancestors: '',
    deptType: 2,
    leaderName: '',
    leaderPhone: '',
    sortOrder: 0,
    status: DepartmentStatus.ENABLED,
    remark: ''
  })
}

function openCreate(parent?: Department) {
  dialogType.value = 'create'
  resetForm()
  if (parent) {
    form.parentCode = parent.deptCode
  }
  parentTreeOptions.value = buildParentOptions()
  dialogVisible.value = true
}

function openEdit(row: Department) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, {
    organCode: row.organCode,
    deptCode: row.deptCode,
    deptName: row.deptName,
    parentCode: row.parentCode,
    ancestors: row.ancestors,
    deptType: row.deptType,
    leaderName: row.leaderName,
    leaderPhone: row.leaderPhone,
    sortOrder: row.sortOrder,
    status: row.status,
    remark: row.remark
  })
  parentTreeOptions.value = buildParentOptions(row.deptCode)
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
      await createDepartment(form)
      ElMessage.success('新增成功')
    } else {
      await updateDepartment(form.organCode, form.deptCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadTree()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: Department) {
  await ElMessageBox.confirm(`确定删除部门「${row.deptName}」吗？若存在子部门将一并影响。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteDepartment(row.organCode, row.deptCode)
  ElMessage.success('删除成功')
  loadTree()
}

function typeText(t?: number) {
  return DEPT_TYPE_OPTIONS.find((o) => o.value === t)?.label ?? (t != null ? String(t) : '-')
}

onMounted(() => {
  loadOrgans()
  loadTree()
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-title">部门列表</span>
            <el-select
              v-model="organCode"
              placeholder="全部机构"
              clearable
              filterable
              style="width: 200px; margin-left: 16px"
              @change="loadTree"
            >
              <el-option
                v-for="o in organOptions"
                :key="o.organCode"
                :label="o.fullName || o.shortName || o.organCode"
                :value="o.organCode"
              />
            </el-select>
          </div>
          <div>
            <el-button :icon="'Refresh'" @click="loadTree">刷新</el-button>
            <el-button type="primary" :icon="'Plus'" @click="openCreate()">新增部门</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="treeData"
        border
        stripe
        row-key="deptCode"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="deptName" label="部门名称" min-width="200" />
        <el-table-column prop="deptCode" label="部门编码" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag>{{ typeText(row.deptType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="leaderName" label="负责人" width="120" />
        <el-table-column prop="leaderPhone" label="联系电话" min-width="130" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openCreate(row)">新增子级</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增部门' : '编辑部门'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="机构" prop="organCode">
              <el-select
                v-model="form.organCode"
                placeholder="请选择机构"
                filterable
                :disabled="dialogType === 'edit'"
                style="width: 100%"
              >
                <el-option
                  v-for="o in organOptions"
                  :key="o.organCode"
                  :label="o.fullName || o.shortName || o.organCode"
                  :value="o.organCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门编码" prop="deptCode">
              <el-input
                v-model="form.deptCode"
                placeholder="部门编码"
                :disabled="dialogType === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="form.deptName" placeholder="部门名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父级部门">
              <el-tree-select
                v-model="form.parentCode"
                :data="parentTreeOptions"
                :props="{ label: 'deptName', value: 'deptCode', children: 'children' }"
                check-strictly
                clearable
                placeholder="顶级（不选）"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门类型">
              <el-select v-model="form.deptType" placeholder="部门类型" style="width: 100%">
                <el-option v-for="o in DEPT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-input v-model="form.leaderName" placeholder="负责人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.leaderPhone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in DEPARTMENT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-left {
    display: flex;
    align-items: center;

    .header-title {
      font-size: 16px;
      font-weight: 500;
    }
  }
}
</style>
