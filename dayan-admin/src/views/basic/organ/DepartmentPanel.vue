<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createDepartment, deleteDepartment, listDepartments, updateDepartment } from '@/api/department'
import {
  DepartmentStatus,
  DEPARTMENT_STATUS_OPTIONS,
  DEPT_TYPE_OPTIONS,
  buildDepartmentTree,
  type Department
} from '@/types/department'

const props = defineProps<{
  organCode: string
  organName: string
}>()

const emit = defineEmits<{
  close: []
}>()

const loading = ref(false)
const treeData = ref<Department[]>([])
const flatList = ref<Department[]>([])
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const parentTreeOptions = ref<Department[]>([])

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
  deptCode: [
    { required: true, message: '请输入部门编码', trigger: 'blur' },
    { max: 100, message: '部门编码长度不能超过 100', trigger: 'blur' }
  ],
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const title = computed(() => `${props.organName} - 部门管理`)

async function loadTree() {
  if (!props.organCode) return
  loading.value = true
  try {
    const list = await listDepartments(props.organCode)
    flatList.value = list
    treeData.value = buildDepartmentTree(list)
  } finally {
    loading.value = false
  }
}

function buildParentOptions(excludeCode?: string): Department[] {
  const filterNode = (nodes: Department[]): Department[] => nodes.flatMap((node) => {
    if (node.deptCode === excludeCode) return []
    const children = node.children ? filterNode(node.children) : undefined
    return [{ ...node, children }]
  })
  return filterNode(buildDepartmentTree(flatList.value))
}

function resetForm() {
  Object.assign(form, {
    organCode: props.organCode,
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
  form.parentCode = parent?.deptCode ?? null
  parentTreeOptions.value = buildParentOptions()
  dialogVisible.value = true
}

function openEdit(row: Department) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, row)
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
      await updateDepartment(props.organCode, form.deptCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    await loadTree()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: Department) {
  await ElMessageBox.confirm(`确定删除部门「${row.deptName}」吗？若存在子部门将一并影响。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteDepartment(props.organCode, row.deptCode)
  ElMessage.success('删除成功')
  await loadTree()
}

function typeText(type?: number) {
  return DEPT_TYPE_OPTIONS.find((option) => option.value === type)?.label ?? '-'
}

watch(() => props.organCode, loadTree)
onMounted(loadTree)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">{{ title }}</span>
        <div class="header-actions">
          <el-button :icon="'Refresh'" @click="loadTree">刷新</el-button>
          <el-button v-permission="'organ:dept:create'" type="primary" :icon="'Plus'" @click="openCreate()">新增部门</el-button>
          <el-button :icon="'Back'" @click="emit('close')">返回机构列表</el-button>
        </div>
      </div>
    </template>

    <el-table v-loading="loading" :data="treeData" border stripe row-key="deptCode" :tree-props="{ children: 'children' }" default-expand-all>
      <el-table-column prop="deptName" label="部门名称" min-width="200" />
      <el-table-column prop="deptCode" label="部门编码" min-width="160" show-overflow-tooltip />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }"><el-tag>{{ typeText(row.deptType) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="leaderName" label="负责人" width="120" />
      <el-table-column prop="leaderPhone" label="联系电话" min-width="130" />
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'organ:dept:create'" link type="primary" size="small" @click="openCreate(row)">新增子级</el-button>
          <el-button v-permission="'organ:dept:update'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'organ:dept:delete'" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogType === 'create' ? '新增部门' : '编辑部门'" width="600px" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item label="机构"><el-input :model-value="props.organName" disabled /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="部门编码" prop="deptCode"><el-input v-model="form.deptCode" placeholder="部门编码" :disabled="dialogType === 'edit'" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="部门名称" prop="deptName"><el-input v-model="form.deptName" placeholder="部门名称" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="父级部门"><el-tree-select v-model="form.parentCode" :data="parentTreeOptions" :props="{ label: 'deptName', value: 'deptCode', children: 'children' }" check-strictly clearable placeholder="顶级（不选）" style="width: 100%" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="部门类型"><el-select v-model="form.deptType" style="width: 100%"><el-option v-for="option in DEPT_TYPE_OPTIONS" :key="option.value" :label="option.label" :value="option.value" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="负责人"><el-input v-model="form.leaderName" placeholder="负责人姓名" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.leaderPhone" placeholder="联系电话" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="状态" prop="status"><el-select v-model="form.status" style="width: 100%"><el-option v-for="option in DEPARTMENT_STATUS_OPTIONS" :key="option.value" :label="option.label" :value="option.value" /></el-select></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" /></el-form-item></el-col>
      </el-row>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button></template>
  </el-dialog>
</template>

<style scoped lang="scss">
.card-header, .header-actions { display: flex; align-items: center; }
.card-header { justify-content: space-between; }
.header-actions { gap: 8px; }
.card-title { font-size: 15px; font-weight: 600; color: #1f2329; }
</style>
