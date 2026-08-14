<script setup lang="ts">
/**
 * 全局素材库（资源管理 → 素材库）。
 *
 * 跨机构浏览/筛选/管理全部素材（含平台素材 park_code=NULL）；
 * 机构维度视图仍在机构详情「素材库」tab（AssetTab 传 parkCode 复用 AssetPane）。
 */
import { ref } from 'vue'
import AssetPane from '../park/detail/AssetPane.vue'

const activeName = ref('image')
</script>

<template>
  <div class="asset-library">
    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      全局素材库：汇集各机构素材与平台素材（机构编码留空查询即含平台素材）。素材删除受引用保护。
    </el-alert>
    <el-tabs v-model="activeName" type="card">
      <el-tab-pane label="图片" name="image" lazy>
        <AssetPane :asset-type="1" />
      </el-tab-pane>
      <el-tab-pane label="视频" name="video" lazy>
        <AssetPane :asset-type="2" />
      </el-tab-pane>
      <el-tab-pane label="文档" name="file" lazy>
        <AssetPane :asset-type="3" />
      </el-tab-pane>
      <el-tab-pane label="VR" name="vr" lazy>
        <AssetPane :asset-type="4" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.asset-library {
  padding: 4px 0;
}
</style>
