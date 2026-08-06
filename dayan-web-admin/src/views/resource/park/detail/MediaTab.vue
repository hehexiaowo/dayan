<script setup lang="ts">
/**
 * 机构详情页 - 媒体库 tab（容器）。
 *
 * 内部用 el-tabs 切 4 个子面板：图片 / 视频 / 文档 / VR。
 * 各子面板独立 useCrud（idKey:'id', fixedParams:{parkCode}）。
 *
 * 设计取舍：4 类媒体字段结构不同（图片有宽高/封面、视频有时长/封面、文档有格式、VR 有类型），
 * 拆 4 个独立 Pane 组件分治，避免单文件膨胀；MediaTab 仅做 el-tabs 容器。
 * 子面板懒加载（v-if + activeName），首次切到才挂载触发 loadPage。
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

/** 已访问过的子面板（懒加载：首次切换才挂载） */
const visited = ref<Set<string>>(new Set(['image']))
function handleTabLeave(newName: string) {
  visited.value.add(newName)
}
</script>

<template>
  <div class="media-tab">
    <el-tabs v-model="activeName" type="card" @tab-change="handleTabLeave">
      <el-tab-pane v-if="visited.has('image')" label="图片" name="image" lazy>
        <MediaImagePane :park-code="parkCode" />
      </el-tab-pane>
      <el-tab-pane v-if="visited.has('video')" label="视频" name="video" lazy>
        <MediaVideoPane :park-code="parkCode" />
      </el-tab-pane>
      <el-tab-pane v-if="visited.has('file')" label="文档" name="file" lazy>
        <MediaFilePane :park-code="parkCode" />
      </el-tab-pane>
      <el-tab-pane v-if="visited.has('vr')" label="VR" name="vr" lazy>
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
