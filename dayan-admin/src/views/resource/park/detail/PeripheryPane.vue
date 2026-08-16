<script setup lang="ts">
/**
 * 机构详情页 - 周边/周边配套子面板。
 *
 * 单表 CRUD：useCrud（idKey:'id', fixedParams:{parkCode}）。
 * 搜索：placeName + peripheryType + status；placeName 必填。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pagePeripheries,
  createPeriphery,
  updatePeriphery,
  deletePeriphery
} from '@/api/park-misc'
import { PERIPHERY_TYPE_OPTIONS, peripheryTypeLabel } from '@/types/park'
import type { ParkPeriphery, ParkPeripheryQuery } from '@/types/park'

const props = defineProps<{
  parkCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkPeriphery,
  ParkPeripheryQuery,
  number
>(
  {
    page: pagePeripheries,
    create: createPeriphery,
    update: (id, data) => updatePeriphery(id, data),
    remove: deletePeriphery
  },
  {
    initialQuery: { peripheryType: undefined, placeName: '', status: undefined },
    idKey: 'id',
    fixedParams: { parkCode: props.parkCode }
  }
)

loadPage()

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ParkPeriphery>({
  id: undefined,
  parkCode: '',
  peripheryType: undefined,
  placeName: '',
  placeAddress: '',
  distance: '',
  detailDescription: '',
  sortOrder: 0,
  status: 1
})

const rules: FormRules<ParkPeriphery> = {
  placeName: [
    { required: true, message: '请输入地点名称', trigger: 'blur' },
    { max: 200, message: '不超过 200 个字符', trigger: 'blur' }
  ]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    parkCode: '',
    peripheryType: undefined,
    placeName: '',
    placeAddress: '',
    distance: '',
    detailDescription: '',
    sortOrder: 0,
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.parkCode = props.parkCode
  dialogVisible.value = true
}

function openEdit(row: ParkPeriphery) {
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
      await createPeriphery(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updatePeriphery(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParkPeriphery) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该周边记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deletePeriphery(row.id)
  ElMessage.success('删除成功')
  loadPage()
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
defineExpose({ loadPage })
</script>

<template>
  <div class="periphery-pane">
    <div class="toolbar">
      <el-input
        v-model="query.placeName"
        placeholder="地点名称"
        clearable
        style="width: 160px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.peripheryType" placeholder="周边类型" clearable style="width: 140px">
        <el-option v-for="o in PERIPHERY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增周边</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="placeName" label="地点名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="peripheryType" label="类型" width="90" align="center">
        <template #default="{ row }">{{ peripheryTypeLabel(row.peripheryType) }}</template>
      </el-table-column>
      <el-table-column prop="placeAddress" label="地址" min-width="180" show-overflow-tooltip />
      <el-table-column prop="distance" label="距离" width="120" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增周边' : '编辑周边'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="地点名称" prop="placeName">
              <el-input v-model="form.placeName" placeholder="地点名称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="周边类型">
              <el-select v-model="form.peripheryType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in PERIPHERY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="距离">
              <el-input v-model="form.distance" placeholder="如 500m / 步行5分钟" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.placeAddress" placeholder="地点地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细描述">
              <el-input v-model="form.detailDescription" type="textarea" :rows="3" placeholder="详细描述" />
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
.periphery-pane {
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
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
