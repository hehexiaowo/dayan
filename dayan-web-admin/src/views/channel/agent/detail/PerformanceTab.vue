<script setup lang="ts">
/**
 * 代理人详情页 - 业绩 tab。
 *
 * 只增不改不删：无 update、无 delete 端点。仅 list(by-agent) + create + summary。
 *
 * 结构：
 * - 顶部汇总卡片：调 getSummary(agentCode) 展示各项合计 + recordCount。
 * - 下方 list（by-agent 返回 List）+ 新增按钮（无编辑/删除按钮）。
 *
 * 关键约束：
 * - create 返回 Void（非 id）。
 * - 周期唯一（agentCode+periodType+periodValue），重复抛"该周期业绩已存在"，前端捕获提示。
 * - periodType 5 态（1日 2周 3月 4季 5年）。
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  listAgentPerformancesByAgent,
  getAgentPerformanceSummary,
  createAgentPerformance
} from '@/api/agent'
import { PERIOD_TYPE_OPTIONS, periodTypeLabel } from '@/types/agent'
import type { AgentPerformance, AgentPerformanceSummary } from '@/types/agent'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 代理人编码（路由参数） */
  agentCode: string
}>()

// ---------- 汇总卡片 ----------
const summary = ref<AgentPerformanceSummary | null>(null)

async function loadSummary() {
  if (!props.agentCode) return
  try {
    summary.value = await getAgentPerformanceSummary(props.agentCode)
  } catch {
    summary.value = null
  }
}

// ---------- 列表（手动 by-agent list，非分页） ----------
const loading = ref(false)
const tableData = ref<AgentPerformance[]>([])

async function loadList() {
  if (!props.agentCode) return
  loading.value = true
  try {
    tableData.value = await listAgentPerformancesByAgent(props.agentCode)
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

async function reload() {
  await Promise.all([loadSummary(), loadList()])
}

reload()

// ---------- 新增弹窗（无编辑） ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Omit<AgentPerformance, 'id'>>({
  agentCode: '',
  channelCode: '',
  periodType: 3,
  periodValue: '',
  equityGrantCount: 0,
  equityGrantAmount: 0,
  sceneOrderCount: 0,
  sceneOrderAmount: 0,
  courseOrderCount: 0,
  courseOrderAmount: 0
})

const rules: FormRules<Omit<AgentPerformance, 'id'>> = {
  periodType: [{ required: true, message: '请选择周期类型', trigger: 'change', type: 'number' }],
  periodValue: [{ required: true, message: '请输入周期值', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    agentCode: '',
    channelCode: '',
    periodType: 3,
    periodValue: '',
    equityGrantCount: 0,
    equityGrantAmount: 0,
    sceneOrderCount: 0,
    sceneOrderAmount: 0,
    courseOrderCount: 0,
    courseOrderAmount: 0
  })
}

function openCreate() {
  resetForm()
  form.agentCode = props.agentCode
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
    // create 返回 Void；周期唯一由后端校验，重复会抛业务异常
    await createAgentPerformance(form)
    ElMessage.success('新增成功')
    dialogVisible.value = false
    reload()
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ reload })
</script>

<template>
  <div class="performance-tab">
    <!-- 汇总卡片 -->
    <el-row :gutter="12" class="summary-row">
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-title">权益赠送</div>
          <div class="summary-value">{{ summary?.totalEquityGrantCount ?? 0 }} 次</div>
          <div class="summary-sub">¥ {{ summary?.totalEquityGrantAmount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-title">场景订单</div>
          <div class="summary-value">{{ summary?.totalSceneOrderCount ?? 0 }} 单</div>
          <div class="summary-sub">¥ {{ summary?.totalSceneOrderAmount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-title">课程订单</div>
          <div class="summary-value">{{ summary?.totalCourseOrderCount ?? 0 }} 单</div>
          <div class="summary-sub">¥ {{ summary?.totalCourseOrderAmount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-title">业绩记录数</div>
          <div class="summary-value">{{ summary?.recordCount ?? 0 }} 条</div>
          <div class="summary-sub">&nbsp;</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button :icon="'Refresh'" @click="reload">刷新</el-button>
      <el-button type="primary" :icon="'Plus'" @click="openCreate">新增业绩</el-button>
      <span class="tip">业绩只增不改不删；周期（代理人+周期类型+周期值）唯一。</span>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column label="周期类型" width="100" align="center">
        <template #default="{ row }">{{ periodTypeLabel(row.periodType) }}</template>
      </el-table-column>
      <el-table-column prop="periodValue" label="周期值" width="120" align="center" />
      <el-table-column prop="equityGrantCount" label="权益赠送次数" width="120" align="center" />
      <el-table-column prop="equityGrantAmount" label="权益赠送金额" width="130" align="center" />
      <el-table-column prop="sceneOrderCount" label="场景订单数" width="110" align="center" />
      <el-table-column prop="sceneOrderAmount" label="场景订单金额" width="130" align="center" />
      <el-table-column prop="courseOrderCount" label="课程订单数" width="110" align="center" />
      <el-table-column prop="courseOrderAmount" label="课程订单金额" width="130" align="center" />
      <el-table-column prop="createdAt" label="创建时间" width="170" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <!-- 只增不改不删：无操作列 -->
    </el-table>

    <!-- 新增弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增业绩"
      width="780px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="代理人编码">
              <el-input v-model="form.agentCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="周期类型" prop="periodType">
              <el-select v-model="form.periodType" style="width: 100%">
                <el-option v-for="o in PERIOD_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="周期值" prop="periodValue">
              <el-input v-model="form.periodValue" placeholder="如 202608" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="权益赠送次数">
              <el-input-number v-model="form.equityGrantCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="权益赠送金额">
              <el-input-number v-model="form.equityGrantAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="场景订单数">
              <el-input-number v-model="form.sceneOrderCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="场景订单金额">
              <el-input-number v-model="form.sceneOrderAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="课程订单数">
              <el-input-number v-model="form.courseOrderCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="课程订单金额">
              <el-input-number v-model="form.courseOrderAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert
          type="info"
          :closable="false"
          title="同一周期（代理人+周期类型+周期值）仅允许一条记录，重复将提示“该周期业绩已存在”。"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.performance-tab {
  .summary-row {
    margin-bottom: 16px;
  }
  .summary-card {
    text-align: center;
    .summary-title {
      color: var(--el-text-color-secondary);
      font-size: 13px;
    }
    .summary-value {
      font-size: 22px;
      font-weight: 600;
      margin-top: 4px;
    }
    .summary-sub {
      color: var(--el-text-color-secondary);
      font-size: 13px;
      margin-top: 2px;
    }
  }
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
