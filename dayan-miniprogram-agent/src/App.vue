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
@import 'uview-ui/index.scss';

/* 全局样式 */
page {
  background-color: #f8f8f8;
}
</style>
