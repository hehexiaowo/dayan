<script setup lang="ts">
/**
 * 管家详情页 - 服务记录 tab。
 *
 * 分页模式：useCrud（主键雪花 string，idKey:'id'，泛型 K=string，fixedParams:{butlerCode}）。
 *
 * 关键约束：
 * - 主键雪花 id 用 string（防精度溢出）。
 * - status：3 态（0进行中/1已完成/2已取消）。新增时显式传 status（不依赖后端默认，DDL 默认 0 但 service 默认 1）。
 *   默认 0 进行中。
 * - serviceType：4 态（1需求评估/2方案定制/3全程安排/4回访品控）。
 * - communicateWay：5 态（1电话/2企业微信/3微信/4当面沟通/5其他）。
 * - clientCode 无客户选择器文档，用 input 兜底 + TODO。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageButlerServiceRecords,
  createButlerServiceRecord,
  updateButlerServiceRecord,
  deleteButlerServiceRecord
} from '@/api/service'
import {
  BUTLER_SERVICE_TYPE_OPTIONS,
  SERVICE_RECORD_STATUS_OPTIONS,
  COMMUNICATE_WAY_OPTIONS,
  butlerServiceTypeLabel,
  serviceRecordStatusLabel,
  serviceRecordStatusTagType,
  communicateWayLabel
} from '@/types/service'
import type { ButlerServiceRecord, ButlerServiceRecordQuery } from '@/types/service'
import { formatDateTime, formatDate } from '@/utils/format'
import { useClientPicker } from '@/composables/useClientPicker'

const props = defineProps<{
  /** 管家编码（路由参数） */
  butlerCode: string
}>()

// ---------- 客户远程搜索选择器（pageClients 数据源） ----------
const { clientOptions, clientLoading, searchClients, ensureClient } = useClientPicker()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<ButlerServiceRecord, ButlerServiceRecordQuery, string>(
    {
      page: pageButlerServiceRecords,
      create: createButlerServiceRecord,
      update: (id, data) => updateButlerServiceRecord(id, data),
      remove: (id) => deleteButlerServiceRecord(id)
    },
    {
      initialQuery: { clientCode: '', serviceType: undefined, status: undefined },
      idKey: 'id',
      fixedParams: { butlerCode: props.butlerCode }
    }
  )

loadPage()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ButlerServiceRecord>({
  id: undefined,
  butlerCode: '',
  clientCode: '',
  serviceType: 1,
  serviceTitle: '',
  serviceDate: '',
  status: 0,
  communicateWay: undefined,
  remark: ''
})

const rules: FormRules<ButlerServiceRecord> = {
  clientCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }],
  serviceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }],
  serviceTitle: [{ required: true, message: '请输入服务标题', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    butlerCode: '',
    clientCode: '',
    serviceType: 1,
    serviceTitle: '',
    serviceDate: '',
    status: 0,
    communicateWay: undefined,
    remark: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.butlerCode = props.butlerCode
  dialogVisible.value = true
}

function openEdit(row: ButlerServiceRecord) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    butlerCode: row.butlerCode,
    clientCode: row.clientCode ?? '',
    serviceType: row.serviceType ?? 1,
    serviceTitle: row.serviceTitle ?? '',
    serviceDate: row.serviceDate ?? '',
    status: row.status ?? 0,
    communicateWay: row.communicateWay,
    remark: row.remark ?? ''
  })
  // 编辑回填时确保已选客户出现在候选列表（否则只显示 code）
  ensureClient(row.clientCode)
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
      // 新增时显式传 status（不依赖后端默认）
      await createButlerServiceRecord({ ...form, status: form.status ?? 0 })
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateButlerServiceRecord(form.id, { ...form })
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ButlerServiceRecord) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除服务记录「${row.serviceTitle}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteButlerServiceRecord(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

defineExpose({ loadPage })
</script>

<template>
  <div class="record-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="客户编码">
        <el-input v-model="query.clientCode" placeholder="客户编码" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="服务类型">
        <el-select v-model="query.serviceType" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in BUTLER_SERVICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in SERVICE_RECORD_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增记录</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="clientCode" label="客户编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="serviceTitle" label="服务标题" min-width="160" show-overflow-tooltip />
      <el-table-column prop="serviceType" label="服务类型" width="110" align="center">
        <template #default="{ row }">{{ butlerServiceTypeLabel(row.serviceType) }}</template>
      </el-table-column>
      <el-table-column prop="serviceDate" label="服务日期" width="120" align="center">
        <template #default="{ row }">{{ formatDate(row.serviceDate) }}</template>
      </el-table-column>
      <el-table-column prop="communicateWay" label="沟通方式" width="110" align="center">
        <template #default="{ row }">{{ communicateWayLabel(row.communicateWay) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="serviceRecordStatusTagType(row.status)" size="small">
            {{ serviceRecordStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
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
      :title="dialogMode === 'create' ? '新增服务记录' : '编辑服务记录'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="管家编码">
              <el-input v-model="form.butlerCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户" prop="clientCode">
              <el-select
                v-model="form.clientCode"
                filterable
                remote
                clearable
                :remote-method="searchClients"
                :loading="clientLoading"
                placeholder="输入客户姓名搜索"
                style="width: 100%"
              >
                <el-option
                  v-for="c in clientOptions"
                  :key="c.clientCode"
                  :label="`${c.fullName}（${c.clientCode}）`"
                  :value="c.clientCode!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务类型" prop="serviceType">
              <el-select v-model="form.serviceType" style="width: 100%">
                <el-option v-for="o in BUTLER_SERVICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in SERVICE_RECORD_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="服务标题" prop="serviceTitle">
              <el-input v-model="form.serviceTitle" placeholder="服务标题" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务日期">
              <el-date-picker
                v-model="form.serviceDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="沟通方式">
              <el-select v-model="form.communicateWay" clearable placeholder="选择沟通方式" style="width: 100%">
                <el-option v-for="o in COMMUNICATE_WAY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.record-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
