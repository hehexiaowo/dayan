<script setup lang="ts">
/**
 * 代理人详情页 - 收藏 tab。
 *
 * 数据模式：by-agent list + 幂等 add(POST 返回 id) + remove(DELETE /{id})。无 update。
 *
 * 关键约束：
 * - 幂等 add：重复收藏返回既有 id 不报错（后端保证）。
 * - 主键雪花 id（前端 string 防精度溢出），remove 路径用 id。
 * - targetType 4 态（1养老机构 2场景 3课程 4内容）。
 * - targetCode 按 targetType 指向不同实体，无统一选择器，用 input 兜底 + TODO。
 */
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listAgentFavoritesByAgent,
  createAgentFavorite,
  deleteAgentFavorite
} from '@/api/agent'
import { listAllOrgans } from '@/api/organ'
import { listScenes } from '@/api/scene'
import { listCourses } from '@/api/course'
import { listContents } from '@/api/content'
import {
  FAVORITE_TARGET_TYPE_OPTIONS,
  favoriteTargetTypeLabel
} from '@/types/agent'
import type { AgentFavorite } from '@/types/agent'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 代理人编码（路由参数） */
  agentCode: string
}>()

// ---------- 列表（手动 by-agent list，非分页） ----------
const loading = ref(false)
const tableData = ref<AgentFavorite[]>([])

async function loadList() {
  if (!props.agentCode) return
  loading.value = true
  try {
    tableData.value = await listAgentFavoritesByAgent(props.agentCode)
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

loadList()

// ---------- 新增弹窗（幂等 add，无编辑） ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

// 表单类型：targetType 在 VO 中必填，但表单初始为空，故放宽为可选
type FavoriteForm = Omit<AgentFavorite, 'targetType'> & { targetType?: number }

const form = reactive<FavoriteForm>({
  agentCode: '',
  targetType: undefined,
  targetCode: ''
})

const rules: FormRules<FavoriteForm> = {
  targetType: [{ required: true, message: '请选择收藏对象类型', trigger: 'change', type: 'number' }],
  targetCode: [{ required: true, message: '请输入收藏对象编码', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    agentCode: '',
    targetType: undefined,
    targetCode: ''
  })
}

function openCreate() {
  resetForm()
  form.agentCode = props.agentCode
  dialogVisible.value = true
}

/** 收藏对象下拉（按 targetType 动态切换数据源：1机构 2场景 3课程 4内容） */
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
    // 幂等 add：重复收藏返回既有 id 不报错
    await createAgentFavorite(form)
    ElMessage.success('收藏成功')
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: AgentFavorite) {
  if (!row.id) return
  await ElMessageBox.confirm(
    `确定取消收藏「${favoriteTargetTypeLabel(row.targetType)}：${row.targetCode}」吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteAgentFavorite(row.id)
  ElMessage.success('已取消收藏')
  loadList()
}

defineExpose({ loadList })
</script>

<template>
  <div class="favorite-tab">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button :icon="'Refresh'" @click="loadList">刷新</el-button>
      <el-button type="primary" :icon="'Plus'" @click="openCreate">新增收藏</el-button>
      <span class="tip">收藏为幂等 add（重复不报错）；无编辑，要改先删再加。</span>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column label="对象类型" width="120" align="center">
        <template #default="{ row }">{{ favoriteTargetTypeLabel(row.targetType) }}</template>
      </el-table-column>
      <el-table-column prop="targetCode" label="对象编码" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="收藏时间" width="180" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <!-- 无编辑，仅删除 -->
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增收藏"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="代理人编码">
              <el-input v-model="form.agentCode" disabled />
            </el-form-item>
          </el-col>
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
        </el-row>
        <el-alert
          type="info"
          :closable="false"
          title="收藏为幂等操作：若已收藏相同对象，将返回既有记录（不报错）。"
          show-icon
        />
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
    align-items: center;
    gap: 8px;
    .tip {
      color: var(--el-text-color-secondary);
      font-size: 13px;
      margin-left: 8px;
    }
  }
}
</style>
