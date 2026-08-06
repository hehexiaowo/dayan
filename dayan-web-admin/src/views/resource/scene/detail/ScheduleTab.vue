<script setup lang="ts">
/**
 * 场景详情页 - 活动日程 tab。
 *
 * 架构（useCrud 分页 + 弹窗 CRUD）：
 * 1. 搜索条（status + scheduleDate）+ 新增按钮
 * 2. 主表格：useCrud（idKey:'id', fixedParams:{sceneCode}）分页加载
 * 3. 新增/编辑 el-dialog：
 *    - status 用 el-select 5 态（DDL 权威，VO 注释只 3 态是过时）
 *    - currentPerson 编辑时 disabled（与 maxPerson 容量校验联动，后端有乐观锁自动状态机）
 *    - scheduleDate 用 el-date-picker；startTime/endTime 用 el-time-picker
 *    - priceOverride 是金额字段：el-input-number，min=0、precision=2
 *
 * 状态色（表格 tag）：
 * - 已取消(0) info / 可预约(1) success / 已约满(2) warning / 进行中(3) primary / 已结束(4) danger
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageSceneSchedules,
  createSceneSchedule,
  updateSceneSchedule,
  deleteSceneSchedule
} from '@/api/scene-sub'
import { SCENE_SCHEDULE_STATUS_OPTIONS } from '@/types/scene'
import type { SceneSchedule, SceneScheduleQuery } from '@/types/scene'

const props = defineProps<{
  /** 场景编码（从详情页 prop 带入，create 表单自动携带） */
  sceneCode: string
}>()

// ---------- 列表（useCrud，主键 id） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  SceneSchedule,
  SceneScheduleQuery,
  number
>(
  {
    page: pageSceneSchedules,
    create: createSceneSchedule,
    update: (id, data) => updateSceneSchedule(id, data),
    remove: deleteSceneSchedule
  },
  {
    initialQuery: { status: undefined, scheduleDate: undefined },
    idKey: 'id',
    fixedParams: { sceneCode: props.sceneCode }
  }
)

loadPage()

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SceneSchedule>({
  id: undefined,
  sceneCode: '',
  scheduleDate: '',
  startTime: '',
  endTime: '',
  maxPerson: 0,
  currentPerson: 0,
  priceOverride: undefined,
  remark: '',
  status: 1
})

const rules: FormRules<SceneSchedule> = {
  scheduleDate: [{ required: true, message: '请选择活动日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  maxPerson: [{ required: true, message: '请输入最大人数', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    sceneCode: '',
    scheduleDate: '',
    startTime: '',
    endTime: '',
    maxPerson: 0,
    currentPerson: 0,
    priceOverride: undefined,
    remark: '',
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.sceneCode = props.sceneCode
  dialogVisible.value = true
}

function openEdit(row: SceneSchedule) {
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
      await createSceneSchedule(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateSceneSchedule(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: SceneSchedule) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除该日程（${row.scheduleDate}）吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteSceneSchedule(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function scheduleStatusLabel(v?: number): string {
  const found = SCENE_SCHEDULE_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/**
 * 状态 tag 色映射：
 * - 已取消(0) info / 可预约(1) success / 已约满(2) warning / 进行中(3) primary / 已结束(4) danger
 */
function scheduleStatusTagType(v?: number): 'success' | 'info' | 'warning' | 'danger' | 'primary' {
  switch (v) {
    case 0:
      return 'info'
    case 1:
      return 'success'
    case 2:
      return 'warning'
    case 3:
      return 'primary'
    case 4:
      return 'danger'
    default:
      return 'info'
  }
}

function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}

defineExpose({ loadPage })
</script>

<template>
  <div class="schedule-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in SCENE_SCHEDULE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="活动日期">
        <el-date-picker
          v-model="query.scheduleDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增日程</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="scheduleDate" label="活动日期" width="120" align="center">
        <template #default="{ row }">{{ formatDate(row.scheduleDate) }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="100" align="center" />
      <el-table-column prop="endTime" label="结束时间" width="100" align="center" />
      <el-table-column prop="maxPerson" label="最大人数" width="100" align="center" />
      <el-table-column prop="currentPerson" label="已预约" width="100" align="center" />
      <el-table-column prop="priceOverride" label="覆盖价格" width="110" align="right">
        <template #default="{ row }">{{ row.priceOverride != null ? row.priceOverride : '--' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="scheduleStatusTagType(row.status)" size="small">
            {{ scheduleStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增日程' : '编辑日程'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="活动日期" prop="scheduleDate">
              <el-date-picker
                v-model="form.scheduleDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in SCENE_SCHEDULE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker
                v-model="form.startTime"
                value-format="HH:mm:ss"
                placeholder="选择时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-time-picker
                v-model="form.endTime"
                value-format="HH:mm:ss"
                placeholder="选择时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大人数" prop="maxPerson">
              <el-input-number v-model="form.maxPerson" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已预约">
              <el-input-number
                v-model="form.currentPerson"
                :min="0"
                :max="form.maxPerson || 0"
                controls-position="right"
                style="width: 100%"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="覆盖价格">
              <el-input-number
                v-model="form.priceOverride"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
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

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
