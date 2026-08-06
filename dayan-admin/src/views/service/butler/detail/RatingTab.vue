<script setup lang="ts">
/**
 * 管家详情页 - 评价 tab。
 *
 * 分页模式：useCrud（主键雪花 string，idKey:'id'，泛型 K=string，fixedParams:{butlerCode}）。
 *
 * 关键约束：
 * - 主键雪花 id 用 string（防精度溢出）。
 * - rating：1-5 整数（后端无范围校验，前端必须卡，用 el-rate max=5）。
 * - status：0已隐藏/1正常。
 * - serviceRecordCode 是悬空字段（无对应实体编码），用 input 兜底 + TODO。
 * - update 只改 rating/content/status，clientCode/serviceRecordCode 不可改（编辑时禁用）。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageButlerRatings,
  createButlerRating,
  updateButlerRating,
  deleteButlerRating
} from '@/api/service'
import {
  RATING_STATUS_OPTIONS,
  ratingStatusLabel,
  ratingStatusTagType
} from '@/types/service'
import type { ButlerRating, ButlerRatingQuery } from '@/types/service'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 管家编码（路由参数） */
  butlerCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<ButlerRating, ButlerRatingQuery, string>(
    {
      page: pageButlerRatings,
      create: createButlerRating,
      update: (id, data) => updateButlerRating(id, data),
      remove: (id) => deleteButlerRating(id)
    },
    {
      initialQuery: { clientCode: '', rating: undefined, status: undefined },
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

const form = reactive<ButlerRating>({
  id: undefined,
  butlerCode: '',
  clientCode: '',
  serviceRecordCode: '',
  rating: 5,
  content: '',
  status: 1
})

const rules: FormRules<ButlerRating> = {
  clientCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }],
  rating: [{ required: true, message: '请选择评分', trigger: 'change', type: 'number' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    butlerCode: '',
    clientCode: '',
    serviceRecordCode: '',
    rating: 5,
    content: '',
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.butlerCode = props.butlerCode
  dialogVisible.value = true
}

function openEdit(row: ButlerRating) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    butlerCode: row.butlerCode,
    clientCode: row.clientCode ?? '',
    serviceRecordCode: row.serviceRecordCode ?? '',
    rating: row.rating ?? 5,
    content: row.content ?? '',
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 前端卡评分范围 1-5（后端无范围校验）
  const r = Number(form.rating)
  if (!Number.isFinite(r) || r < 1 || r > 5) {
    ElMessage.warning('评分必须在 1-5 之间')
    return
  }
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createButlerRating({ ...form, rating: Math.trunc(r) })
      ElMessage.success('新增成功')
    } else if (form.id) {
      // update 只改 rating/content/status
      await updateButlerRating(form.id, {
        rating: Math.trunc(r),
        content: form.content,
        status: form.status
      })
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ButlerRating) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除该评价吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteButlerRating(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

defineExpose({ loadPage })
</script>

<template>
  <div class="rating-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="客户编码">
        <el-input v-model="query.clientCode" placeholder="客户编码" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="评分">
        <el-select v-model="query.rating" placeholder="全部" clearable style="width: 100px">
          <el-option :value="5" label="5 分" />
          <el-option :value="4" label="4 分" />
          <el-option :value="3" label="3 分" />
          <el-option :value="2" label="2 分" />
          <el-option :value="1" label="1 分" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in RATING_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增评价</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="clientCode" label="客户编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="serviceRecordCode" label="服务记录编码" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.serviceRecordCode || '--' }}</template>
      </el-table-column>
      <el-table-column prop="rating" label="评分" width="160" align="center">
        <template #default="{ row }">
          <el-rate :model-value="row.rating" disabled show-score :max="5" />
        </template>
      </el-table-column>
      <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="ratingStatusTagType(row.status)" size="small">
            {{ ratingStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
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
      :title="dialogMode === 'create' ? '新增评价' : '编辑评价'"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="管家编码">
              <el-input v-model="form.butlerCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户编码" prop="clientCode">
              <!-- 编辑时 clientCode 不可改（update 不含 clientCode） -->
              <el-input
                v-model="form.clientCode"
                :disabled="dialogMode === 'edit'"
                placeholder="客户编码"
                maxlength="64"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="服务记录编码">
              <!-- TODO: 后端无对应实体编码，可手填或留空；编辑时不可改（update 不含 serviceRecordCode） -->
              <el-input
                v-model="form.serviceRecordCode"
                :disabled="dialogMode === 'edit'"
                placeholder="关联服务记录编码（可选，后端无对应实体）"
                maxlength="64"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评分" prop="rating">
              <!-- el-rate 取值 0-5，后端无范围校验，前端必须卡 1-5 -->
              <el-rate v-model="form.rating" :max="5" show-score />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in RATING_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="评价内容">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="3"
                placeholder="客户评价内容"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="24">
            <el-alert
              type="info"
              :closable="false"
              title="编辑仅可修改评分、评价内容与状态；客户编码与服务记录编码不可改。"
              show-icon
            />
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
.rating-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
