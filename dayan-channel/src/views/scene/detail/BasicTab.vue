<script setup lang="ts">
/**
 * 场景详情页 - 基本信息 tab（只读）。
 *
 * 调 getScene(sceneCode) 拉取场景信息，el-descriptions :column="2" border 全字段只读展示
 * （ID/编码/名称/类型/状态/园区/审核状态/容量/时间/描述/封面图）。
 * auditStatus 渠道端无枚举映射，原值展示。接口失败降级为空态提示。
 */
import { ref } from 'vue'
import { getScene } from '@/api/scene'
import { SCENE_TYPE_OPTIONS, SCENE_STATUS_OPTIONS, sceneStatusTagType, type SceneInfo } from '@/types/scene'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 场景编码（路由参数） */
  sceneCode: string
}>()

const loading = ref(false)
const scene = ref<SceneInfo | null>(null)

async function loadDetail() {
  if (!props.sceneCode) return
  loading.value = true
  try {
    scene.value = await getScene(props.sceneCode)
  } catch {
    scene.value = null
  } finally {
    loading.value = false
  }
}

loadDetail()

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
  <div v-loading="loading">
    <el-descriptions v-if="scene" :column="2" border>
      <el-descriptions-item label="ID">{{ scene.id ?? '--' }}</el-descriptions-item>
      <el-descriptions-item label="场景编码">{{ scene.sceneCode || '--' }}</el-descriptions-item>
      <el-descriptions-item label="场景名称" :span="2">{{ scene.sceneName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="类型">{{ sceneTypeText(scene.sceneType) }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag v-if="scene.sceneStatus != null" size="small" :type="sceneStatusTagType(scene.sceneStatus)">
          {{ sceneStatusText(scene.sceneStatus) }}
        </el-tag>
        <span v-else>--</span>
      </el-descriptions-item>
      <el-descriptions-item label="园区编码">{{ scene.parkCode || '--' }}</el-descriptions-item>
      <el-descriptions-item label="园区名称">{{ scene.parkName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="审核状态">{{ scene.auditStatus ?? '--' }}</el-descriptions-item>
      <el-descriptions-item label="容量">{{ scene.capacity ?? '--' }}</el-descriptions-item>
      <el-descriptions-item label="已用容量">{{ scene.usedCapacity ?? '--' }}</el-descriptions-item>
      <el-descriptions-item label="开始时间">{{ formatDateTime(scene.startTime) }}</el-descriptions-item>
      <el-descriptions-item label="结束时间">{{ formatDateTime(scene.endTime) }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatDateTime(scene.createdAt) }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ formatDateTime(scene.updatedAt) }}</el-descriptions-item>
      <el-descriptions-item label="场景描述" :span="2">{{ scene.description || '--' }}</el-descriptions-item>
      <el-descriptions-item label="封面图" :span="2">{{ scene.coverImage || '--' }}</el-descriptions-item>
    </el-descriptions>
    <el-empty v-else-if="!loading" description="未加载到场景信息" />
  </div>
</template>
