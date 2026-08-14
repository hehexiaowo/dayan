<script setup lang="ts">
/**
 * 客户详情页 - 照护需求 tab。
 *
 * 数据模式：分页（GET /care-needs 带 clientCode 分页），useCrud（idKey:'id', fixedParams:{clientCode}）。
 *
 * 关键约束：
 * - 主键自增 id。
 * - careLevel 后端无 @Schema 枚举文档，暂用 el-input-number 兜底 + TODO。
 * - butlerCode/butlerFullName 关联管家，无 options 文档用 input 兜底。
 * - 偏好类字段（careTypePreference/livingPreference/foodPreference/areaPreference）用 el-input；
 *   长文本（parkRecommendations/evalResult/specialRequirements）用 textarea。
 * - budgetMin/budgetMax 是 BigDecimal；evalDate/expectedCheckinDate 是 date。
 */
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageCareNeeds,
  createCareNeed,
  updateCareNeed,
  deleteCareNeed
} from '@/api/client-sub'
import { listButlers } from '@/api/service'
import type { ClientCareNeed, ClientCareNeedQuery } from '@/types/client'
import type { ButlerInfo } from '@/types/service'

const props = defineProps<{
  /** 客户编码（路由参数） */
  clientCode: string
}>()

// ---------- 列表（useCrud，主键 id） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ClientCareNeed,
  ClientCareNeedQuery,
  number
>(
  {
    page: pageCareNeeds,
    create: createCareNeed,
    update: (id, data) => updateCareNeed(id, data),
    remove: deleteCareNeed
  },
  {
    initialQuery: { careLevel: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { clientCode: props.clientCode }
  }
)

loadPage()

/** 管家下拉选项（watch 联动 butlerFullName 见 form 定义后） */
const butlerOptions = ref<ButlerInfo[]>([])
async function loadButlers() {
  try {
    butlerOptions.value = await listButlers()
  } catch {
    butlerOptions.value = []
  }
}
onMounted(loadButlers)

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ClientCareNeed>({
  id: undefined,
  clientCode: '',
  butlerCode: '',
  butlerFullName: '',
  evalDate: '',
  careLevel: undefined,
  careTypePreference: '',
  livingPreference: '',
  foodPreference: '',
  budgetMin: undefined,
  budgetMax: undefined,
  areaPreference: '',
  specialRequirements: '',
  expectedCheckinDate: '',
  parkRecommendations: '',
  evalResult: '',
  status: 1,
  remark: ''
})

const rules: FormRules<ClientCareNeed> = {}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    clientCode: '',
    butlerCode: '',
    butlerFullName: '',
    evalDate: '',
    careLevel: undefined,
    careTypePreference: '',
    livingPreference: '',
    foodPreference: '',
    budgetMin: undefined,
    budgetMax: undefined,
    areaPreference: '',
    specialRequirements: '',
    expectedCheckinDate: '',
    parkRecommendations: '',
    evalResult: '',
    status: 1,
    remark: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.clientCode = props.clientCode
  dialogVisible.value = true
}

function openEdit(row: ClientCareNeed) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

// 选 butler 后联动回填 butlerFullName（找不到则保留原值，避免编辑回填被清空）
watch(
  () => form.butlerCode,
  (code) => {
    const b = butlerOptions.value.find((x) => x.butlerCode === code)
    if (b) form.butlerFullName = b.fullName || ''
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
    if (dialogMode.value === 'create') {
      await createCareNeed(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateCareNeed(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ClientCareNeed) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该照护需求记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteCareNeed(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
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

defineExpose({ loadPage })
</script>

<template>
  <div class="care-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="照护等级">
        <!-- TODO: careLevel 枚举值待后端补 @Schema 文档后改为 select -->
        <el-input-number v-model="query.careLevel" :min="0" controls-position="right" style="width: 140px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增评估</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="evalDate" label="评估日期" width="120" align="center">
        <template #default="{ row }">{{ formatDate(row.evalDate) }}</template>
      </el-table-column>
      <el-table-column prop="butlerFullName" label="评估管家" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.butlerFullName || row.butlerCode || '--' }}</template>
      </el-table-column>
      <el-table-column prop="careLevel" label="照护等级" width="100" align="center">
        <template #default="{ row }">{{ row.careLevel != null ? row.careLevel : '--' }}</template>
      </el-table-column>
      <el-table-column prop="careTypePreference" label="照护类型偏好" min-width="140" show-overflow-tooltip />
      <el-table-column label="预算" width="140" align="right">
        <template #default="{ row }">
          <span v-if="row.budgetMin != null || row.budgetMax != null">
            {{ row.budgetMin ?? '*' }} ~ {{ row.budgetMax ?? '*' }}
          </span>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="expectedCheckinDate" label="期望入住" width="120" align="center">
        <template #default="{ row }">{{ formatDate(row.expectedCheckinDate) }}</template>
      </el-table-column>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增照护需求' : '编辑照护需求'"
      width="860px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="评估日期">
              <el-date-picker
                v-model="form.evalDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择评估日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <!-- TODO: careLevel 枚举值待后端补 @Schema 文档后改为 select -->
            <el-form-item label="照护等级">
              <el-input-number v-model="form.careLevel" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评估管家">
              <el-select v-model="form.butlerCode" placeholder="选择管家" filterable clearable style="width: 100%">
                <el-option
                  v-for="b in butlerOptions"
                  :key="b.butlerCode"
                  :label="b.fullName || b.butlerCode"
                  :value="b.butlerCode!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管家姓名">
              <el-input v-model="form.butlerFullName" placeholder="选择管家后自动带出" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="照护类型偏好">
              <el-input v-model="form.careTypePreference" placeholder="如 全护理/半护理" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="居住偏好">
              <el-input v-model="form.livingPreference" placeholder="如 单间/双人间" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="餐饮偏好">
              <el-input v-model="form.foodPreference" placeholder="如 素食/低盐" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="区域偏好">
              <el-input v-model="form.areaPreference" placeholder="如 朝阳区" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预算下限">
              <el-input-number v-model="form.budgetMin" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预算上限">
              <el-input-number v-model="form.budgetMax" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期望入住日期">
              <el-date-picker
                v-model="form.expectedCheckinDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择入住日期"
                style="width: 100%"
              />
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
            <el-form-item label="特殊需求">
              <el-input v-model="form.specialRequirements" type="textarea" :rows="2" placeholder="特殊需求" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="推荐机构">
              <el-input v-model="form.parkRecommendations" type="textarea" :rows="2" placeholder="推荐机构" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="评估结果">
              <el-input v-model="form.evalResult" type="textarea" :rows="2" placeholder="评估结果" />
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
.care-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
