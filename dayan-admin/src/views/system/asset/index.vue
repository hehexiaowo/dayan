<script setup lang="ts">
/**
 * 系统素材仓库（系统管理 → 素材仓库）。
 *
 * 管理整个系统的文件与外链资源：本地 OSS 对象 + 外部存储链接（storageType 区分），
 * 跨机构浏览/筛选（含平台素材 park_code=NULL）；
 * 机构维度视图仍在机构详情「素材仓库」tab（AssetTab 传 parkCode 复用 AssetPane）。
 */
import { ref } from 'vue'
import AssetPane from '@/views/resource/park/detail/AssetPane.vue'

const activeName = ref('image')
</script>

<template>
  <div class="asset-library">
    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      系统素材仓库：统一管理本地 OSS 文件与外部链接资源，各业务模块从这里统一调配；机构编码留空查询即含平台素材，删除受引用保护。
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
