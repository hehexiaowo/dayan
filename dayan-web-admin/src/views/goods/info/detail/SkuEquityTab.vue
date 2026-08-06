<script setup lang="ts">
/**
 * 商品详情页 - 权益规格 tab（goodsType=1 时显示）。
 *
 * 分页模式：useCrud（主键 id 自增 number，传 idKey:'id'，fixedParams:{goodsCode}）。
 *
 * 关键约束：
 * - 主键是自增 id（number），update/delete 都用 id。
 * - skuCode 服务端生成 GE 前缀，前端不传。
 * - create 必填：goodsCode（fixedParams 带入）、templateCode。
 * - salesCount create 硬编码 0，UpdateDTO 无此字段，表单不展示。
 * - equityType 枚举无文档（DDL 只写"权益类型"），暂用 el-input-number 兜底。
 * - templateCode 无跨模块选择器文档，暂用 el-input 兜底。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageSkuEquities,
  createSkuEquity,
  updateSkuEquity,
  deleteSkuEquity
} from '@/api/goods-sku'
import {
  SKU_STATUS_OPTIONS,
  skuStatusLabel,
  skuStatusTagType
} from '@/types/goods'
import type { GoodsSkuEquity, GoodsSkuEquityQuery } from '@/types/goods'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 商品编码（从详情页 prop 带入，create 表单隐藏） */
  goodsCode: string
}>()

// ---------- 权益规格列表（useCrud，主键 id 自增 number） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<GoodsSkuEquity, GoodsSkuEquityQuery, number>(
    {
      page: pageSkuEquities,
      create: createSkuEquity,
      update: (id, data) => updateSkuEquity(id, data),
      remove: deleteSkuEquity
    },
    {
      initialQuery: { skuName: '', templateCode: '', status: undefined },
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

const form = reactive<GoodsSkuEquity>({
  id: undefined,
  goodsCode: '',
  skuCode: undefined,
  skuName: '',
  templateCode: '',
  equityType: undefined,
  equityValue: '',
  skuPrice: undefined,
  stock: undefined,
  specDescription: '',
  sortOrder: 0,
  status: 1
})

const rules: FormRules<GoodsSkuEquity> = {
  templateCode: [{ required: true, message: '请输入权益模板编码', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    goodsCode: '',
    skuCode: undefined,
    skuName: '',
    templateCode: '',
    equityType: undefined,
    equityValue: '',
    skuPrice: undefined,
    stock: undefined,
    specDescription: '',
    sortOrder: 0,
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  // goodsCode 从上下文带入
  form.goodsCode = props.goodsCode
  dialogVisible.value = true
}

function openEdit(row: GoodsSkuEquity) {
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
    // 确保 goodsCode 从上下文带入（编辑时也不可改关联键）
    form.goodsCode = props.goodsCode
    if (dialogMode.value === 'create') {
      await createSkuEquity(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateSkuEquity(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: GoodsSkuEquity) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该权益规格记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteSkuEquity(row.id)
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
      <el-form-item label="模板编码">
        <el-input
          v-model="query.templateCode"
          placeholder="权益模板编码"
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
        <el-button :icon="'Plus'" @click="openCreate">新增权益规格</el-button>
      </el-form-item>
    </el-form>

    <!-- 主表格 -->
    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="skuCode" label="规格编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="skuName" label="规格名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="templateCode" label="权益模板编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="equityType" label="权益类型" width="100" align="center">
        <template #default="{ row }">{{ row.equityType != null ? row.equityType : '--' }}</template>
      </el-table-column>
      <el-table-column prop="equityValue" label="权益值" min-width="120" show-overflow-tooltip />
      <el-table-column prop="skuPrice" label="SKU 价格" width="110" align="right" />
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
      :title="dialogMode === 'create' ? '新增权益规格' : '编辑权益规格'"
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
            <el-form-item label="权益模板编码" prop="templateCode">
              <!-- TODO: templateCode 暂无跨模块选择器文档，先用 input 兜底 -->
              <el-input
                v-model="form.templateCode"
                placeholder="权益模板编码"
                maxlength="50"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权益类型">
              <!-- TODO: equityType 枚举无文档（DDL 只写"权益类型"），暂用 input-number 兜底 -->
              <el-input-number v-model="form.equityType" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权益值">
              <el-input v-model="form.equityValue" placeholder="权益值" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="SKU 价格">
              <el-input-number v-model="form.skuPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
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
            <el-form-item label="规格描述">
              <el-input v-model="form.specDescription" type="textarea" :rows="2" placeholder="规格描述" />
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
