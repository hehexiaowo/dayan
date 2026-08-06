<script setup lang="ts">
/**
 * 客户详情页 - 家庭成员 tab。
 *
 * 数据模式：by-client list（GET /family-members/by-client/{clientCode} 返回 List，非分页）。
 * 故采用方案 A：手动 ref<ClientFamilyMember[]> + list 调用 + 增删改后重新 list（不用 useCrud）。
 *
 * 主键：自增 id。gender 用 GENDER_OPTIONS（el-select）；其余布尔字段（isEmergencyContact/
 * isPrimaryContact/isDecisionMaker）用 el-switch 提交 0/1；status 0/1。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listFamilyMembersByClient,
  createFamilyMember,
  updateFamilyMember,
  deleteFamilyMember
} from '@/api/client-sub'
import { GENDER_OPTIONS, Gender } from '@/types/client'
import type { ClientFamilyMember } from '@/types/client'

const props = defineProps<{
  /** 客户编码（路由参数） */
  clientCode: string
}>()

// ---------- 列表（by-client list，非分页） ----------
const loading = ref(false)
const tableData = ref<ClientFamilyMember[]>([])

async function loadList() {
  if (!props.clientCode) return
  loading.value = true
  try {
    tableData.value = await listFamilyMembersByClient(props.clientCode)
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

loadList()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ClientFamilyMember>({
  id: undefined,
  clientCode: '',
  memberName: '',
  relation: '',
  gender: Gender.UNKNOWN,
  phone: '',
  email: '',
  isEmergencyContact: 0,
  isPrimaryContact: 0,
  isDecisionMaker: 0,
  address: '',
  remark: '',
  status: 1,
  sortOrder: 0
})

const rules: FormRules<ClientFamilyMember> = {
  memberName: [{ required: true, message: '请输入成员姓名', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    clientCode: '',
    memberName: '',
    relation: '',
    gender: Gender.UNKNOWN,
    phone: '',
    email: '',
    isEmergencyContact: 0,
    isPrimaryContact: 0,
    isDecisionMaker: 0,
    address: '',
    remark: '',
    status: 1,
    sortOrder: 0
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.clientCode = props.clientCode
  dialogVisible.value = true
}

function openEdit(row: ClientFamilyMember) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
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
      await createFamilyMember(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateFamilyMember(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ClientFamilyMember) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除家庭成员「${row.memberName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteFamilyMember(row.id)
  ElMessage.success('删除成功')
  loadList()
}

// ---------- 辅助渲染 ----------
function genderText(v?: number): string {
  const found = GENDER_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusLabel(v?: number): string {
  return v === 1 ? '启用' : '停用'
}
function statusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}

defineExpose({ loadList })
</script>

<template>
  <div class="family-tab">
    <div class="toolbar">
      <el-button type="primary" :icon="'Plus'" @click="openCreate">新增家庭成员</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="memberName" label="姓名" min-width="100" show-overflow-tooltip />
      <el-table-column prop="relation" label="关系" width="100" show-overflow-tooltip />
      <el-table-column label="性别" width="70" align="center">
        <template #default="{ row }">{{ genderText(row.gender) }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
      <el-table-column label="紧急联系人" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isEmergencyContact === 1" type="danger" size="small">紧急</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="主要联系人" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isPrimaryContact === 1" type="success" size="small">主要</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="决策人" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDecisionMaker === 1" type="warning" size="small">决策</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="120" align="center">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增家庭成员' : '编辑家庭成员'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="memberName">
              <el-input v-model="form.memberName" placeholder="成员姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关系">
              <el-input v-model="form.relation" placeholder="如 父亲/母亲/配偶" maxlength="50" />
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
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="邮箱" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="联系角色">
              <div class="role-switches">
                <el-switch
                  v-model="form.isEmergencyContact"
                  :active-value="1"
                  :inactive-value="0"
                  active-text="紧急联系人"
                />
                <el-switch
                  v-model="form.isPrimaryContact"
                  :active-value="1"
                  :inactive-value="0"
                  active-text="主要联系人"
                />
                <el-switch
                  v-model="form.isDecisionMaker"
                  :active-value="1"
                  :inactive-value="0"
                  active-text="决策人"
                />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.address" placeholder="地址" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
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
.family-tab {
  .toolbar {
    margin-bottom: 16px;
  }
  .role-switches {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 24px;
  }
}
</style>
