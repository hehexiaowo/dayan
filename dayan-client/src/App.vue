<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app';

onLaunch(() => {
  console.log('Client App Launch');
});
onShow(() => {
  console.log('Client App Show');
});
onHide(() => {
  console.log('Client App Hide');
});
</script>

<style lang="scss">
/* 全局样式：引入设计令牌与公共类（dy-*），所有页面可直接使用 */
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

page {
  background-color: #f5f6f8;
}

/* 隐藏桌面浏览器预览时的原生滚动条（真机本就是叠加式不可见），保持滚动功能 */
::-webkit-scrollbar {
  width: 0 !important;
  height: 0 !important;
  display: none !important;
}
html {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none;
}
</style>
