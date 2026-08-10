<template>
  <!-- 方格示意地图：省级=中国方格图(grid)，市级=方格流(flow)。色阶=机构数。 -->
  <view class="tile-map">
    <view v-if="mode === 'grid'" class="tile-grid">
      <view
        v-for="t in gridTiles"
        :key="t.code"
        class="tile"
        :class="tileClass(t.count)"
        :style="{ gridRow: t.row + 1, gridColumn: t.col + 1 }"
        @click="onSelect(t)"
      >
        <text class="tile-name" :class="{ dim: !t.count }">{{ t.short }}</text>
        <text class="tile-count" :class="{ dim: !t.count }">{{ t.count || '' }}</text>
      </view>
    </view>
    <view v-else class="tile-flow">
      <view
        v-for="item in items"
        :key="item.code"
        class="flow-tile"
        :class="tileClass(item.count)"
        @click="onSelect(item)"
      >
        <text class="flow-name">{{ item.name }}</text>
        <text class="flow-count">{{ item.count }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { PROVINCE_TILES } from '@/utils/region';
import type { RegionItem } from '@/types/park';

const props = withDefaults(
  defineProps<{
    items: RegionItem[];
    mode?: 'grid' | 'flow';
  }>(),
  { mode: 'grid' },
);

const emit = defineEmits<{ select: [item: RegionItem] }>();

interface GridTile extends RegionItem { row: number; col: number; short: string }

/** 省级：按固定布局排格；API 未覆盖的省补 0 灰格 */
const gridTiles = computed<GridTile[]>(() => {
  const byCode = new Map(props.items.map((i) => [i.code, i]));
  return Object.entries(PROVINCE_TILES).map(([code, t]) => ({
    code,
    name: byCode.get(code)?.name || t.short,
    count: byCode.get(code)?.count || 0,
    row: t.row,
    col: t.col,
    short: t.short,
  }));
});

const maxCount = computed(() => Math.max(1, ...props.items.map((i) => i.count)));

/** 色阶：0=灰；>0 按占最大值比例 3 档蓝 */
function tileClass(count: number): string {
  if (!count) return 't0';
  const r = count / maxCount.value;
  if (r > 0.66) return 't3';
  if (r > 0.33) return 't2';
  return 't1';
}

function onSelect(item: RegionItem) {
  if (!item.count) return; // 0 机构省份不可点
  emit('select', item);
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.tile-grid {
  display: grid;
  grid-template-rows: repeat(8, 1fr);
  grid-template-columns: repeat(9, 1fr);
  gap: 6rpx;
}
.tile {
  aspect-ratio: 1;
  border-radius: $radius-sm;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.tile-name { font-size: 20rpx; font-weight: 500; color: #fff; }
.tile-count { font-size: 18rpx; color: rgba(255, 255, 255, 0.85); }
.tile-name.dim, .tile-count.dim { color: $text-placeholder; }

.t0 { background: $brand-info-light; }
.t1 { background: rgba(64, 158, 255, 0.35); }
.t2 { background: rgba(64, 158, 255, 0.65); }
.t3 { background: $brand-primary; }

.tile-flow {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}
.flow-tile {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
  padding: 16rpx 24rpx;
  border-radius: $radius-sm;
}
.flow-name { font-size: 26rpx; color: #fff; font-weight: 500; }
.flow-count { font-size: 24rpx; color: rgba(255, 255, 255, 0.85); }
.flow-tile.t0 .flow-name, .flow-tile.t0 .flow-count { color: $text-placeholder; }
</style>
