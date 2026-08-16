<script setup lang="ts">
/**
 * 商品详情页 - 场景配置 tab（goodsType=2 时显示）。
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageScenes,
  createScene,
  updateScene,
  deleteScene
} from '@/api/goods-sku'
import { listScenes } from '@/api/scene'
import { listParks } from '@/api/park'
import {
  SKU_STATUS_OPTIONS,
  skuStatusLabel,
  skuStatusTagType
} from '@/types/goods'
import type { GoodsScene, GoodsSceneQuery } from '@/types/goods'
import type { SceneInfo } from '@/types/scene'
import type { ParkInfo } from '@/types/park'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 商品编码（从详情页 prop 带入，create 表单隐藏） */
  goodsCode: string
}>()

// ---------- 场景配置列表（useCrud，主键 id 自增 number） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<GoodsScene, GoodsSceneQuery, number>(
    {
      page: pageScenes,
      create: createScene,
      update: (id, data) => updateScene(id, data),
      remove: deleteScene
    },
    {
      initialQuery: { skuName: '', sceneCode: '', status: undefined },
      idKey: 'id',
      fixedParams: { goodsCode: props.goodsCode }
    }
  )

loadPage()

/** 场景/机构下拉选项 + 名称映射（后端 VO 不带名称，前端自行映射） */
const sceneOptions = ref<SceneInfo[]>([])
const parkOptions = ref<ParkInfo[]>([])
const sceneNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  for (const s of sceneOptions.value) {
    if (s.sceneCode) map[s.sceneCode] = s.sceneName || s.sceneCode
  }
  return map
})
const parkNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  for (const p of parkOptions.value) {
    if (p.parkCode) map[p.parkCode] = p.fullName || p.shortName || p.parkCode
  }
  return map
})
async function loadOptions() {
  try {
    const [scenes, parks] = await Promise.all([listScenes(), listParks()])
    sceneOptions.value = scenes
    parkOptions.value = parks
  } catch {
    sceneOptions.value = []
    parkOptions.value = []
  }
}
onMounted(loadOptions)

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<GoodsScene>({
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
const rules: FormRules<GoodsScene> = {
  sceneCode: [{ required: true, message: '请输入场景编码', trigger: 'blur' }],
  parkCode: [{ required: true, message: '请输入园区编码', trigger: 'blur' }],
  // sku_price DDL NOT NULL
  skuPrice: [{ required: true, message: '请输入 SKU 价格', trigger: 'blur' }]
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

function openEdit(row: GoodsScene) {
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
      await createScene(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateScene(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: GoodsScene) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该场景配置记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteScene(row.id)
  ElMessage.success('删除成功')
  loadPage()
}
</script>

<template>
  <div class="sku-tab">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input
        v-model="query.skuName"
        placeholder="规格名称"
        clearable
        style="width: 150px"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="query.sceneCode"
        placeholder="场景编码"
        clearable
        style="width: 140px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option v-for="o in SKU_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增场景配置</el-button>
      </div>
    </div>

    <!-- 主表格 -->
    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="skuCode" label="规格编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="skuName" label="规格名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="场景" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ sceneNameMap[row.sceneCode] || row.sceneCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="机构" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ parkNameMap[row.parkCode] || row.parkCode || '-' }}</template>
      </el-table-column>
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
      :title="dialogMode === 'create' ? '新增场景配置' : '编辑场景配置'"
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
            <el-form-item label="场景" prop="sceneCode">
              <el-select
                v-model="form.sceneCode"
                placeholder="选择场景"
                filterable
                :disabled="dialogMode === 'edit'"
                style="width: 100%"
              >
                <el-option
                  v-for="s in sceneOptions"
                  :key="s.sceneCode"
                  :label="s.sceneName || s.sceneCode"
                  :value="s.sceneCode!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构" prop="parkCode">
              <el-select v-model="form.parkCode" placeholder="选择机构" filterable clearable style="width: 100%">
                <el-option
                  v-for="p in parkOptions"
                  :key="p.parkCode"
                  :label="p.fullName || p.shortName || p.parkCode"
                  :value="p.parkCode!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SKU 价格" prop="skuPrice">
              <el-input-number v-model="form.skuPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="人数上限">
              <el-input-number v-model="form.personLimit" :min="0" :max="9999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <!-- duration_hours DDL DECIMAL(4,1) -->
            <el-form-item label="时长(小时)">
              <el-input-number v-model="form.durationHours" :min="0" :precision="1" controls-position="right" style="width: 100%" />
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
