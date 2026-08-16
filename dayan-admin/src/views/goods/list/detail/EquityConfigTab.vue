<script setup lang="ts">
/**
 * 商品详情页 - 权益配置 tab（goodsType=1 时显示）。
 *
 * 架构：goods_equity(1:1) + goods_service_item_rel(N:M)。
 *
 * 上半部（权益级，管「人」和「期限」）：
 * - 权益期限（固定天数/终身）+ 激活后有效天数
 * - 权益人构成（本人固定1 + 配偶 + 双方父母N席，自动算总人数）
 * - 配额共享方式（共享池/按人独立）
 * - 可转让次数（0=不可，N=可转让N次）
 *
 * 下半部（商品×服务级，管「次数、权利、范围、用法」）：
 * - 次数 + 配额周期（每年按激活周年重置 / 权益期内总量）
 * - 展开行：入住权（保证/优先/优惠）+ 折扣率 + 服务网络（全部/自选机构）
 *   + 单次使用规则（随心住类：晚数/间数/人数/预订/预定金/取消政策/黑名单）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGoodsEquity, saveGoodsEquity, deleteGoodsEquity } from '@/api/goods-equity'
import { listServiceItems } from '@/api/service-item'
import { ITEM_CATEGORY_OPTIONS, QUOTA_TYPE_OPTIONS } from '@/types/service-item'
import {
  VALIDITY_TYPE_OPTIONS,
  SHARE_MODE_OPTIONS,
  networkScopeSummary,
  type GoodsEquitySaveDTO,
  type NetworkScope,
  type UsageRule,
  type RefundRule,
} from '@/types/goods-equity'
import type { ServiceItem } from '@/types/service-item'
import NetworkScopeSelector from '@/components/NetworkScopeSelector.vue'

const props = defineProps<{
  /** 商品编码（从详情页 prop 带入） */
  goodsCode: string
}>()

// ---------- 加载状态 ----------
const loading = ref(false)
const saveLoading = ref(false)

// ---------- 1:1 配置表单（权益级） ----------
const form = reactive({
  validityType: 1,          // 1=固定天数 2=终身
  validDays: 365,
  shelfLifeDays: 730,
  shareMode: 1,             // 1=共享池 0=按人独立
  maxTransferable: 0,       // 可转让次数（0=不可）
  description: ''
})

// 权益人构成：本人固定1 + 配偶(0/1) + 父母(0~4)
const holder = reactive({
  spouse: 0,
  parent: 0,
  designateAtActivation: false
})

/** 总人数 = 1 + 配偶 + 父母（由构成自动计算，后端校验一致） */
const personCount = computed(() => 1 + holder.spouse + holder.parent)

// ---------- 服务项目子表（N:M，商品×服务级权益内容） ----------
interface RelRow {
  itemCode: string
  quantity: number
  quotaType: number
  sortOrder: number
  /** 商品级网络收窄（null=继承服务项目的网络范围） */
  networkScope: NetworkScope | null
  // 入住权
  admissionGuaranteed: number
  admissionPriority: number
  admissionDiscount: number
  discountRate: number | null | undefined
  // 单次使用规则（随心住类）
  usageEnabled: boolean
  usageRule: UsageRule
}

/** 取消政策默认档位（文档口径：72h全退/48h退50%/24h不退） */
function defaultRefundPolicy(): RefundRule[] {
  return [
    { beforeHours: 72, refundRate: 100 },
    { beforeHours: 48, refundRate: 50 },
    { beforeHours: 24, refundRate: 0 }
  ]
}

function defaultUsageRule(): UsageRule {
  return {
    maxDaysPerUse: 3,
    maxNightsPerUse: 2,
    maxRoomsPerUse: 1,
    maxGuestsPerUse: 2,
    requireBeneficiaryCheckIn: true,
    advanceBookDays: 15,
    depositAmount: 500,
    refundPolicy: defaultRefundPolicy(),
    blackoutType: 'spring_festival',
    blackoutDays: 9
  }
}

const relRows = ref<RelRow[]>([])

// ---------- 下拉选项 ----------
const serviceItemOptions = ref<ServiceItem[]>([])

// ---------- 是否已有配置（控制删除按钮） ----------
const configExists = ref(false)

