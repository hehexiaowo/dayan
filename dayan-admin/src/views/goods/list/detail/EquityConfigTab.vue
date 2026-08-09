<script setup lang="ts">
/**
 * 商品详情页 - 权益配置 tab（goodsType=1 时显示）。
 *
 * 替代旧 SkuEquityTab，改用 goods_equity(1:1) + goods_service_item_rel(N:M) 架构。
 *
 * 结构：
 * - 上半部：1:1 配置表单（人数/有效天数/库存有效期/可转让/说明）
 * - 下半部：服务项目子表（下拉选 service_item + 数量 + 删除/新增行）
 *
 * 保存逻辑：调 saveGoodsEquity（后端 UPSERT goods_equity + rel 先删后插）。
 * 无分页——1:1 配置只有一条记录，rel 子表全量加载。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGoodsEquity, saveGoodsEquity, deleteGoodsEquity } from '@/api/goods-equity'
import { listServiceItems } from '@/api/service-item'
import {
  ITEM_CATEGORY_OPTIONS,
  ITEM_SUBTYPE_OPTIONS,
  QUOTA_TYPE_OPTIONS
} from '@/types/service-item'
import type { GoodsEquitySaveDTO } from '@/types/goods-equity'
import type { ServiceItem } from '@/types/service-item'

const props = defineProps<{
  /** 商品编码（从详情页 prop 带入） */
  goodsCode: string
}>()

// ---------- 加载状态 ----------
const loading = ref(false)
const saveLoading = ref(false)

// ---------- 1:1 配置表单 ----------
const form = reactive({
  personCount: 1,
  validDays: 365,
  shelfLifeDays: 730,
  maxTransferable: 1,
  description: ''
})

// ---------- 服务项目子表（N:M） ----------
interface RelRow {
  itemCode: string
  quantity: number
  quotaType: number
  sortOrder: number
}

const relRows = ref<RelRow[]>([])

// ---------- 服务项目下拉选项 ----------
const serviceItemOptions = ref<ServiceItem[]>([])

// ---------- 是否已有配置（控制删除按钮） ----------
const configExists = ref(false)

async function loadConfig() {
  loading.value = true
  try {
    // 并行：加载权益配置 + 加载可用服务项目列表
    const [equity, items] = await Promise.all([
      getGoodsEquity(props.goodsCode),
      listServiceItems({ status: 1, size: 999 })
    ])
    serviceItemOptions.value = items || []

    if (equity) {
      configExists.value = true
      form.personCount = equity.personCount
      form.validDays = equity.validDays
      form.shelfLifeDays = equity.shelfLifeDays
      form.maxTransferable = equity.maxTransferable
      form.description = equity.description || ''
      relRows.value = (equity.serviceItems || []).map(r => ({
        itemCode: r.itemCode,
        quantity: r.quantity,
        quotaType: r.quotaType || 2,
        sortOrder: r.sortOrder || 0
      }))
    } else {
      configExists.value = false
      relRows.value = []
    }
  } catch {
    // 404 = 无配置，正常初始态
    configExists.value = false
    relRows.value = []
  } finally {
    loading.value = false
  }
}

function addRelRow() {
  relRows.value.push({ itemCode: '', quantity: 1, quotaType: 2, sortOrder: 0 })
}

function removeRelRow(index: number) {
  relRows.value.splice(index, 1)
}

function getServiceItemName(itemCode: string): string {
  const item = serviceItemOptions.value.find(i => i.itemCode === itemCode)
  return item?.itemName || itemCode
}

async function handleSave() {
  // 校验：rel 行不能有空 itemCode
  for (const row of relRows.value) {
    if (!row.itemCode) {
      ElMessage.warning('请选择所有服务项目（不能有空行）')
      return
    }
  }

  const dto: GoodsEquitySaveDTO = {
    goodsCode: props.goodsCode,
    personCount: form.personCount,
    validDays: form.validDays,
    shelfLifeDays: form.shelfLifeDays,
    maxTransferable: form.maxTransferable,
    description: form.description,
    status: 1,
    serviceItems: relRows.value.map(r => ({
      itemCode: r.itemCode,
      quantity: r.quantity,
      quotaType: r.quotaType,
      sortOrder: r.sortOrder
    }))
  }

  saveLoading.value = true
  try {
    await saveGoodsEquity(dto)
    ElMessage.success('保存成功')
    configExists.value = true
    await loadConfig()
  } finally {
    saveLoading.value = false
  }
}

async function handleDelete() {
  await ElMessageBox.confirm('确定删除该权益配置及所有服务项目关联？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteGoodsEquity(props.goodsCode)
  ElMessage.success('删除成功')
  await loadConfig()
}

onMounted(() => {
  loadConfig()
})
</script>

<template>
  <div class="equity-config-tab" v-loading="loading">
    <!-- 上半部：1:1 配置表单 -->
    <el-card shadow="never" class="config-card">
      <template #header>
        <div class="card-header">
          <span>权益配置</span>
          <el-tag v-if="configExists" size="small" type="success">已配置</el-tag>
          <el-tag v-else size="small" type="info">未配置</el-tag>
        </div>
      </template>
      <el-form :model="form" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="使用人人数">
              <el-input-number v-model="form.personCount" :min="1" :max="10" controls-position="right" style="width: 100%" />
              <div class="field-hint">1=个人版, 2=双人版, 3+=家庭版</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="激活后有效天数">
              <el-input-number v-model="form.validDays" :min="1" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存有效期(天)">
              <el-input-number v-model="form.shelfLifeDays" :min="1" :max="9999" controls-position="right" style="width: 100%" />
              <div class="field-hint">未激活时的库存有效期</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="可转让">
              <el-switch v-model="form.maxTransferable" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="配置说明">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="权益配置说明（选填）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 下半部：服务项目子表 -->
    <el-card shadow="never" class="rel-card">
      <template #header>
        <div class="card-header">
          <span>服务项目组合</span>
          <el-button type="primary" size="small" :icon="'Plus'" @click="addRelRow">添加服务项目</el-button>
        </div>
      </template>

      <el-table :data="relRows" border stripe row-key="itemCode" empty-text="暂无服务项目，点击「添加服务项目」">
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="服务项目" min-width="260">
          <template #default="{ row }">
            <el-select v-model="row.itemCode" placeholder="请选择服务项目" filterable style="width: 100%">
              <el-option
                v-for="item in serviceItemOptions"
                :key="item.itemCode"
                :label="`${item.itemCode} · ${item.itemName}`"
                :value="item.itemCode!"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="大类" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.itemCode" size="small">
              {{ ITEM_CATEGORY_OPTIONS.find(o => o.value === serviceItemOptions.find(i => i.itemCode === row.itemCode)?.itemCategory)?.label || '—' }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="数量/配额" width="120" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" :max="999" controls-position="right" size="small" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="配额周期" width="130" align="center">
          <template #default="{ row }">
            <el-select v-model="row.quotaType" size="small" style="width: 100%">
              <el-option
                v-for="opt in QUOTA_TYPE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="100" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.sortOrder" :min="0" :max="999" controls-position="right" size="small" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="removeRelRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 底部操作按钮 -->
    <div class="actions">
      <el-button type="primary" :loading="saveLoading" @click="handleSave">保存配置</el-button>
      <el-button v-if="configExists" type="danger" plain @click="handleDelete">删除配置</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.equity-config-tab {
  .config-card,
  .rel-card {
    margin-bottom: 16px;
  }
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    justify-content: space-between;
  }
  .field-hint {
    font-size: 12px;
    color: #999;
    line-height: 1.4;
  }
  .actions {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
  }
}
</style>
