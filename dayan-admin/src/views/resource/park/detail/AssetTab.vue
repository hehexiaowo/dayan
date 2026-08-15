<script setup lang="ts">
/**
 * 机构详情页 - 素材库 tab（容器组件）。
 *
 * 内层 el-tabs 切 4 类素材（图片/视频/文件/VR），全部复用统一 AssetPane 组件。
 * 每类素材通过 :asset-type prop 过滤，UI 体验不变，后端统一走 system_asset 单表
 * （系统素材库，机构维度按 parkCode 过滤）。
 */
import { ref } from 'vue'
import AssetPane from './AssetPane.vue'

defineProps<{
  parkCode: string
}>()

const activeName = ref('image')
</script>

<template>
  <el-tabs v-model="activeName" type="card" class="asset-tabs">
    <el-tab-pane label="图片" name="image" lazy>
      <AssetPane :park-code="parkCode" :asset-type="1" />
    </el-tab-pane>
    <el-tab-pane label="视频" name="video" lazy>
      <AssetPane :park-code="parkCode" :asset-type="2" />
    </el-tab-pane>
    <el-tab-pane label="文档" name="file" lazy>
      <AssetPane :park-code="parkCode" :asset-type="3" />
    </el-tab-pane>
    <el-tab-pane label="VR" name="vr" lazy>
      <AssetPane :park-code="parkCode" :asset-type="4" />
    </el-tab-pane>
  </el-tabs>
</template>

<style scoped>
.asset-tabs {
  margin-top: 8px;
}
</style>
