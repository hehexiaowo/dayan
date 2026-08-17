<script setup lang="ts">
/**
 * 系统素材仓库（系统管理 → 素材仓库）。
 *
 * 统一管理整个系统的文件与外链资源：本地 OSS 对象 + 外部存储链接（storageType 区分），
 * 图片/视频/文档/VR 不做 tab 拆分，作为筛选条件与清单列统一呈现；
 * 机构维度视图仍在机构详情「素材仓库」tab（AssetTab 锁定 refType1/refCode 复用 AssetPane）。
 */
import AssetPane from '@/views/resource/park/detail/AssetPane.vue'
</script>

<template>
  <div class="asset-library">
    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      系统素材仓库：统一管理本地 OSS 文件与外部链接资源，各业务模块从这里统一调配；类型1/类型2/关联编码为冗余分类（真实引用在各业务表），删除受引用保护。
    </el-alert>
    <AssetPane :show-card="true" />
  </div>
</template>

<style scoped>
.asset-library {
  padding: 4px 0;
}
</style>
