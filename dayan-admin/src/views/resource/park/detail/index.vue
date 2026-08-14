<script setup lang="ts">
/**
 * 机构详情页（主从详情页 / tab 式）。
 *
 * 从机构列表页"详情/管理"按钮进入（携带 parkCode 路由参数）。
 * 顶部展示机构主信息摘要 + 返回按钮；下方 el-tabs 按子表维度分 tab，
 * 每个 tab 内是该子表的内联 CRUD（自动携带 parkCode 过滤），均 lazy 懒加载。
 *
 * tab 划分（6 个顶层 tab）：
 * - 基本信息：ParkInfo 主表字段编辑 + ParkScore 评分
 * - 服务配置：内层 el-tabs 切 5 子面板——房型 / 照护 / 餐饮 / 设施 / 服务项目，
 *   各子面板内联展开行定价（ParkPricing）
 * - 素材库：内层 el-tabs 切 4 类型——图片 / 视频 / 文件 / VR（ParkAsset 统一表）
 * - 联系顾问：ParkAdviser
 * - 周边相关：ParkPeriphery
 * - 展示板块：ParkDisplayBlock
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPark } from '@/api/park'
import { PARK_OPERATE_STATUS_OPTIONS } from '@/types/park'
import type { ParkInfo } from '@/types/park'
import BasicTab from './BasicTab.vue'
import AssetTab from './AssetTab.vue'
import ServiceConfigTab from './ServiceConfigTab.vue'
import AdviserTab from './AdviserTab.vue'
import PeripheryPane from './PeripheryPane.vue'
import DisplayPane from './DisplayPane.vue'
import DisplayConfigTab from './DisplayConfigTab.vue'

const route = useRoute()
const router = useRouter()
const parkCode = computed(() => route.params.parkCode as string)

const activeTab = ref('basic')
const detailLoading = ref(false)
const parkInfo = ref<ParkInfo | null>(null)

async function loadDetail() {
  if (!parkCode.value) return
  detailLoading.value = true
  try {
    parkInfo.value = await getPark(parkCode.value)
  } catch {
    parkInfo.value = null
  } finally {
    detailLoading.value = false
  }
}

loadDetail()

function goBack() {
  router.push({ path: '/resource/park' })
}

function operateStatusText(s?: number): string {
  const found = PARK_OPERATE_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : '--'
}

const tabs = [
  { name: 'basic', label: '基本信息' },
  { name: 'svc-config', label: '服务配置' },
  { name: 'asset', label: '素材库' },
  { name: 'display-config', label: '网络展示' },
  { name: 'adviser', label: '联系顾问' },
  { name: 'periphery', label: '周边相关' },
  { name: 'display', label: '展示板块' }
] as const
</script>

<template>
  <div v-loading="detailLoading" class="park-detail">
    <!-- 顶部：返回 + 主实体摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div v-if="parkInfo" class="park-summary">
        <span class="title">{{ parkInfo.fullName }}</span>
        <el-tag size="small" class="ml-8">{{ parkInfo.parkCode }}</el-tag>
        <el-tag size="small" type="info" class="ml-8">
          {{ operateStatusText(parkInfo.operateStatus) }}
        </el-tag>
        <span v-if="parkInfo.city || parkInfo.district" class="meta">
          {{ parkInfo.province }}{{ parkInfo.city }}{{ parkInfo.district }}
        </span>
        <el-divider v-if="parkInfo.totalBeds != null || parkInfo.minPriceDisplay != null" direction="vertical" />
        <span v-if="parkInfo.totalBeds != null" class="meta">
          床位 {{ parkInfo.availableBeds ?? '--' }}/{{ parkInfo.totalBeds }}
        </span>
        <span v-if="parkInfo.minPriceDisplay != null" class="meta highlight">
          ¥{{ parkInfo.minPriceDisplay }}{{ parkInfo.maxPriceDisplay ? `~${parkInfo.maxPriceDisplay}` : '' }}<template v-if="parkInfo.priceUnit"> {{ parkInfo.priceUnit }}</template>
        </span>
      </div>
      <div v-else-if="!detailLoading" class="park-summary">
        <span class="title">未找到机构（parkCode={{ parkCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：6 个顶层 tab（均 lazy 懒加载） -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" lazy>
        <BasicTab v-if="t.name === 'basic'" :park-code="parkCode" />
        <ServiceConfigTab v-else-if="t.name === 'svc-config'" :park-code="parkCode" />
        <AssetTab v-else-if="t.name === 'asset'" :park-code="parkCode" />
        <DisplayConfigTab v-else-if="t.name === 'display-config'" :park-code="parkCode" />
        <AdviserTab v-else-if="t.name === 'adviser'" :park-code="parkCode" />
        <PeripheryPane v-else-if="t.name === 'periphery'" :park-code="parkCode" />
        <DisplayPane v-else-if="t.name === 'display'" :park-code="parkCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.park-detail {
  padding: 16px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}
.park-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.park-summary .title {
  font-size: 18px;
  font-weight: 600;
}
.park-summary .meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-left: 8px;
}
.park-summary .meta.highlight {
  color: var(--el-color-success);
  font-weight: 600;
}
.ml-8 {
  margin-left: 8px;
}
</style>
