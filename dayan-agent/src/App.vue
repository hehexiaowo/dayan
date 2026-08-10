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
@import 'uview-ui/theme.scss';
@import 'uview-ui/index.scss';
@import '@/styles/variables.scss';

/* 全局样式 */
page {
  background-color: $bg-page;
  font-size: 28rpx;
  color: $text-primary;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* 全局点击优化 */
view,
text {
  box-sizing: border-box;
}

button {
  margin: 0;
  padding: 0;
  background: none;
  border: none;
  line-height: normal;

  &::after {
    border: none;
  }
}

input {
  box-sizing: border-box;
}

image {
  display: block;
}
</style>
