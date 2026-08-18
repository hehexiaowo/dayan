<template>
  <view class="page">
    <!-- 当前创作分类 -->
    <view v-if="category" class="cat-banner">
      <DyIconBlock :text="category.icon || '创'" :color="catColor(category.iconColor)" size="md" shape="circle" />
      <view class="cat-banner-info">
        <text class="cat-banner-name">{{ category.toolName }}</text>
        <text class="cat-banner-desc">{{ category.toolDesc }}</text>
      </view>
    </view>

    <!-- 素材（按分类目的条件渲染） -->
    <view>
      <view class="dy-section-title">{{ purpose === 'product' ? '保险产品/政策资料（必选）' : '知识库文档（可选，精准取材）' }}<text v-if="purpose === 'product'" class="req">*</text></view>
      <view class="search-row"><input class="dy-search" v-model="kbKeyword" placeholder="搜索知识库文档" confirm-type="search" @confirm="loadKbDocs" /></view>
      <view class="pick-list dy-card">
        <view v-for="d in kbDocs" :key="d.fileId" class="pick-item dy-clickable"
          :class="{ picked: kbFileIds.includes(d.fileId) }" @click="toggle(kbFileIds, d.fileId)">
          <view class="pick-main">
            <text class="pick-title dy-ellipsis">{{ d.fileName }}</text>
            <text class="pick-tag">{{ d.repoName }}</text>
          </view>
          <view class="check-round" :class="{ on: kbFileIds.includes(d.fileId) }"><text class="check-mark">✓</text></view>
        </view>
        <view v-if="!kbDocs.length" class="pick-empty"><text class="pick-empty-text">暂无文档（需渠道知识库建库）</text></view>
      </view>

      <view class="dy-section-title">{{ purpose === 'product' ? '权益商品（必选）' : '权益商品（可选）' }}<text v-if="purpose === 'product'" class="req">*</text></view>
      <view class="search-row"><input class="dy-search" v-model="goodsKeyword" placeholder="搜索商品" confirm-type="search" @confirm="loadGoods" /></view>
      <view class="pick-list dy-card">
        <view v-for="g in goodsList" :key="g.goodsCode" class="pick-item dy-clickable"
          :class="{ picked: goodsCodes.includes(g.goodsCode) }" @click="toggle(goodsCodes, g.goodsCode)">
          <view class="pick-main">
            <text class="pick-title dy-ellipsis">{{ g.goodsName }}</text>
            <text class="pick-tag">¥{{ g.salePrice ?? '面议' }}</text>
          </view>
          <view class="check-round" :class="{ on: goodsCodes.includes(g.goodsCode) }"><text class="check-mark">✓</text></view>
        </view>
      </view>
    </view>

    <template v-if="purpose === 'park'">
      <view class="dy-section-title">养老机构（必选）<text class="req">*</text></view>
      <view class="cat-row">
        <text v-for="c in PARK_CATS" :key="c.value" class="cat-tag dy-clickable"
          :class="{ on: parkCategory === c.value }" @click="switchParkCat(c.value)">{{ c.label }}</text>
      </view>
      <view class="search-row"><input class="dy-search" v-model="parkKeyword" placeholder="按名称筛选" /></view>
      <view class="pick-list dy-card">
        <view v-for="p in filteredParks" :key="p.parkCode" class="pick-item dy-clickable"
          :class="{ picked: parkCodes.includes(p.parkCode) }" @click="toggle(parkCodes, p.parkCode)">
          <view class="pick-main">
            <text class="pick-title dy-ellipsis">{{ p.fullName }}</text>
            <text class="pick-tag">{{ p.city }} · 床位 {{ p.availableBeds ?? '-' }}</text>
          </view>
          <view class="check-round" :class="{ on: parkCodes.includes(p.parkCode) }"><text class="check-mark">✓</text></view>
        </view>
      </view>
    </template>

    <!-- 分类专属：粘贴内容 -->
    <template v-if="purpose === 'science'">
      <view class="dy-section-title">粘贴文章内容或链接（必填）<text class="req">*</text></view>
      <textarea class="dy-textarea" v-model="pastedText" maxlength="4000"
        placeholder="粘贴要转写的文章全文，或文章链接（转写将忠于原文事实）" />
    </template>
    <template v-if="purpose === 'product'">
      <view class="dy-section-title">粘贴保险计划书文本（必填）<text class="req">*</text></view>
      <textarea class="dy-textarea" v-model="pastedText" maxlength="8000"
        placeholder="粘贴已有保险计划书的核心内容（保障责任、保额、费率、缴费方式等）" />
    </template>

    <!-- 可选素材 -->
    <view class="dy-section-title">主题 / 切入话题<text v-if="purpose === 'science'" class="req">*</text></view>
    <textarea class="dy-textarea" v-model="topic" maxlength="200" placeholder="如：延迟退休政策解读、给爸妈的旅居计划…" />

    <view class="dy-section-title">参考范文（可选）</view>
    <view class="pick-list dy-card">
      <view v-for="t in templates" :key="t.code" class="pick-item dy-clickable"
        :class="{ picked: refContentCode === t.code }" @click="pickRef(t.code)">
        <view class="pick-main"><text class="pick-title dy-ellipsis">{{ t.name }}（模板）</text><text class="pick-tag">{{ t.desc }}</text></view>
        <view class="check-round" :class="{ on: refContentCode === t.code }"><text class="check-mark">✓</text></view>
      </view>
    </view>

    <!-- 形态/风格/读者 -->
    <view class="dy-section-title">发布形态</view>
    <view class="option-grid">
      <view v-for="o in AI_CONTENT_TYPE_OPTIONS" :key="o.value" class="option-card dy-clickable"
        :class="{ picked: contentType === o.value }" @click="contentType = o.value">
        <text class="option-title">{{ o.label }}</text>
        <text class="option-desc">{{ o.desc }}</text>
        <view v-if="contentType === o.value" class="option-flag"><text class="option-flag-mark">✓</text></view>
      </view>
    </view>
    <view class="dy-section-title">写作风格</view>
    <view class="option-grid">
      <view v-for="o in AI_STYLE_OPTIONS" :key="o.value" class="option-card dy-clickable"
        :class="{ picked: styleCode === o.value }" @click="styleCode = o.value">
        <text class="option-title">{{ o.label }}</text>
        <text class="option-desc">{{ o.desc }}</text>
        <view v-if="styleCode === o.value" class="option-flag"><text class="option-flag-mark">✓</text></view>
      </view>
    </view>
    <view class="dy-section-title">目标读者</view>
    <view class="option-grid">
      <view v-for="o in AI_AUDIENCE_OPTIONS" :key="o.value" class="option-card dy-clickable"
        :class="{ picked: audience === o.value }" @click="audience = o.value">
        <text class="option-title">{{ o.label }}</text>
        <text class="option-desc">{{ o.desc }}</text>
        <view v-if="audience === o.value" class="option-flag"><text class="option-flag-mark">✓</text></view>
      </view>
    </view>

    <view class="footer-bar">
      <view class="btn-primary dy-clickable" :class="{ 'dy-btn-disabled': creating }" @click="submit">
        <text class="btn-primary-text">{{ creating ? '创建中…' : '下一步：生成策略' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createAiProject, getAiTemplates, getAiartistConfigs } from '@/api/toolAiartist'
import { getKnowledgeDocs } from '@/api/knowledge'
import { getGoodsList } from '@/api/goods'
import { getRegions } from '@/api/park'
import { assembleMaterials } from '@/utils/aiMaterial'
import type { AiRefTemplateOption, KnowledgeDocOption } from '@/types/aiContent'
import type { AiartistConfig } from '@/types/toolAiartist'
import { AI_CONTENT_TYPE_OPTIONS, AI_STYLE_OPTIONS, AI_AUDIENCE_OPTIONS } from '@/types/aiContent'
import type { ParkCard } from '@/types/park'
import type { GoodsProduct } from '@/types'
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue'

/**
 * 第 1 步（分类化）：创作分类（tool_info 的 aiartist 实例）决定目的与素材规则。
 * 主题创作（science）→ 粘贴文章/链接；机构介绍（park）→ 选机构；
 * 保险计划（product）→ 粘贴计划书 + 知识库/商品。
 * 素材由本页聚合随创建提交，后端存快照供 digest 阶段一次性消费。
 */
const PARK_CATS = [
  { value: 'vital', label: '活力长居' },
  { value: 'care', label: '照护长居' },
  { value: 'sojourn', label: '旅游短居' }
] as const

type CatColor = 'blue' | 'green' | 'orange' | 'red' | 'gray'

const COLOR_SET: readonly CatColor[] = ['blue', 'green', 'orange', 'red', 'gray']
function catColor(color?: string): CatColor {
  return (color && COLOR_SET.includes(color as CatColor) ? color : 'blue') as CatColor
}

/** 兜底分类 — 接口不可用/未配置时按 toolCode 识别目的（与 tool_info 预置种子一致） */
const FALLBACK_CONFIGS: AiartistConfig[] = [
  { toolCode: 'TL00003', toolName: 'AI创作（主题创作）', purpose: 'science', icon: '主', iconColor: 'blue' },
  { toolCode: 'TL90006', toolName: 'AI创作（机构介绍）', purpose: 'park', icon: '机', iconColor: 'green' },
  { toolCode: 'TL90007', toolName: 'AI创作（保险计划）', purpose: 'product', icon: '保', iconColor: 'orange' },
]

const toolCode = ref('')
const category = ref<AiartistConfig | null>(null)
const purpose = computed(() => category.value?.purpose || 'science')

const contentType = ref(1)
const styleCode = ref('professional')
const audience = ref('general')
const topic = ref('')
const pastedText = ref('')
const refContentCode = ref('')
const kbFileIds = ref<string[]>([])
const goodsCodes = ref<string[]>([])
const parkCodes = ref<string[]>([])
const creating = ref(false)

const templates = ref<AiRefTemplateOption[]>([])
const kbKeyword = ref('')
const kbDocs = ref<KnowledgeDocOption[]>([])
const goodsKeyword = ref('')
const goodsList = ref<GoodsProduct[]>([])
const parkCategory = ref<string>('vital')
const parkKeyword = ref('')
const parks = ref<ParkCard[]>([])

const filteredParks = computed(() =>
  parkKeyword.value ? parks.value.filter((p) => p.fullName?.includes(parkKeyword.value)) : parks.value
)

onLoad(async (opts) => {
  toolCode.value = opts?.toolCode || ''
  if (!toolCode.value) {
    uni.showToast({ title: '请先选择创作分类', icon: 'none' })
    return
  }
  await loadCategory()
  loadTemplates()
  loadKbDocs()
  loadGoods()
  loadParks()
})

async function loadCategory() {
  try {
    const list = await getAiartistConfigs()
    const all = list && list.length ? list : FALLBACK_CONFIGS
    category.value = all.find((c) => c.toolCode === toolCode.value) || null
  } catch {
    category.value = FALLBACK_CONFIGS.find((c) => c.toolCode === toolCode.value) || null
  }
}

async function loadTemplates() { try { templates.value = await getAiTemplates() } catch { /* 已提示 */ } }
async function loadKbDocs() { try { kbDocs.value = await getKnowledgeDocs(kbKeyword.value || undefined) } catch { /* 已提示 */ } }
async function loadGoods() { try { goodsList.value = await getGoodsList({ goodsName: goodsKeyword.value || undefined }) } catch { /* 已提示 */ } }
async function loadParks() {
  try {
    const res = await getRegions({ category: parkCategory.value as any, level: 'park' })
    parks.value = res.parkList ?? []
  } catch { /* 已提示 */ }
}
function switchParkCat(v: string) {
  if (parkCategory.value === v) return
  parkCategory.value = v
  loadParks()
}

function toggle(list: string[], v: string) {
  const i = list.indexOf(v)
  i >= 0 ? list.splice(i, 1) : list.push(v)
}
function pickRef(code: string) {
  refContentCode.value = refContentCode.value === code ? '' : code
}

/** 勾选状态 → 选中对象集（素材聚合与引用命名用） */
function selectedKbDocs() {
  return kbDocs.value.filter((d) => kbFileIds.value.includes(d.fileId))
}
function selectedGoods() {
  return goodsList.value.filter((g) => goodsCodes.value.includes(g.goodsCode))
}

async function submit() {
  if (creating.value) return
  if (purpose.value === 'science' && !pastedText.value.trim()) {
    uni.showToast({ title: '请粘贴要转写的文章内容或链接', icon: 'none' }); return
  }
  if (purpose.value === 'science' && !topic.value.trim()) {
    uni.showToast({ title: '请填写主题/切入话题', icon: 'none' }); return
  }
  if (purpose.value === 'product' && !pastedText.value.trim()) {
    uni.showToast({ title: '请粘贴保险计划书文本', icon: 'none' }); return
  }
  if (purpose.value === 'product' && (!kbFileIds.value.length || !goodsCodes.value.length)) {
    uni.showToast({ title: '保险计划需选知识库资料与权益商品', icon: 'none' }); return
  }
  if (purpose.value === 'park' && !parkCodes.value.length) {
    uni.showToast({ title: '请选择养老机构', icon: 'none' }); return
  }
  creating.value = true
  uni.showLoading({ title: '聚合素材中…', mask: true })
  try {
    const pastedTitle = purpose.value === 'product' ? '保险计划书（粘贴）' : '转写文章（粘贴）'
    const { materials, materialRefs, warnings } = await assembleMaterials({
      purpose: purpose.value,
      topic: topic.value.trim(),
      kbDocs: selectedKbDocs(),
      goods: selectedGoods(),
      parkCodes: parkCodes.value,
      refContentCode: refContentCode.value,
      pastedText: pastedText.value,
      pastedTitle,
    })
    if (warnings.length) {
      uni.showToast({ title: `部分素材加载失败：${warnings[0]}`, icon: 'none', duration: 2000 })
    }
    const id = await createAiProject({
      toolCode: toolCode.value,
      contentType: contentType.value,
      styleCode: styleCode.value,
      audience: audience.value,
      topic: topic.value || undefined,
      materialRefs,
      materials,
    })
    uni.navigateTo({ url: `/pages/acquisition/tools/aiartist/step-strategy?id=${id}` })
  } catch { /* 全局拦截器已提示 */ } finally {
    uni.hideLoading()
    creating.value = false
  }
}
</script>

<style scoped lang="scss">
.page { padding: $spacing-md $spacing-md 180rpx; background: $bg-page; min-height: 100vh; }
.cat-banner { display: flex; align-items: center; background: $gradient-blue; border-radius: $radius-lg; padding: $spacing-md $spacing-lg; margin-bottom: $spacing-md; }
.cat-banner-info { flex: 1; margin-left: $spacing-md; display: flex; flex-direction: column; min-width: 0; }
.cat-banner-name { font-size: 30rpx; font-weight: 700; color: #fff; }
.cat-banner-desc { margin-top: 6rpx; font-size: 22rpx; color: rgba(255, 255, 255, 0.85); }
.req { color: $brand-error; margin-left: 8rpx; }
.option-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx; margin-bottom: 24rpx; }
.option-card { position: relative; background: $bg-card; border: 2rpx solid $border-light; border-radius: $radius-md; padding: 24rpx; }
.option-card.picked { border-color: $brand-primary; background: rgba(64, 158, 255, 0.06); }
.option-title { display: block; font-size: 28rpx; font-weight: 600; color: $text-primary; }
.option-desc { display: block; font-size: 22rpx; color: $text-secondary; margin-top: 8rpx; }
.option-flag { position: absolute; top: 0; right: 0; width: 44rpx; height: 44rpx; background: $brand-primary; border-radius: 0 $radius-md 0 $radius-md; display: flex; align-items: center; justify-content: center; }
.option-flag-mark { color: #fff; font-size: 22rpx; }
.search-row { margin-bottom: 16rpx; }
.pick-list { padding: 8rpx $spacing-md; }
.pick-item { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 0; border-bottom: 1rpx solid $border-light; }
.pick-item:last-child { border-bottom: none; }
.pick-main { flex: 1; min-width: 0; }
.pick-title { display: block; font-size: 26rpx; color: $text-primary; }
.pick-tag { display: block; font-size: 22rpx; color: $text-secondary; margin-top: 4rpx; }
.pick-empty { padding: 24rpx 0; }
.pick-empty-text { font-size: 24rpx; color: $text-placeholder; }
.check-round { width: 40rpx; height: 40rpx; border-radius: 50%; border: 2rpx solid $border-base; display: flex; align-items: center; justify-content: center; margin-left: 16rpx; flex-shrink: 0; }
.check-round.on { background: $brand-primary; border-color: $brand-primary; }
.check-mark { color: #fff; font-size: 22rpx; }
.cat-row { display: flex; gap: 16rpx; margin-bottom: 16rpx; }
.cat-tag { font-size: 24rpx; color: $text-regular; background: $bg-card; border: 2rpx solid $border-light; border-radius: 999rpx; padding: 8rpx 28rpx; }
.cat-tag.on { color: $brand-primary; border-color: $brand-primary; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 50; display: flex; gap: 20rpx; padding: 20rpx $spacing-md calc(20rpx + env(safe-area-inset-bottom)); background: $bg-card; box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, .04); }
.btn-primary { flex: 1; height: $control-height; background: $gradient-blue; border-radius: $radius-md; display: flex; align-items: center; justify-content: center; }
.btn-primary-text { color: #fff; font-size: 30rpx; font-weight: 600; }
</style>
