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
 * - targetType 按 DDL 09_client.sql 注释用 el-select（1=养老机构, 2=场景, 3=课程, 4=内容）。
 * - targetCode/targetName 必填。
 */
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listClientFavoritesByClient,
  createClientFavorite,
  deleteClientFavorite
} from '@/api/client-sub'
import { listAllOrgans } from '@/api/organ'
import { listScenes } from '@/api/scene'
import { listCourses } from '@/api/course'
import { listContents } from '@/api/content'
import { FAVORITE_TARGET_TYPE_OPTIONS, favoriteTargetTypeLabel } from '@/types/agent'
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
  targetType: [{ required: true, message: '请选择收藏对象类型', trigger: 'change' }],
  targetCode: [{ required: true, message: '请输入收藏对象编码', trigger: 'change' }]
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

/** 收藏对象下拉（按 targetType 动态切换：1机构 2场景 3课程 4内容） */
interface TargetOption {
  code: string
  name: string
}
const targetOptions = ref<TargetOption[]>([])
async function loadTargetOptions(targetType?: number) {
  if (!targetType) {
    targetOptions.value = []
    return
  }
  try {
    let opts: TargetOption[] = []
    if (targetType === 1) {
      opts = (await listAllOrgans()).map((o) => ({ code: o.organCode, name: o.fullName || o.shortName || o.organCode }))
    } else if (targetType === 2) {
      opts = (await listScenes()).map((s) => ({ code: s.sceneCode!, name: s.sceneName || s.sceneCode! }))
    } else if (targetType === 3) {
      opts = (await listCourses()).map((c) => ({ code: c.courseCode!, name: c.courseName || c.courseCode! }))
    } else if (targetType === 4) {
      opts = (await listContents()).map((c) => ({ code: c.contentCode!, name: c.title || c.contentCode! }))
    }
    targetOptions.value = opts
  } catch {
    targetOptions.value = []
  }
}
watch(
  () => form.targetType,
  (t) => {
    form.targetCode = ''
    loadTargetOptions(t)
  }
)

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
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增收藏</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="targetType" label="对象类型" width="110" align="center">
        <template #default="{ row }">{{ favoriteTargetTypeLabel(row.targetType) }}</template>
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
            <el-form-item label="对象类型" prop="targetType">
              <el-select v-model="form.targetType" placeholder="选择类型" style="width: 100%">
                <el-option v-for="o in FAVORITE_TARGET_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对象" prop="targetCode">
              <el-select
                v-model="form.targetCode"
                :placeholder="form.targetType ? '选择对象' : '请先选择类型'"
                :disabled="!form.targetType"
                filterable
                style="width: 100%"
              >
                <el-option v-for="o in targetOptions" :key="o.code" :label="o.name" :value="o.code" />
              </el-select>
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
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;

    .toolbar-actions {
      display: flex;
      gap: 8px;
      margin-left: auto;
    }
  }
}
</style>
