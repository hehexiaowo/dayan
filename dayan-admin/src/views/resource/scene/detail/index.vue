<script setup lang="ts">
/**
 * 场景活动详情页（主从详情页 / tab 式）。
 *
 * 从场景列表页"详情"按钮进入（携带 sceneCode 路由参数）。
 * 顶部展示场景主信息摘要（场景名 + 编码 + 类型/场景状态/审核状态标签 + 价格）+ 返回按钮；
 * 下方 el-tabs 按子表维度分 5 个 tab，每个 tab 内是该子表的内联 CRUD（自动携带 sceneCode）。
 *
 * tab 划分（对应 P9.2 计划，1 主表 + 4 子表）：
 * - 基本信息：SceneInfo 主表字段编辑（复用主列表页 updateScene，不含状态机）
 * - 项目明细：SceneItem（by-scene list + CRUD，主键 id）
 * - 价格档位：SceneItemPrice（分页 + CRUD，sceneItemCode 关联当前场景的 items）
 * - 活动日程：SceneSchedule（分页 + CRUD，status 5 态）
 * - 所需资源：SceneResource（分页 + CRUD）
 *
 * 懒加载：所有 el-tab-pane 带 lazy 属性，未访问的 tab 不渲染内容（但标签常驻可见）。
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getScene } from '@/api/scene'
import {
  SCENE_TYPE_OPTIONS,
  SCENE_STATUS_OPTIONS,
  AUDIT_STATUS_OPTIONS,
  SceneStatus
} from '@/types/scene'
import type { SceneInfo } from '@/types/scene'
import BasicTab from './BasicTab.vue'
import ItemTab from './ItemTab.vue'
import PriceTab from './PriceTab.vue'
import ScheduleTab from './ScheduleTab.vue'
import ResourceTab from './ResourceTab.vue'

const route = useRoute()
const router = useRouter()
const sceneCode = computed(() => route.params.sceneCode as string)

const activeTab = ref('basic')
const detailLoading = ref(false)
const sceneInfo = ref<SceneInfo | null>(null)

async function loadDetail() {
  if (!sceneCode.value) return
  detailLoading.value = true
  try {
    sceneInfo.value = await getScene(sceneCode.value)
  } catch {
    sceneInfo.value = null
  } finally {
    detailLoading.value = false
  }
}

loadDetail()

function goBack() {
  router.push({ path: '/resource/scene' })
}

/** 场景类型文本 */
function sceneTypeLabel(t?: number): string {
  const found = SCENE_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : '--'
}

/** 场景状态文本 */
function sceneStatusLabel(s?: number): string {
  const found = SCENE_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : '--'
}

/** 审核状态文本 */
function auditStatusLabel(s?: number): string {
  const found = AUDIT_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : '--'
}

/** 根据场景状态返回 el-tag type：草稿info/上架success/下架warning/满期danger。 */
function sceneStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case SceneStatus.PUBLISHED:
      return 'success'
    case SceneStatus.OFFLINE:
      return 'warning'
    case SceneStatus.FULL:
      return 'danger'
    case SceneStatus.DRAFT:
    default:
      return 'info'
  }
}

/** 审核状态 tag type */
function auditStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 1:
      return 'success'
    case 0:
      return 'warning'
    case 2:
      return 'danger'
    default:
      return 'info'
  }
}

/** 价格显示：isFree=1 显示「免费」，否则显示 salePrice + 单位。 */
function priceLabel(row: SceneInfo): string {
  if (row.isFree === 1) return '免费'
  if (row.salePrice == null) return '--'
  return row.priceUnit ? `${row.salePrice} ${row.priceUnit}` : String(row.salePrice)
}

const tabs = [
  { name: 'basic', label: '基本信息' },
  { name: 'item', label: '项目明细' },
  { name: 'price', label: '价格档位' },
  { name: 'schedule', label: '活动日程' },
  { name: 'resource', label: '所需资源' }
] as const
</script>

<template>
  <div v-loading="detailLoading" class="scene-detail">
    <!-- 顶部：返回 + 主实体摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div v-if="sceneInfo" class="scene-summary">
        <span class="title">{{ sceneInfo.sceneName }}</span>
        <el-tag size="small" class="ml-8">{{ sceneInfo.sceneCode }}</el-tag>
        <el-tag size="small" type="info" class="ml-8">
          {{ sceneTypeLabel(sceneInfo.sceneType) }}
        </el-tag>
        <el-tag size="small" :type="sceneStatusTagType(sceneInfo.sceneStatus)" class="ml-8">
          {{ sceneStatusLabel(sceneInfo.sceneStatus) }}
        </el-tag>
        <el-tag
          size="small"
          :type="auditStatusTagType(sceneInfo.auditStatus)"
          effect="light"
          class="ml-8"
        >
          {{ auditStatusLabel(sceneInfo.auditStatus) }}
        </el-tag>
        <span v-if="sceneInfo.salePrice != null || sceneInfo.isFree === 1" class="meta">
          · {{ priceLabel(sceneInfo) }}
        </span>
      </div>
      <div v-else-if="!detailLoading" class="scene-summary">
        <span class="title">未找到场景（sceneCode={{ sceneCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：5 个 tab，全部 lazy 懒加载 -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" lazy>
        <BasicTab v-if="t.name === 'basic'" :scene-code="sceneCode" />
        <ItemTab v-else-if="t.name === 'item'" :scene-code="sceneCode" />
        <PriceTab v-else-if="t.name === 'price'" :scene-code="sceneCode" />
        <ScheduleTab v-else-if="t.name === 'schedule'" :scene-code="sceneCode" />
        <ResourceTab v-else-if="t.name === 'resource'" :scene-code="sceneCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.scene-detail {
  padding: 16px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}
.scene-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.scene-summary .title {
  font-size: 18px;
  font-weight: 600;
}
.scene-summary .meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-left: 8px;
}
.ml-8 {
  margin-left: 8px;
}
</style>
