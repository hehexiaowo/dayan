<script setup lang="ts">
/**
 * 机构详情页 - 媒体库 tab（容器）。
 *
 * 内部用 el-tabs 切 4 个子面板：图片 / 视频 / 文档 / VR。
 * 各子面板独立 useCrud（idKey:'id', fixedParams:{parkCode}）。
 *
 * 设计取舍：4 类媒体字段结构不同（图片有宽高/封面、视频有时长/封面、文档有格式、VR 有类型），
 * 拆 4 个独立 Pane 组件分治，避免单文件膨胀；MediaTab 仅做 el-tabs 容器。
 * 子面板懒加载：el-tab-pane 的 lazy 属性（首次切到才渲染内容），nav 标题始终常驻。
 */
import { ref } from 'vue'
import MediaImagePane from './MediaImagePane.vue'
import MediaVideoPane from './MediaVideoPane.vue'
import MediaFilePane from './MediaFilePane.vue'
import MediaVrPane from './MediaVrPane.vue'

defineProps<{
  /** 机构编码（从详情页 prop 带入，透传各子面板） */
  parkCode: string
}>()

/** 当前激活的媒体子类：image / video / file / vr */
const activeName = ref<'image' | 'video' | 'file' | 'vr'>('image')
</script>

<template>
  <div class="media-tab">
    <el-tabs v-model="activeName" type="card">
      <el-tab-pane label="图片" name="image" lazy>
        <MediaImagePane :park-code="parkCode" />
      </el-tab-pane>
      <el-tab-pane label="视频" name="video" lazy>
        <MediaVideoPane :park-code="parkCode" />
      </el-tab-pane>
      <el-tab-pane label="文档" name="file" lazy>
        <MediaFilePane :park-code="parkCode" />
      </el-tab-pane>
      <el-tab-pane label="VR" name="vr" lazy>
        <MediaVrPane :park-code="parkCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.media-tab {
  /* el-tabs 默认即可 */
}
</style>
