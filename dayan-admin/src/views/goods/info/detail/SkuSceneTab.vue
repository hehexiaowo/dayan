<script setup lang="ts">
/**
 * 商品详情页 - 场景规格 tab（goodsType=2 时显示）。
 *
 * 分页模式：useCrud（主键 id 自增 number，传 idKey:'id'，fixedParams:{goodsCode}）。
 *
 * 关键约束：
 * - 主键是自增 id（number），update/delete 都用 id。
 * - skuCode 服务端生成 GS 前缀，前端不传。
 * - create 必填：goodsCode（fixedParams 带入）、sceneCode。
 * - parkCode DDL 是 NOT NULL 但 DTO 无 @NotBlank——前端表单把 parkCode 设为必填（rules），避免 DB 报错。
 * - salesCount create 硬编码 0，UpdateDTO 无此字段，表单不展示。
 * - sceneCode 无跨模块选择器文档，暂用 el-input 兜底。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageSkuScenes,
  createSkuScene,
  updateSkuScene,
  deleteSkuScene
} from '@/api/goods-sku'
import {
  SKU_STATUS_OPTIONS,
  skuStatusLabel,
  skuStatusTagType
} from '@/types/goods'
import type { GoodsSkuScene, GoodsSkuSceneQuery } from '@/types/goods'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 商品编码（从详情页 prop 带入，create 表单隐藏） */
  goodsCode: string
}>()

// ---------- 场景规格列表（useCrud，主键 id 自增 number） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<GoodsSkuScene, GoodsSkuSceneQuery, number>(
    {
      page: pageSkuScenes,
      create: createSkuScene,
      update: (id, data) => updateSkuScene(id, data),
      remove: deleteSkuScene
    },
    {
      initialQuery: { skuName: '', sceneCode: '', status: undefined },
      idKey: 'id',
      fixedParams: { goodsCode: props.goodsCode }
    }
  )

loadPage()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<GoodsSkuScene>({
  id: undefined,
  goodsCode: '',
  skuCode: undefined,
  skuName: '',
  sceneCode: '',
  parkCode: '',
  skuPrice: undefined,
  personLimit: undefined,
  durationHours: undefined,
  scheduleDescription: '',
  stock: undefined,
  sortOrder: 0,
  status: 1
})

// parkCode DDL NOT NULL 但 DTO 无 @NotBlank——前端 rules 强制必填避免 DB 报错
const rules: FormRules<GoodsSkuScene> = {
  sceneCode: [{ required: true, message: '请输入场景编码', trigger: 'blur' }],
  parkCode: [{ required: true, message: '请输入园区编码', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    goodsCode: '',
    skuCode: undefined,
    skuName: '',
    sceneCode: '',
    parkCode: '',
    skuPrice: undefined,
    personLimit: undefined,
    durationHours: undefined,
    scheduleDescription: '',
    stock: undefined,
    sortOrder: 0,
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.goodsCode = props.goodsCode
  dialogVisible.value = true
}

function openEdit(row: GoodsSkuScene) {
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
    form.goodsCode = props.goodsCode
    if (dialogMode.value === 'create') {
      await createSkuScene(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateSkuScene(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: GoodsSkuScene) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该场景规格记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteSkuScene(row.id)
  ElMessage.success('删除成功')
  loadPage()
}
</script>

<template>
  <div class="sku-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="规格名称">
        <el-input
          v-model="query.skuName"
          placeholder="规格名称"
          clearable
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="场景编码">
        <el-input
          v-model="query.sceneCode"
          placeholder="场景编码"
          clearable
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in SKU_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增场景规格</el-button>
      </el-form-item>
    </el-form>

    <!-- 主表格 -->
    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="skuCode" label="规格编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="skuName" label="规格名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="sceneCode" label="场景编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="parkCode" label="园区编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="skuPrice" label="SKU 价格" width="110" align="right" />
      <el-table-column prop="personLimit" label="人数上限" width="100" align="center" />
      <el-table-column prop="durationHours" label="时长(小时)" width="110" align="center" />
      <el-table-column prop="stock" label="库存" width="90" align="center" />
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="skuStatusTagType(row.status)" size="small">{{ skuStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
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
      :title="dialogMode === 'create' ? '新增场景规格' : '编辑场景规格'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规格名称">
              <el-input v-model="form.skuName" placeholder="规格名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景编码" prop="sceneCode">
              <!-- TODO: sceneCode 暂无跨模块选择器文档，先用 input 兜底 -->
              <el-input
                v-model="form.sceneCode"
                placeholder="场景编码"
                maxlength="50"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="园区编码" prop="parkCode">
              <!-- parkCode DDL NOT NULL，前端强制必填 -->
              <el-input v-model="form.parkCode" placeholder="园区编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SKU 价格">
              <el-input-number v-model="form.skuPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="人数上限">
              <el-input-number v-model="form.personLimit" :min="0" :max="9999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时长(小时)">
              <el-input-number v-model="form.durationHours" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存">
              <el-input-number v-model="form.stock" :min="0" :max="9999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">在售</el-radio>
                <el-radio :value="0">停售</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="排期说明">
              <el-input v-model="form.scheduleDescription" type="textarea" :rows="2" placeholder="排期说明" />
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
.sku-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
