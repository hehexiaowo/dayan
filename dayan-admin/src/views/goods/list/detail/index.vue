<script setup lang="ts">
/**
 * 商品详情页（主从详情页 / 动态 tab 式）。
 *
 * 从商品列表页「详情」按钮进入（携带 goodsCode 路由参数）。
 * 顶部展示商品主信息摘要（名称 + 编码 + 类型 + 状态）+ 返回按钮；下方 el-tabs 按
 * **动态** 显示 tab（与 park/supplier/client/butler 的固定 tab 不同）：
 *
 * - 基本信息 tab：始终显示（GoodsInfo 主表字段编辑）。
 * - SKU 子表 tab：4 选 1，按 goodsType 互斥显示（goodsType 创建后不可改，决定唯一 SKU 子表）：
 *   - goodsType=1（权益商品）→ 权益配置（EquityConfigTab）
 *   - goodsType=2（场景商品）→ 场景配置（SceneTab）
 *   - goodsType=3（课程商品）→ 课程配置（CourseTab）
 *   - goodsType=4（旅游短居商品）→ 权益配置（SojournTab，原「旅游短居配置」）
 *
 * 加载 getGoods 拿到 goodsType 后才计算 tab 列表，避免未拿主信息时渲染错误 SKU tab。
 * 所有 tab 带 lazy 属性懒加载（子 tab nav 常驻，未访问不渲染内容）。
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGoods } from '@/api/goods'
import {
  goodsTypeLabel,
  goodsStatusLabel,
  goodsStatusTagType
} from '@/types/goods'
import type { GoodsInfo } from '@/types/goods'
import BasicTab from './BasicTab.vue'
import EquityConfigTab from './EquityConfigTab.vue'
import SceneTab from './SceneTab.vue'
import CourseTab from './CourseTab.vue'
import SojournTab from './SojournTab.vue'
import GoodsPageConfigTab from './GoodsPageConfigTab.vue'

const route = useRoute()
const router = useRouter()
const goodsCode = computed(() => route.params.goodsCode as string)

const activeTab = ref('basic')
const detailLoading = ref(false)
const goodsInfo = ref<GoodsInfo | null>(null)

async function loadDetail() {
  if (!goodsCode.value) return
  detailLoading.value = true
  try {
    goodsInfo.value = await getGoods(goodsCode.value)
  } catch {
    goodsInfo.value = null
  } finally {
    detailLoading.value = false
  }
}

loadDetail()

function goBack() {
  router.push({ path: '/goods/list' })
}

/**
 * 动态 tab 列表：基本信息 + SKU 子表 tab（按 goodsType 二选一）+ 页面配置（全类型通用）。
 *
 * 必须等 goodsInfo 加载完（拿到 goodsType）才追加 SKU tab，否则会渲染空 tab。
 */
const tabs = computed(() => {
  const base = [{ name: 'basic', label: '基本信息' }]
  const skuTabMap: Record<number, { name: string; label: string }> = {
    1: { name: 'equity-config', label: '权益配置' },
    2: { name: 'scene', label: '场景配置' },
    3: { name: 'course', label: '课程配置' },
    4: { name: 'sojourn', label: '权益配置' }
  }
  const goodsType = goodsInfo.value?.goodsType
  const skuTab = goodsType != null ? skuTabMap[goodsType] : null
  const pageConfigTab = { name: 'page-config', label: '页面配置' }
  return skuTab ? [...base, skuTab, pageConfigTab] : [...base, pageConfigTab]
})
</script>

<template>
  <div v-loading="detailLoading" class="goods-detail">
    <!-- 顶部：返回 + 主实体摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div v-if="goodsInfo" class="goods-summary">
        <span class="title">{{ goodsInfo.goodsName }}</span>
        <el-tag size="small" class="ml-8">{{ goodsInfo.goodsCode }}</el-tag>
        <el-tag size="small" type="info" class="ml-8">
          {{ goodsTypeLabel(goodsInfo.goodsType) }}
        </el-tag>
        <el-tag
          size="small"
          :type="goodsStatusTagType(goodsInfo.goodsStatus)"
          class="ml-8"
        >
          {{ goodsStatusLabel(goodsInfo.goodsStatus) }}
        </el-tag>
      </div>
      <div v-else-if="!detailLoading" class="goods-summary">
        <span class="title">未找到商品（goodsCode={{ goodsCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- 动态 tab 区：基本信息常驻 + 1 个 SKU tab（按 goodsType） -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" lazy>
        <BasicTab v-if="t.name === 'basic'" :goods-code="goodsCode" />
        <EquityConfigTab v-else-if="t.name === 'equity-config'" :goods-code="goodsCode" />
        <SceneTab v-else-if="t.name === 'scene'" :goods-code="goodsCode" />
        <CourseTab v-else-if="t.name === 'course'" :goods-code="goodsCode" />
        <SojournTab v-else-if="t.name === 'sojourn'" :goods-code="goodsCode" />
        <GoodsPageConfigTab v-else-if="t.name === 'page-config'" :goods-code="goodsCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.goods-detail {
  padding: 16px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}
.goods-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.goods-summary .title {
  font-size: 18px;
  font-weight: 600;
}
.ml-8 {
  margin-left: 8px;
}
</style>
