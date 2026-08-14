<script setup lang="ts">
/**
 * 机构详情页 - 价格配置 tab（5 合 1 容器）。
 *
 * 将房间类型 / 照护等级 / 餐饮方案 / 设施配置 / 服务项目 5 个结构化资料 tab
 * 合并到统一的「价格配置」顶层 tab，内部用 el-tabs 切换子面板。
 * 每个子面板复用原有组件，不做内部逻辑改动。
 */
import { ref } from 'vue'
import RoomTab from './RoomTab.vue'
import CareTab from './CareTab.vue'
import FoodTab from './FoodTab.vue'
import FacilityTab from './FacilityTab.vue'
import ServiceItemPane from './ServiceItemPane.vue'

defineProps<{ parkCode: string }>()

const activeName = ref('room')
</script>

<template>
  <el-tabs v-model="activeName" type="card" class="svc-config-tabs">
    <el-tab-pane label="房间类型" name="room" lazy>
      <RoomTab v-if="activeName === 'room'" :park-code="parkCode" />
    </el-tab-pane>
    <el-tab-pane label="照护等级" name="care" lazy>
      <CareTab v-if="activeName === 'care'" :park-code="parkCode" />
    </el-tab-pane>
    <el-tab-pane label="餐饮方案" name="food" lazy>
      <FoodTab v-if="activeName === 'food'" :park-code="parkCode" />
    </el-tab-pane>
    <el-tab-pane label="设施配置" name="facility" lazy>
      <FacilityTab v-if="activeName === 'facility'" :park-code="parkCode" />
    </el-tab-pane>
    <el-tab-pane label="服务项目" name="service" lazy>
      <ServiceItemPane v-if="activeName === 'service'" :park-code="parkCode" />
    </el-tab-pane>
  </el-tabs>
</template>

<style scoped>
.svc-config-tabs {
  margin-top: -8px;
}
</style>
