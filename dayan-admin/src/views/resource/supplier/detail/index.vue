<script setup lang="ts">
/**
 * 供应商详情页（主从详情页 / tab 式）。
 *
 * 从供应商列表页"详情/管理"按钮进入（携带 supplierCode 路由参数）。
 * 顶部展示供应商主信息摘要 + 返回按钮；下方 el-tabs 组织子表。
 *
 * tab 划分（对应 P9 计划，任务 3 实现）：
 * - 基本信息（basic）：SupplierInfo 主表字段编辑
 * - 联系人（contact）：SupplierContact CRUD
 * - 评价（evaluation）：SupplierEvaluation CRUD（综合分/等级后端自动算）
 * - 合同（contract）：SupplierContract 概览 + 跳转独立合同页（6 态状态流转）
 *
 * 注：供应商 RBAC（role/permission/openplatform）按 TC-E2E-005 裁定跳过（Admin 代录）。
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSupplier } from '@/api/supplier'
import { SUPPLIER_STATUS_OPTIONS } from '@/types/supplier'
import type { SupplierInfo } from '@/types/supplier'
import BasicTab from './BasicTab.vue'
import ContactTab from './ContactTab.vue'
import EvaluationTab from './EvaluationTab.vue'
import ContractTab from './ContractTab.vue'

const route = useRoute()
const router = useRouter()
const supplierCode = computed(() => route.params.supplierCode as string)

const activeTab = ref('basic')
const detailLoading = ref(false)
const supplierInfo = ref<SupplierInfo | null>(null)

async function loadDetail() {
  if (!supplierCode.value) return
  detailLoading.value = true
  try {
    supplierInfo.value = await getSupplier(supplierCode.value)
  } catch {
    supplierInfo.value = null
  } finally {
    detailLoading.value = false
  }
}

loadDetail()

function goBack() {
  router.push({ path: '/resource/supplier' })
}

function statusText(s?: number): string {
  const found = SUPPLIER_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : '--'
}

const tabs = [
  { name: 'basic', label: '基本信息' },
  { name: 'contact', label: '联系人' },
  { name: 'evaluation', label: '评价' },
  { name: 'contract', label: '合同' }
] as const
</script>

<template>
  <div v-loading="detailLoading" class="supplier-detail">
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div v-if="supplierInfo" class="supplier-summary">
        <span class="title">{{ supplierInfo.fullName }}</span>
        <el-tag size="small" class="ml-8">{{ supplierInfo.supplierCode }}</el-tag>
        <el-tag size="small" type="info" class="ml-8">
          {{ statusText(supplierInfo.status) }}
        </el-tag>
      </div>
      <div v-else-if="!detailLoading" class="supplier-summary">
        <span class="title">未找到供应商（supplierCode={{ supplierCode }}）</span>
      </div>
    </div>

    <el-divider />

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name">
        <BasicTab v-if="t.name === 'basic'" :supplier-code="supplierCode" />
        <ContactTab v-else-if="t.name === 'contact'" :supplier-code="supplierCode" />
        <EvaluationTab v-else-if="t.name === 'evaluation'" :supplier-code="supplierCode" />
        <ContractTab v-else-if="t.name === 'contract'" :supplier-code="supplierCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.supplier-detail {
  padding: 16px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}
.supplier-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.supplier-summary .title {
  font-size: 18px;
  font-weight: 600;
}
.ml-8 {
  margin-left: 8px;
}
</style>