async function loadConfig() {
  loading.value = true
  try {
    const [equity, items] = await Promise.all([
      getGoodsEquity(props.goodsCode),
      listServiceItems({ status: 1, size: 999 })
    ])
    serviceItemOptions.value = items || []

    if (equity) {
      configExists.value = true
      form.validityType = equity.validityType ?? 1
      form.validDays = equity.validDays
      form.shelfLifeDays = equity.shelfLifeDays
      form.shareMode = equity.shareMode ?? 1
      form.maxTransferable = equity.maxTransferable ?? 0
      form.description = equity.description || ''
      holder.spouse = equity.holderRule?.spouse ?? 0
      holder.parent = equity.holderRule?.parent ?? 0
      holder.designateAtActivation = equity.holderRule?.designateAtActivation ?? false
      relRows.value = (equity.serviceItems || []).map(r => ({
        itemCode: r.itemCode,
        quantity: r.quantity,
        quotaType: r.quotaType || 2,
        sortOrder: r.sortOrder || 0,
        networkScope: r.networkScope?.mode === 'custom' && r.networkScope.parks?.length
          ? r.networkScope
          : null,
        admissionGuaranteed: r.admissionGuaranteed ?? 0,
        admissionPriority: r.admissionPriority ?? 0,
        admissionDiscount: r.admissionDiscount ?? 0,
        discountRate: r.discountRate ?? null,
        usageEnabled: !!r.usageRule,
        usageRule: r.usageRule ? { ...defaultUsageRule(), ...r.usageRule } : defaultUsageRule()
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
  relRows.value.push({
    itemCode: '',
    quantity: 1,
    quotaType: 2,
    sortOrder: 0,
    networkScope: null,
    admissionGuaranteed: 0,
    admissionPriority: 0,
    admissionDiscount: 0,
    discountRate: null,
    usageEnabled: false,
    usageRule: defaultUsageRule()
  })
}

function removeRelRow(index: number) {
  relRows.value.splice(index, 1)
}

function addRefundRow(row: RelRow) {
  if (!row.usageRule.refundPolicy) row.usageRule.refundPolicy = []
  row.usageRule.refundPolicy.push({ beforeHours: 24, refundRate: 0 })
}

function removeRefundRow(row: RelRow, index: number) {
  row.usageRule.refundPolicy?.splice(index, 1)
}

async function handleSave() {
  // 校验：rel 行不能有空 itemCode
  for (const row of relRows.value) {
    if (!row.itemCode) {
      ElMessage.warning('请选择所有服务项目（不能有空行）')
      return
    }
    if (row.networkScope?.mode === 'custom' && !row.networkScope.parks?.length) {
      ElMessage.warning(`服务项目「${row.itemCode}」选择了自选机构范围，请至少勾选 1 家机构`)
      return
    }
  }

  const dto: GoodsEquitySaveDTO = {
    goodsCode: props.goodsCode,
    personCount: personCount.value,
    validityType: form.validityType,
    holderRule: {
      self: 1,
      spouse: holder.spouse,
      parent: holder.parent,
      designateAtActivation: holder.designateAtActivation
    },
    shareMode: form.shareMode,
    validDays: form.validDays,
    shelfLifeDays: form.shelfLifeDays,
    maxTransferable: form.maxTransferable,
    description: form.description,
    status: 1,
    serviceItems: relRows.value.map(r => ({
      itemCode: r.itemCode,
      quantity: r.quantity,
      quotaType: r.quotaType,
      sortOrder: r.sortOrder,
      networkScope: r.networkScope?.mode === 'custom' && r.networkScope.parks?.length
        ? { mode: 'custom', parks: r.networkScope.parks }
        : null,
      admissionGuaranteed: r.admissionGuaranteed,
      admissionPriority: r.admissionPriority,
      admissionDiscount: r.admissionDiscount,
      // 未勾优惠权时丢弃折扣率，避免口径不一致
      discountRate: r.admissionDiscount === 1 ? (r.discountRate ?? null) : null,
      usageRule: r.usageEnabled ? r.usageRule : null
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
  <div v-loading="loading" class="equity-config-tab">
    <!-- 上半部：权益级配置（人 / 期限 / 共享 / 转让） -->
    <el-card shadow="never" class="config-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">权益配置</span>
          <el-tag v-if="configExists" size="small" type="success">已配置</el-tag>
          <el-tag v-else size="small" type="info">未配置</el-tag>
        </div>
      </template>
      <el-form :model="form" label-width="130px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="权益期限">
              <el-radio-group v-model="form.validityType">
                <el-radio-button
                  v-for="opt in VALIDITY_TYPE_OPTIONS"
                  :key="opt.value"
                  :value="opt.value"
                >{{ opt.label }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col v-if="form.validityType === 1" :span="8">
            <el-form-item label="激活后有效天数">
              <el-input-number v-model="form.validDays" :min="1" :max="99999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col v-else :span="8">
            <el-form-item label="有效期">
              <el-tag type="warning">终身有效（激活后不过期）</el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存有效期(天)">
              <el-input-number v-model="form.shelfLifeDays" :min="1" :max="9999" controls-position="right" style="width: 100%" />
              <div class="field-hint">未激活时的库存有效期</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="权益人构成">
              <div class="holder-rule">
                <el-tag type="primary" effect="plain">本人 ×1（必含，默认权益人）</el-tag>
                <div class="holder-item">
                  <span class="holder-label">配偶</span>
                  <el-switch v-model="holder.spouse" :active-value="1" :inactive-value="0" />
                </div>
                <div class="holder-item">
                  <span class="holder-label">双方父母席位</span>
                  <el-input-number v-model="holder.parent" :min="0" :max="4" size="small" controls-position="right" style="width: 110px" />
                  <span class="holder-label-sub">（含公婆/岳父母，0~4）</span>
                </div>
                <div v-if="holder.parent > 0" class="holder-item">
                  <span class="holder-label">父母人选激活时指定</span>
                  <el-switch v-model="holder.designateAtActivation" />
                </div>
                <el-tag type="success">共 {{ personCount }} 人</el-tag>
              </div>
              <div class="field-hint">使用人人数由构成自动计算；激活时按席位生成权益人，父母席位在「权益人管理」中补全/指定人选</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="配额共享方式">
              <el-select v-model="form.shareMode" style="width: 100%">
                <el-option
                  v-for="opt in SHARE_MODE_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <div class="field-hint">共享池=全体权益人共用次数；按人独立=每位权益人各享一份</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="可转让次数">
              <el-input-number v-model="form.maxTransferable" :min="0" :max="9" controls-position="right" style="width: 100%" />
              <div class="field-hint">0 = 不可转让</div>
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

    <!-- 下半部：服务项目组合（次数 + 权益内容） -->
    <el-card shadow="never" class="rel-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">服务项目组合</span>
          <el-button type="primary" :icon="'Plus'" @click="addRelRow">添加服务项目</el-button>
        </div>
      </template>

      <el-table :data="relRows" border stripe empty-text="暂无服务项目，点击「添加服务项目」">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-body">
              <!-- 入住权 -->
              <div class="expand-section">
                <div class="expand-title">入住权 / 优惠</div>
                <el-space wrap>
                  <el-checkbox v-model="row.admissionGuaranteed" :true-value="1" :false-value="0">保证入住权</el-checkbox>
                  <el-checkbox v-model="row.admissionPriority" :true-value="1" :false-value="0">优先入住权</el-checkbox>
                  <el-checkbox v-model="row.admissionDiscount" :true-value="1" :false-value="0">优惠入住权/旅居优惠权</el-checkbox>
                  <template v-if="row.admissionDiscount === 1">
                    <span class="inline-label">门市价折扣</span>
                    <el-input-number v-model="row.discountRate" :min="1" :max="99.99" :precision="1" size="small" controls-position="right" style="width: 120px" />
                    <span class="inline-label-sub">（{{ row.discountRate ? (row.discountRate / 10) + ' 折' : '未定' }}，如 90 = 9折）</span>
                  </template>
                </el-space>
                <div class="field-hint">保证/优先/优惠随商品档次配置（如至尊版三权全含、一年期管家无保证权）</div>
              </div>

              <!-- 服务网络（商品级收窄） -->
              <div class="expand-section">
                <div class="expand-title">服务网络范围</div>
                <NetworkScopeSelector
                  v-model="row.networkScope"
                  all-label="跟随服务项目（默认）"
                  title="收窄商品级服务网络"
                />
                <div class="field-hint">
                  默认使用所选服务项目的网络范围（可在「商品管理 → 服务项目」中定义并精确到房型）；此处可按商品进一步收窄
                </div>
              </div>

              <!-- 单次使用规则 -->
              <div class="expand-section">
                <div class="expand-title">
                  单次使用规则
                  <el-switch v-model="row.usageEnabled" active-text="启用（随心住类）" />
                </div>
                <template v-if="row.usageEnabled">
                  <el-row :gutter="12" class="usage-grid">
                    <el-col :span="4"><el-form-item label="每次最多天数"><el-input-number v-model="row.usageRule.maxDaysPerUse" :min="1" size="small" controls-position="right" style="width:100%" /></el-form-item></el-col>
                    <el-col :span="4"><el-form-item label="每次最多晚数"><el-input-number v-model="row.usageRule.maxNightsPerUse" :min="1" size="small" controls-position="right" style="width:100%" /></el-form-item></el-col>
                    <el-col :span="4"><el-form-item label="每次房间数"><el-input-number v-model="row.usageRule.maxRoomsPerUse" :min="1" size="small" controls-position="right" style="width:100%" /></el-form-item></el-col>
                    <el-col :span="4"><el-form-item label="每间可住人数"><el-input-number v-model="row.usageRule.maxGuestsPerUse" :min="1" size="small" controls-position="right" style="width:100%" /></el-form-item></el-col>
                    <el-col :span="4"><el-form-item label="提前预订(天)"><el-input-number v-model="row.usageRule.advanceBookDays" :min="0" size="small" controls-position="right" style="width:100%" /></el-form-item></el-col>
                    <el-col :span="4"><el-form-item label="预定金(元)"><el-input-number v-model="row.usageRule.depositAmount" :min="0" size="small" controls-position="right" style="width:100%" /></el-form-item></el-col>
                  </el-row>
                  <el-space wrap class="usage-line">
                    <el-checkbox v-model="row.usageRule.requireBeneficiaryCheckIn">须权益人本人到场办理</el-checkbox>
                    <el-divider direction="vertical" />
                    <span class="inline-label">不可入住时段</span>
                    <el-select v-model="row.usageRule.blackoutType" clearable placeholder="无" size="small" style="width: 120px">
                      <el-option label="春节" value="spring_festival" />
                    </el-select>
                    <template v-if="row.usageRule.blackoutType">
                      <el-input-number v-model="row.usageRule.blackoutDays" :min="1" :max="30" size="small" controls-position="right" style="width: 100px" />
                      <span class="inline-label-sub">天（如春节：除夕至初八共 9 天）</span>
                    </template>
                  </el-space>
                  <div class="refund-block">
                    <div class="inline-label">取消退预定金政策（按距入住时间）</div>
                    <div v-for="(rp, idx) in row.usageRule.refundPolicy" :key="idx" class="refund-row">
                      <span class="inline-label">入住前</span>
                      <el-input-number v-model="rp.beforeHours" :min="1" size="small" controls-position="right" style="width: 100px" />
                      <span class="inline-label">小时前取消，退</span>
                      <el-input-number v-model="rp.refundRate" :min="0" :max="100" size="small" controls-position="right" style="width: 100px" />
                      <span class="inline-label-sub">%</span>
                      <el-button link type="danger" size="small" @click="removeRefundRow(row, idx)">删除</el-button>
                    </div>
                    <el-button link type="primary" size="small" @click="addRefundRow(row)">+ 添加档位</el-button>
                  </div>
                  <div class="field-hint">文档口径：72小时前取消全退、48小时前退50%、24小时内不退；预定金于入住后1-3个工作日退还</div>
                </template>
                <div v-else class="field-hint">未启用：按常规服务项目履约（无间夜/人数/押金约束）</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="服务项目" min-width="220">
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
        <el-table-column label="大类" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.itemCode" size="small">
              {{ ITEM_CATEGORY_OPTIONS.find(o => o.value === serviceItemOptions.find(i => i.itemCode === row.itemCode)?.itemCategory)?.label || '—' }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="110" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" :max="999" controls-position="right" size="small" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="配额周期" width="190" align="center">
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
        <el-table-column label="权益内容" min-width="230">
          <template #default="{ row }">
            <el-space wrap size="small">
              <el-tag v-if="row.admissionGuaranteed === 1" size="small" type="danger">保证入住</el-tag>
              <el-tag v-if="row.admissionPriority === 1" size="small" type="warning">优先入住</el-tag>
              <el-tag v-if="row.admissionDiscount === 1" size="small" type="success">优惠{{ row.discountRate ? (row.discountRate / 10) + '折' : '' }}</el-tag>
              <el-tag v-if="row.networkScope?.mode === 'custom'" size="small" type="info">{{ networkScopeSummary(row.networkScope) }}</el-tag>
              <el-tag v-if="row.usageEnabled" size="small" type="primary">随心住规则</el-tag>
              <span v-if="row.admissionGuaranteed !== 1 && row.admissionPriority !== 1 && row.admissionDiscount !== 1 && !row.usageEnabled && row.networkMode !== 'custom'" class="field-hint">展开配置权益内容</span>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="90" align="center">
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
  .card-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2329;
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

  .holder-rule {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
  }
  .holder-item {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .holder-label {
    font-size: 14px;
    color: #606266;
  }
  .holder-label-sub {
    font-size: 12px;
    color: #999;
  }

  .expand-body {
    padding: 8px 16px 16px;
    background: #fafafa;
  }
  .expand-section {
    margin-bottom: 16px;
    &:last-child {
      margin-bottom: 0;
    }
  }
  .expand-title {
    font-weight: 600;
    font-size: 14px;
    color: #303133;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .usage-grid {
    margin-bottom: 8px;
  }
  .usage-line {
    margin: 8px 0;
  }
  .inline-label {
    font-size: 13px;
    color: #606266;
  }
  .inline-label-sub {
    font-size: 12px;
    color: #999;
  }
  .refund-block {
    margin-top: 10px;
    .refund-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 6px 0;
    }
  }
}
</style>
