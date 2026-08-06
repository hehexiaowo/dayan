<script setup lang="ts">
/**
 * 机构详情页 - 周边/服务项 tab（容器）。
 *
 * 单 tab 内用 el-tabs 切两个区：周边配套 + 服务项目。
 * 两区独立 useCrud，互不耦合。PeripheryTab 仅做 el-tabs 容器。
 * 子区懒加载（v-if + visited），首次切到才挂载触发 loadPage。
 */
import { ref } from 'vue'
import PeripheryPane from './PeripheryPane.vue'
import ServiceItemPane from './ServiceItemPane.vue'

defineProps<{
  /** 机构编码（从详情页 prop 带入，透传两子区） */
  parkCode: string
}>()

/** 当前激活的子区：periphery / service */
const activeName = ref<'periphery' | 'service'>('periphery')

/** 已访问过的子区（懒加载：首次切换才挂载） */
const visited = ref<Set<string>>(new Set(['periphery']))
function handleTabLeave(newName: string) {
  visited.value.add(newName)
}
</script>

<template>
  <div class="periphery-tab">
    <el-tabs v-model="activeName" type="card" @tab-change="handleTabLeave">
      <el-tab-pane v-if="visited.has('periphery')" label="周边配套" name="periphery" lazy>
        <PeripheryPane :park-code="parkCode" />
      </el-tab-pane>
      <el-tab-pane v-if="visited.has('service')" label="服务项目" name="service" lazy>
        <ServiceItemPane :park-code="parkCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.periphery-tab {
  /* el-tabs 默认即可 */
}
</style>
