<script setup lang="ts">
/**
 * 机构详情页（主从详情页 / tab 式）。
 *
 * 从机构列表页"详情/管理"按钮进入（携带 parkCode 路由参数）。
 * 顶部展示机构主信息摘要 + 返回按钮；下方 el-tabs 按子表维度分 tab，
 * 每个 tab 内是该子表的内联 CRUD（自动携带 parkCode 过滤）。
 *
 * tab 划分（对应 P9 计划）：
 * - 基本信息：ParkInfo 主表字段编辑（复用主列表页编辑表单逻辑）+ ParkScore 评分
 * - 房型价格：ParkRoomType + ParkPricing（chargeType=1，展开行定价内联）
 * - 照护价格：ParkCareType + ParkPricing（chargeType=2）
 * - 餐饮价格：ParkFoodType + ParkPricing（chargeType=3）
 * - 素材文件：ParkMediaImage/Video/File/Vr（单 tab 内 el-tabs 切 4 子类）
 * - 基础设施：ParkFacility + ParkPricing（chargeType=5）
 * - 联系顾问：ParkAdviser
 * - 周边相关：ParkPeriphery
 * - 服务项目：ParkServiceItem + ParkPricing（chargeType=6）
 *
 * 注：各 tab 内容均已实现（任务 0 骨架 + 任务 1 前 4 tab + 任务 2 后 4 tab）。
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPark } from '@/api/park'
import { PARK_OPERATE_STATUS_OPTIONS } from '@/types/park'
import type { ParkInfo } from '@/types/park'
import BasicTab from './BasicTab.vue'
import RoomTab from './RoomTab.vue'
import CareTab from './CareTab.vue'
import FoodTab from './FoodTab.vue'
import MediaTab from './MediaTab.vue'
import FacilityTab from './FacilityTab.vue'
import AdviserTab from './AdviserTab.vue'
import PeripheryPane from './PeripheryPane.vue'
import ServiceItemPane from './ServiceItemPane.vue'
import DisplayPane from './DisplayPane.vue'

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
  { name: 'room', label: '房型价格' },
  { name: 'care', label: '照护价格' },
  { name: 'food', label: '餐饮价格' },
  { name: 'media', label: '素材文件' },
  { name: 'facility', label: '基础设施' },
  { name: 'adviser', label: '联系顾问' },
  { name: 'periphery', label: '周边相关' },
  { name: 'service', label: '服务项目' },
  { name: 'display', label: '展示板块' }
] as const
</script>

<template>
  <div class="park-detail" v-loading="detailLoading">
    <!-- 顶部：返回 + 主实体摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div class="park-summary" v-if="parkInfo">
        <span class="title">{{ parkInfo.fullName }}</span>
        <el-tag size="small" class="ml-8">{{ parkInfo.parkCode }}</el-tag>
        <el-tag size="small" type="info" class="ml-8">
          {{ operateStatusText(parkInfo.operateStatus) }}
        </el-tag>
        <span class="meta" v-if="parkInfo.city || parkInfo.district">
          {{ parkInfo.province }}{{ parkInfo.city }}{{ parkInfo.district }}
        </span>
        <el-divider direction="vertical" v-if="parkInfo.totalBeds != null || parkInfo.minPriceDisplay != null" />
        <span class="meta" v-if="parkInfo.totalBeds != null">
          床位 {{ parkInfo.availableBeds ?? '--' }}/{{ parkInfo.totalBeds }}
        </span>
        <span class="meta highlight" v-if="parkInfo.minPriceDisplay != null">
          ¥{{ parkInfo.minPriceDisplay }}{{ parkInfo.maxPriceDisplay ? `~${parkInfo.maxPriceDisplay}` : '' }}<template v-if="parkInfo.priceUnit"> {{ parkInfo.priceUnit }}</template>
        </span>
      </div>
      <div v-else-if="!detailLoading" class="park-summary">
        <span class="title">未找到机构（parkCode={{ parkCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：9 个子表 tab -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" lazy>
        <BasicTab v-if="t.name === 'basic'" :park-code="parkCode" />
        <RoomTab v-else-if="t.name === 'room'" :park-code="parkCode" />
        <CareTab v-else-if="t.name === 'care'" :park-code="parkCode" />
        <FoodTab v-else-if="t.name === 'food'" :park-code="parkCode" />
        <MediaTab v-else-if="t.name === 'media'" :park-code="parkCode" />
        <FacilityTab v-else-if="t.name === 'facility'" :park-code="parkCode" />
        <AdviserTab v-else-if="t.name === 'adviser'" :park-code="parkCode" />
        <PeripheryPane v-else-if="t.name === 'periphery'" :park-code="parkCode" />
        <ServiceItemPane v-else-if="t.name === 'service'" :park-code="parkCode" />
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
