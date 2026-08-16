<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  pageStateMachines,
  createStateMachine,
  updateStateMachine,
  deleteStateMachine
} from '@/api/stateMachine'
import {
  type SystemStateMachine,
  type StateMachineQuery,
  STATE_MACHINE_BIZ_TYPE_OPTIONS
} from '@/types/stateMachine'

const loading = ref(false)
const tableData = ref<SystemStateMachine[]>([])
const total = ref(0)

/** 业务类型 value → 中文 label 映射 */
const bizTypeLabel = (value: string): string =>
  STATE_MACHINE_BIZ_TYPE_OPTIONS.find((o) => o.value === value)?.label ?? value

const query = reactive<StateMachineQuery>({
  machineCode: '',
  bizType: '',
  current: 1,
  size: 20
})

/** 拉取分页数据 */
async function loadData() {
  loading.value = true
  try {
    const res = await pageStateMachines({ ...query })
    tableData.value = res.records
    total.value = res.total
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  loadData()
}

function handlePageChange(page: number) {
  query.current = page
  loadData()
}

function handleSizeChange(size: number) {
  query.size = size
  query.current = 1
  loadData()
}

function handleReset() {
  query.machineCode = ''
  query.bizType = ''
  query.current = 1
  loadData()
}

// ---------------- 新增/编辑弹窗 ----------------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | undefined>(undefined)
const formRef = ref<FormInstance>()
const submitting = ref(false)

function defaultForm(): SystemStateMachine {
  return {
    machineCode: '',
    machineName: '',
    bizType: 'order',
    fromState: 0,
    fromStateName: '',
    fromSubState: null,
    toState: 0,
    toStateName: '',
    toSubState: null,
    eventCode: '',
    eventName: '',
    conditionExpr: null,
    actionBean: null,
    sortOrder: 0,
    status: 1,
    remark: null
  }
}

const form = reactive<SystemStateMachine>(defaultForm())

const rules: FormRules<SystemStateMachine> = {
  machineCode: [{ required: true, message: '请输入状态机编码', trigger: 'blur' }],
  machineName: [{ required: true, message: '请输入状态机名称', trigger: 'blur' }],
  bizType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  fromState: [{ required: true, message: '请输入起始状态码', trigger: 'blur' }],
  toState: [{ required: true, message: '请输入目标状态码', trigger: 'blur' }],
  eventCode: [{ required: true, message: '请输入触发事件编码', trigger: 'blur' }]
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = undefined
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEdit(row: SystemStateMachine) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, defaultForm(), row)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createStateMachine({ ...form })
      ElMessage.success('新增成功')
    } else if (editingId.value !== undefined) {
      await updateStateMachine(editingId.value, { ...form })
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: SystemStateMachine) {
  if (row.id === undefined) {
    ElMessage.warning('记录缺少主键 id，无法删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除状态机规则「${row.fromStateName || row.fromState} → ${row.toStateName || row.toState}」？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户取消
  }
  await deleteStateMachine(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="sm-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input
          v-model="query.machineCode"
          placeholder="状态机编码"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="query.bizType"
          placeholder="业务类型"
          clearable
          style="width: 160px"
        >
          <el-option
            v-for="item in STATE_MACHINE_BIZ_TYPE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">状态机规则</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增规则</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        empty-text="暂无状态规则"
      >
        <el-table-column prop="machineCode" label="状态机编码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="machineName" label="状态机名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="业务类型" width="100" align="center">
          <template #default="{ row }">
            {{ bizTypeLabel(row.bizType) }}
          </template>
        </el-table-column>
        <el-table-column label="状态迁移" min-width="220">
          <template #default="{ row }">
            <span class="state-flow">
              <el-tag type="info" size="small">{{ row.fromStateName || row.fromState }}</el-tag>
              <el-icon class="arrow"><ArrowRight /></el-icon>
              <el-tag type="success" size="small">{{ row.toStateName || row.toState }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="触发事件" min-width="140">
          <template #default="{ row }">
            <span>{{ row.eventName || row.eventCode }}</span>
            <span class="event-code">{{ row.eventCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
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
      :title="dialogMode === 'create' ? '新增状态机规则' : '编辑状态机规则'"
      width="680px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        label-position="right"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态机编码" prop="machineCode">
              <el-input v-model="form.machineCode" placeholder="如 ORDER_SM（大写）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态机名称" prop="machineName">
              <el-input v-model="form.machineName" placeholder="如 订单状态机" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="业务类型" prop="bizType">
              <el-select v-model="form.bizType" placeholder="选择业务类型" style="width: 100%">
                <el-option
                  v-for="item in STATE_MACHINE_BIZ_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">状态迁移</el-divider>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="起始状态码" prop="fromState">
              <el-input-number
                v-model="form.fromState"
                :min="0"
                :step="1"
                :precision="0"
                controls-position="right"
                placeholder="状态枚举值"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="起始状态名">
              <el-input v-model="form.fromStateName" placeholder="如 待支付" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="目标状态码" prop="toState">
              <el-input-number
                v-model="form.toState"
                :min="0"
                :step="1"
                :precision="0"
                controls-position="right"
                placeholder="状态枚举值"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标状态名">
              <el-input v-model="form.toStateName" placeholder="如 已支付" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="事件编码" prop="eventCode">
              <el-input v-model="form.eventCode" placeholder="如 pay" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="事件名称">
              <el-input v-model="form.eventName" placeholder="如 支付" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">高级配置（可选）</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="起始子状态">
              <el-input v-model="form.fromSubState" placeholder="子状态值（可空）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标子状态">
              <el-input v-model="form.toSubState" placeholder="子状态值（可空）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="条件表达式">
          <el-input v-model="form.conditionExpr" placeholder="流转条件表达式（可空）" />
        </el-form-item>
        <el-form-item label="执行器 Bean">
          <el-input v-model="form.actionBean" placeholder="流转执行器 bean 名（可空）" />
        </el-form-item>

        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.sm-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      font-size: 15px;
      font-weight: 600;
      color: #1f2329;
    }
  }

  .search-card {
    :deep(.el-card__body) {
      padding-bottom: 2px;
    }
  }

  .toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;

    .toolbar-actions {
      display: flex;
      gap: 8px;
      margin-left: auto;
    }
  }

  .state-flow {
    display: inline-flex;
    align-items: center;
    gap: 6px;

    .arrow {
      color: #909399;
    }
  }

  .event-code {
    margin-left: 6px;
    font-size: 12px;
    color: #8a8f99;
  }

  .pager {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
