<script setup lang="ts">
/**
 * 场景详情页（tab 式主从详情，路由 /scene/detail/:sceneCode）。
 *
 * 从场景列表页"详情"按钮进入（携带 sceneCode 路由参数）。
 * 顶部为返回按钮 + 场景摘要（场景名 + 编码 + 类型 + 状态 + 园区）；
 * 下方 el-tabs 分 2 个 tab（基本信息 / 活动日程），全部 lazy 懒加载、只读。
 *
 * 摘要数据源 getScene（BasicTab 内部会再次拉全量字段，各自独立降级）。
 * 后端端点未实现时降级：摘要区提示未找到，tab 内空列表。
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getScene } from '@/api/scene'
import { SCENE_TYPE_OPTIONS, SCENE_STATUS_OPTIONS, type SceneInfo } from '@/types/scene'
import { statusTagType } from '@/utils/format'
import BasicTab from './BasicTab.vue'
import ScheduleTab from './ScheduleTab.vue'

const route = useRoute()
const router = useRouter()

/** 场景编码（路由参数） */
const sceneCode = computed(() => (route.params.sceneCode as string) || '')

const activeTab = ref('basic')
const detailLoading = ref(false)
const scene = ref<SceneInfo | null>(null)

async function loadSummary() {
  if (!sceneCode.value) return
  detailLoading.value = true
  try {
    scene.value = await getScene(sceneCode.value)
  } catch {
    scene.value = null
  } finally {
    detailLoading.value = false
  }
}

loadSummary()

/** 返回上一页（列表页） */
function goBack() {
  router.back()
}

/** 场景类型文本（1-8，对齐 SCENE_TYPE_OPTIONS：参观体验/健康讲座/亲子互动/节日活动/文化娱乐/健康检测/美食品鉴/其他） */
function sceneTypeText(v?: number): string {
  return SCENE_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

/** 场景状态文本（0 草稿 / 1 已上架 / 2 已下架 / 3 已满期） */
function sceneStatusText(v?: number): string {
  return SCENE_STATUS_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}
</script>

<template>
  <div v-loading="detailLoading" class="scene-detail">
    <!-- 顶部：返回 + 场景摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div v-if="scene" class="summary">
        <span class="title">{{ scene.sceneName }}</span>
        <el-tag size="small">{{ scene.sceneCode }}</el-tag>
        <el-tag size="small">{{ sceneTypeText(scene.sceneType) }}</el-tag>
        <el-tag v-if="scene.sceneStatus != null" size="small" :type="statusTagType(scene.sceneStatus)">
          {{ sceneStatusText(scene.sceneStatus) }}
        </el-tag>
        <span v-if="scene.parkName" class="meta">{{ scene.parkName }}</span>
      </div>
      <div v-else-if="!detailLoading" class="summary">
        <span class="title">未找到场景（sceneCode={{ sceneCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：2 个 tab，全部 lazy 懒加载 -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="基本信息" name="basic" lazy>
        <BasicTab :scene-code="sceneCode" />
      </el-tab-pane>
      <el-tab-pane label="活动日程" name="schedules" lazy>
        <ScheduleTab :scene-code="sceneCode" />
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
  gap: 12px;
}
.summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.summary .title {
  font-size: 16px;
  font-weight: 600;
}
.summary .meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-left: 4px;
}
</style>
