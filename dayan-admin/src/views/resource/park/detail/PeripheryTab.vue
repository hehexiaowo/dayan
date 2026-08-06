<script setup lang="ts">
/**
 * 机构详情页 - 周边/服务项 tab（容器）。
 *
 * 单 tab 内用 el-tabs 切两个区：周边配套 + 服务项目。
 * 两区独立 useCrud，互不耦合。PeripheryTab 仅做 el-tabs 容器。
 * 子区懒加载：el-tab-pane 的 lazy 属性（首次切到才渲染内容），nav 标题始终常驻。
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
</script>

<template>
  <div class="periphery-tab">
    <el-tabs v-model="activeName" type="card">
      <el-tab-pane label="周边配套" name="periphery" lazy>
        <PeripheryPane :park-code="parkCode" />
      </el-tab-pane>
      <el-tab-pane label="服务项目" name="service" lazy>
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
