<script setup lang="ts">
/**
 * 机构详情页（主从详情页 / tab 式）。
 *
 * 从机构列表页"详情/管理"按钮进入（携带 parkCode 路由参数）。
 * 顶部展示机构主信息摘要 + 返回按钮；下方 el-tabs 按子表维度分 tab，
 * 每个 tab 内是该子表的内联 CRUD（自动携带 parkCode 过滤）。
 *
 * tab 划分（对应 P9 计划）：
 * - 基本信息：ParkInfo 主表字段编辑（复用主列表页编辑表单逻辑）
 * - 房型：ParkRoomType + ParkRoomPrice（type 列表 + 展开行 price 内联）
 * - 照护：ParkCareType + ParkCarePrice
 * - 餐饮：ParkFoodType + ParkFoodPrice
 * - 媒体库：ParkMediaImage/Video/File/Vr（4 表合并，按 mediaType 筛选）
 * - 设施：ParkFacility
 * - 顾问：ParkAdviser
 * - 周边/服务项：ParkPeriphery + ParkServiceItem
 *
 * 注：本文件为任务 0 基础设施骨架，tab 内容在任务 1/2 逐个填充。
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
  { name: 'room', label: '房型' },
  { name: 'care', label: '照护' },
  { name: 'food', label: '餐饮' },
  { name: 'media', label: '媒体库' },
  { name: 'facility', label: '设施' },
  { name: 'adviser', label: '顾问' },
  { name: 'periphery', label: '周边/服务项' }
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
      </div>
      <div v-else-if="!detailLoading" class="park-summary">
        <span class="title">未找到机构（parkCode={{ parkCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：基本信息/房型/照护/餐饮 已实现，其余 4 个 tab 占位 -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name">
        <BasicTab v-if="t.name === 'basic'" :park-code="parkCode" />
        <RoomTab v-else-if="t.name === 'room'" :park-code="parkCode" />
        <CareTab v-else-if="t.name === 'care'" :park-code="parkCode" />
        <FoodTab v-else-if="t.name === 'food'" :park-code="parkCode" />
        <div v-else class="tab-placeholder">
          <!-- TODO 任务2：媒体/设施/顾问/周边 各子表内联 CRUD -->
          <el-empty :description="`${t.label}管理（任务 2 实现）`" />
        </div>
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
.tab-placeholder {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ml-8 {
  margin-left: 8px;
}
</style>
