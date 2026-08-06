<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app';

onLaunch(() => {
  console.log('Agent App Launch');
});
onShow(() => {
  console.log('Agent App Show');
});
onHide(() => {
  console.log('Agent App Hide');
});
</script>

<style lang="scss">
/* 全局样式（uView 组件库未使用，已移除其 SCSS 导入以避免未定义的主题变量） */
page {
  background-color: #f5f6f8;
}
</style>
