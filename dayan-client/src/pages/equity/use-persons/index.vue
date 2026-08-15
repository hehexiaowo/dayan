<template>
  <view class="persons-page dy-safe-bottom">
    <!-- 加载骨架 -->
    <template v-if="loading && persons.length === 0">
      <DySkeleton :rows="2" card />
      <DySkeleton :rows="2" card />
    </template>

    <!-- 空状态 -->
    <DyEmpty
      v-else-if="persons.length === 0"
      text="还没有添加权益人"
      icon="人"
      color="green"
    />

    <view v-else class="person-list">
      <view
        v-for="p in persons"
        :key="p.id"
        class="person-card dy-clickable"
        :class="{ placeholder: isPlaceholder(p) }"
        @click="goEdit(p.id)"
        @longpress="onLongPress(p)"
      >
        <view class="avatar">
          <text class="avatar-text">{{ isPlaceholder(p) ? '?' : (p.usePersonName || '?').charAt(0) }}</text>
        </view>
        <view class="person-info">
          <view class="name-row">
            <text class="name">{{ p.usePersonName }}</text>
            <text v-if="p.isDefaultHolder === 1" class="default-badge">默认</text>
          </view>
          <view class="meta-row">
            <text v-if="p.relationWithHolder" class="meta">{{ relationLabel(p.relationWithHolder) }}</text>
            <text v-if="p.usePersonPhone" class="meta">{{ p.usePersonPhone }}</text>
          </view>
        </view>
        <view v-if="isPlaceholder(p)" class="warn-btn" @click.stop="goEdit(p.id)">请补全</view>
        <text v-else class="arrow">›</text>
      </view>
    </view>

    <!-- 新增按钮 -->
    <view class="add-bar">
      <button class="dy-btn dy-btn-primary" @click="goAdd">+ 新增使用人</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import { getUsePersons, deleteUsePerson, setDefaultUsePerson } from '@/api/equity';
import type { EquityUsePerson } from '@/types';
import { relationLabel } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const equityCode = ref('');
const persons = ref<EquityUsePerson[]>([]);
const loading = ref(false);

function isPlaceholder(p: EquityUsePerson) {
  return !p.usePersonName || p.usePersonName.startsWith('待填写');
}

async function loadPersons() {
  if (!equityCode.value) return;
  loading.value = true;
  try {
    persons.value = await getUsePersons(equityCode.value);
  } catch (e) {
    persons.value = [];
  } finally {
    loading.value = false;
  }
}

function goAdd() {
  uni.navigateTo({ url: `/pages/equity/use-persons/edit?equityCode=${equityCode.value}` });
}
function goEdit(id: string) {
  uni.navigateTo({ url: `/pages/equity/use-persons/edit?equityCode=${equityCode.value}&id=${id}` });
}

function onLongPress(p: EquityUsePerson) {
  const actions = ['编辑'];
  if (p.isDefaultHolder !== 1) actions.push('设为默认');
  actions.push('删除');
  uni.showActionSheet({
    itemList: actions,
    success: (res) => {
      const action = actions[res.tapIndex];
      if (action === '编辑') {
        goEdit(p.id);
      } else if (action === '设为默认') {
        doSetDefault(p);
      } else if (action === '删除') {
        doDelete(p);
      }
    },
  });
}

async function doSetDefault(p: EquityUsePerson) {
  try {
    await setDefaultUsePerson(p.id);
    uni.showToast({ title: '已设为默认', icon: 'success' });
    loadPersons();
  } catch (e) {
    /* 拦截器已提示 */
  }
}

function doDelete(p: EquityUsePerson) {
  uni.showModal({
    title: '确认删除',
    content: `确定删除权益人「${p.usePersonName}」吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteUsePerson(p.id);
          uni.showToast({ title: '已删除', icon: 'success' });
          loadPersons();
        } catch (e) {
          /* 拦截器已提示 */
        }
      }
    },
  });
}

onLoad((q) => {
  equityCode.value = q?.equityCode || '';
});
onShow(loadPersons);
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.persons-page {
  min-height: 100vh;
  background: $bg-page;
  padding: $spacing-sm $spacing-sm;
}

.person-list {
  padding-bottom: $spacing-sm;
}
.person-card {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-lg;
  padding: 28rpx $spacing-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
  &.placeholder {
    border: 2rpx solid $brand-error;
    background: $brand-error-light;
  }
}
.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: $gradient-brand;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  .placeholder & {
    background: $gradient-red;
  }
}
.avatar-text {
  font-size: 34rpx;
  font-weight: bold;
}
.person-info {
  flex: 1;
  margin-left: $spacing-sm;
  overflow: hidden;
}
.name-row {
  display: flex;
  align-items: center;
}
.name {
  font-size: 32rpx;
  font-weight: 500;
  color: $text-primary;
}
.default-badge {
  font-size: 20rpx;
  color: $brand-primary;
  border: 1px solid $brand-primary;
  padding: 2rpx 10rpx;
  border-radius: $radius-sm;
  margin-left: 12rpx;
}
.meta-row {
  display: flex;
  margin-top: $spacing-xs;
}
.meta {
  font-size: 24rpx;
  color: $text-secondary;
  margin-right: $spacing-sm;
}
.arrow {
  color: $text-placeholder;
  font-size: 36rpx;
}
.warn-btn {
  font-size: 24rpx;
  color: #fff;
  background: $brand-error;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
}

.add-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: $spacing-sm $spacing-md;
  background: $bg-card;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
}
</style>
