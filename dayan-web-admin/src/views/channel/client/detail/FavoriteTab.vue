<script setup lang="ts">
/**
 * 客户详情页 - 收藏 tab。
 *
 * 数据模式：by-client list（GET /client-favorites/by-client/{clientCode} 返回 List，非分页）。
 * 采用方案 A：手动 ref<ClientFavorite[]> + list 调用 + 新增/删除后重新 list。
 *
 * 关键约束：
 * - 后端无 update 端点（收藏不可编辑，要改就先删再加）。
 * - 列表只展示，操作列只有"删除"。新增用 dialog。
 * - targetType 枚举后端无 @Schema 文档，暂用 el-input-number 兜底 + TODO。
 * - targetCode/targetName 必填。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listClientFavoritesByClient,
  createClientFavorite,
  deleteClientFavorite
} from '@/api/client-sub'
import type { ClientFavorite } from '@/types/client'

const props = defineProps<{
  /** 客户编码（路由参数） */
  clientCode: string
}>()

// ---------- 列表（by-client list，非分页） ----------
const loading = ref(false)
const tableData = ref<ClientFavorite[]>([])

async function loadList() {
  if (!props.clientCode) return
  loading.value = true
  try {
    tableData.value = await listClientFavoritesByClient(props.clientCode)
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

loadList()

// ---------- 新增弹窗（无编辑） ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

// 表单类型：targetType 在 VO 中必填，但表单初始为空，故放宽为可选
type FavoriteForm = Omit<ClientFavorite, 'targetType'> & { targetType?: number }

const form = reactive<FavoriteForm>({
  id: undefined,
  clientCode: '',
  targetType: undefined,
  targetCode: '',
  targetName: '',
  remark: ''
})

const rules: FormRules<ClientFavorite> = {
  targetType: [{ required: true, message: '请输入收藏对象类型', trigger: 'blur' }],
  targetCode: [{ required: true, message: '请输入收藏对象编码', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    clientCode: '',
    targetType: undefined,
    targetCode: '',
    targetName: '',
    remark: ''
  })
}

function openCreate() {
  resetForm()
  form.clientCode = props.clientCode
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
    await createClientFavorite(form)
    ElMessage.success('收藏成功')
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ClientFavorite) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定取消收藏「${row.targetName || row.targetCode}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteClientFavorite(row.id)
  ElMessage.success('已取消收藏')
  loadList()
}

// ---------- 辅助渲染 ----------
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}

defineExpose({ loadList })
</script>

<template>
  <div class="favorite-tab">
    <div class="toolbar">
      <el-button type="primary" :icon="'Plus'" @click="openCreate">新增收藏</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="targetType" label="对象类型" width="110" align="center">
        <template #default="{ row }">
          <!-- TODO: targetType 枚举值待后端补 @Schema 文档后改为 select -->
          {{ row.targetType != null ? row.targetType : '--' }}
        </template>
      </el-table-column>
      <el-table-column prop="targetCode" label="对象编码" min-width="160" show-overflow-tooltip />
      <el-table-column prop="targetName" label="对象名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="收藏时间" width="120" align="center">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <!-- 收藏无编辑，仅删除 -->
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增收藏"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <!-- TODO: targetType 枚举值待后端补 @Schema 文档后改为 select -->
            <el-form-item label="对象类型" prop="targetType">
              <el-input-number v-model="form.targetType" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对象编码" prop="targetCode">
              <el-input v-model="form.targetCode" placeholder="收藏对象编码" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="对象名称">
              <el-input v-model="form.targetName" placeholder="收藏对象名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">收藏</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.favorite-tab {
  .toolbar {
    margin-bottom: 16px;
  }
}
</style>
