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

/* ===== uni-app H5 输入框修复 ===== */
/* uni-app H5 编译 <input> 时，内部 .uni-input-wrapper 和 .uni-input-input 默认 height:0，
   导致原生输入框不可见/不可交互。以下覆盖确保内部元素撑满 <uni-input> 外层容器。 */
.uni-input-wrapper {
  width: 100%;
  height: 100%;
  min-height: inherit;
  display: flex;
  align-items: center;
}

.uni-input-input,
.uni-input-placeholder {
  width: 100%;
  height: 100%;
  min-height: inherit;
  font-size: inherit;
  line-height: inherit;
}

.uni-input-placeholder {
  color: $text-placeholder;
}

textarea {
  box-sizing: border-box;
}

.uni-textarea-wrapper,
.uni-textarea-textarea,
.uni-textarea-compute,
.uni-textarea-placeholder {
  width: 100%;
}

image {
  display: block;
}
</style>
