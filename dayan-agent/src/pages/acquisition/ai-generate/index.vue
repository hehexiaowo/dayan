<template>
  <view class="page dy-safe-bottom">
    <!-- 步骤条 -->
    <view class="steps">
      <view
        v-for="(s, i) in stepDefs"
        :key="s.key"
        class="step"
        :class="{ active: step === i + 1, done: step > i + 1 }"
      >
        <view class="step-dot">{{ step > i + 1 ? '✓' : i + 1 }}</view>
        <text class="step-label">{{ s.label }}</text>
      </view>
    </view>

    <!-- ============ Step 1 选素材 ============ -->
    <template v-if="step === 1">
      <!-- 参考范文 -->
      <view class="section">
        <view class="section-head">
          <text class="section-title">参考范文</text>
          <text class="section-sub">选一篇既有内容仿写其文风（可不选）</text>
        </view>
        <view v-if="templates.length" class="pick-list tpl-list">
          <view class="group-label">平台范文模板</view>
          <view
            v-for="t in templates"
            :key="t.code"
            class="pick-item dy-clickable"
            :class="{ picked: refContentCode === t.code }"
            @click="toggleRef(t.code)"
          >
            <view class="pick-main">
              <text class="pick-title">{{ t.name }}</text>
              <text class="pick-tag one-line">{{ t.excerpt }}</text>
            </view>
            <text class="pick-check">{{ refContentCode === t.code ? '✓ 已选' : '选为范文' }}</text>
          </view>
        </view>
        <view v-if="refList.length" class="group-label standalone">渠道内容</view>
        <view class="search-row">
          <input v-model="refKeyword" class="search-input" placeholder="搜索文章标题" confirm-type="search" @confirm="loadRefList" />
          <view class="btn-search dy-clickable" @click="loadRefList">搜索</view>
        </view>
        <view v-if="refList.length" class="pick-list">
          <view
            v-for="item in refList"
            :key="item.contentCode"
            class="pick-item dy-clickable"
            :class="{ picked: refContentCode === item.contentCode }"
            @click="toggleRef(item.contentCode)"
          >
            <text class="pick-title">{{ item.title }}</text>
            <text class="pick-check">{{ refContentCode === item.contentCode ? '✓ 已选' : '选为范文' }}</text>
          </view>
          <view v-if="refLoading" class="pick-more">加载中…</view>
          <view v-else-if="refList.length < refTotal" class="pick-more dy-clickable" @click="loadRefMore">加载更多</view>
        </view>
        <view v-else-if="!refLoading" class="empty-hint">暂无可选内容</view>
      </view>

      <!-- 知识库文档 -->
      <view class="section">
        <view class="section-head">
          <text class="section-title">知识库素材</text>
          <text class="section-sub">平台库 + 本渠道库（可不选）</text>
        </view>
        <view class="search-row">
          <input v-model="kbKeyword" class="search-input" placeholder="搜索文档名" confirm-type="search" @confirm="loadKbDocs" />
          <view class="btn-search dy-clickable" @click="loadKbDocs">搜索</view>
        </view>
        <view v-if="kbDocs.length" class="pick-list">
          <view
            v-for="doc in kbDocs"
            :key="doc.fileId"
            class="pick-item dy-clickable"
            :class="{ picked: kbFileIds.includes(doc.fileId) }"
            @click="toggleKbDoc(doc.fileId)"
          >
            <view class="pick-main">
              <text class="pick-title">{{ doc.fileName }}</text>
              <text v-if="doc.repoName" class="pick-tag">{{ doc.repoName }}</text>
            </view>
            <text class="pick-check">{{ kbFileIds.includes(doc.fileId) ? '✓ 已选' : '选入素材' }}</text>
          </view>
        </view>
        <view v-else-if="!kbLoading" class="empty-hint">
          {{ kbDocsError || '暂无文档（需先在后台知识仓库上传资料）' }}
        </view>
      </view>

      <!-- 推荐商品 -->
      <view class="section">
        <view class="section-head">
          <text class="section-title">推荐商品</text>
          <text class="section-sub">生成时把商品卖点融入内容（可不选）</text>
        </view>
        <view class="search-row">
          <input v-model="goodsKeyword" class="search-input" placeholder="搜索商品名称" confirm-type="search" @confirm="loadGoods" />
          <view class="btn-search dy-clickable" @click="loadGoods">搜索</view>
        </view>
        <view v-if="goodsList.length" class="pick-list">
          <view
            v-for="g in goodsList"
            :key="g.goodsCode"
            class="pick-item dy-clickable"
            :class="{ picked: goodsCodes.includes(g.goodsCode) }"
            @click="toggleGoods(g.goodsCode)"
          >
            <view class="pick-main">
              <text class="pick-title">{{ g.goodsName }}</text>
              <text class="pick-tag">¥{{ g.salePrice ?? '面议' }}</text>
            </view>
            <text class="pick-check">{{ goodsCodes.includes(g.goodsCode) ? '✓ 已选' : '选入推荐' }}</text>
          </view>
        </view>
        <view v-else-if="!goodsLoading" class="empty-hint">暂无可选商品</view>
      </view>

      <view v-if="refContentCode || kbFileIds.length || goodsCodes.length" class="summary-hint">
        已选：{{ refContentCode ? '范文 1 篇' : '' }} {{ kbFileIds.length ? `知识库文档 ${kbFileIds.length} 篇` : '' }}
        {{ goodsCodes.length ? `商品 ${goodsCodes.length} 个` : '' }}
      </view>
    </template>

    <!-- ============ Step 2 形态与风格 ============ -->
    <template v-else-if="step === 2">
      <view class="section">
        <view class="section-head">
          <text class="section-title">内容形态</text>
        </view>
        <view class="option-grid">
          <view
            v-for="o in AI_CONTENT_TYPE_OPTIONS"
            :key="o.value"
            class="option-card dy-clickable"
            :class="{ picked: contentType === o.value }"
            @click="contentType = o.value"
          >
            <text class="option-label">{{ o.label }}</text>
            <text class="option-desc">{{ o.desc }}</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="section-title">写作风格</text>
          <text v-if="refContentCode" class="section-sub">已选范文，将优先模仿范文文风</text>
        </view>
        <view class="option-grid">
          <view
            v-for="o in AI_STYLE_OPTIONS"
            :key="o.value"
            class="option-card dy-clickable"
            :class="{ picked: styleCode === o.value }"
            @click="styleCode = o.value"
          >
            <text class="option-label">{{ o.label }}</text>
            <text class="option-desc">{{ o.desc }}</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="section-title">目标读者</text>
        </view>
        <view class="option-grid">
          <view
            v-for="o in AI_AUDIENCE_OPTIONS"
            :key="o.value"
            class="option-card dy-clickable"
            :class="{ picked: audience === o.value }"
            @click="audience = o.value"
          >
            <text class="option-label">{{ o.label }}</text>
            <text class="option-desc">{{ o.desc }}</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="section-title">主题与要求</text>
          <text class="section-sub">可留空，默认按素材归纳</text>
        </view>
        <textarea v-model="topic" class="topic-input" placeholder="例如：介绍大雁养老的终身养老权益，突出六档覆盖人数，引导客户咨询" :maxlength="200" />
      </view>
    </template>

    <!-- ============ Step 3 生成与保存 ============ -->
    <template v-else>
      <view v-if="generating" class="generating">
        <text class="stage-text">{{ stageText }}</text>
        <scroll-view v-if="streamText" scroll-y class="stream-preview" :scroll-top="streamScrollTop">
          <text class="stream-text">{{ streamText }}</text>
        </scroll-view>
        <view v-else class="generating-icon">✨</view>
      </view>

      <template v-else-if="result">
        <view v-if="result.warnings?.length" class="warn-box">
          <text v-for="(w, i) in result.warnings" :key="i" class="warn-line">⚠ {{ w }}</text>
        </view>
        <view class="result-card">
          <text class="result-title">{{ result.title }}</text>
          <text v-if="result.summary" class="result-summary">{{ result.summary }}</text>
          <rich-text v-if="result.contentType === 1" class="result-body" :nodes="result.contentBody" />
          <text v-else class="result-body text">{{ result.contentBody }}</text>
        </view>
      </template>
    </template>

    <!-- 底部操作 -->
    <view class="footer-bar">
      <template v-if="step > 1 && !generating">
        <view class="btn-plain dy-clickable" @click="step -= 1">上一步</view>
      </template>
      <template v-if="step < 3">
        <view class="btn-primary dy-clickable" :class="{ disabled: generating }" @click="goNext">{{ generating ? '生成中…' : (step === 2 ? '生成内容' : '下一步') }}</view>
      </template>
      <template v-else-if="result && !generating">
        <view class="btn-plain dy-clickable" @click="regenerate">重新生成</view>
        <view class="btn-plain dy-clickable" @click="copyResult">复制</view>
        <view class="btn-primary dy-clickable" :class="{ disabled: saving }" @click="save">保存到我的内容</view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { AI_CONTENT_TYPE_OPTIONS, AI_STYLE_OPTIONS, AI_AUDIENCE_OPTIONS } from '@/types/aiContent'
import type { AiGenerateResult, KnowledgeDocOption, AiRefTemplateOption } from '@/types/aiContent'
import type { ContentArticle, GoodsProduct } from '@/types'
import { getContentList } from '@/api/content'
import { getGoodsList } from '@/api/goods'
import { getKnowledgeDocs } from '@/api/knowledge'
import { generateAiContent, saveAiContent, getAiTemplates } from '@/api/aiContent'
import { postSseStream } from '@/utils/sse'
import { copyText } from '@/utils/clipboard'
import { htmlToText } from '@/utils/htmlToText'

const stepDefs = [
  { key: 'material', label: '选素材' },
  { key: 'style', label: '定风格' },
  { key: 'result', label: '生成' }
]

const step = ref(1)

// ---- Step1 素材 ----
const refKeyword = ref('')
const refList = ref<ContentArticle[]>([])
const refTotal = ref(0)
const refLoading = ref(false)
const refPage = ref(1)
const refContentCode = ref('')
const templates = ref<AiRefTemplateOption[]>([])

const kbKeyword = ref('')
const kbDocs = ref<KnowledgeDocOption[]>([])
const kbLoading = ref(false)
const kbDocsError = ref('')
const kbFileIds = ref<string[]>([])

const goodsKeyword = ref('')
const goodsList = ref<GoodsProduct[]>([])
const goodsLoading = ref(false)
const goodsCodes = ref<string[]>([])

// ---- Step2 形态风格 ----
const contentType = ref<number>(1)
const styleCode = ref('professional')
const audience = ref('general')
const topic = ref('')

// ---- Step3 生成 ----
const generating = ref(false)
const saving = ref(false)
const result = ref<AiGenerateResult | null>(null)
const stageText = ref('')
const streamText = ref('')
const streamScrollTop = ref(0)

async function loadRefList() {
  refLoading.value = true
  try {
    const res = await getContentList({ current: 1, size: 10, title: refKeyword.value })
    refList.value = res.records
    refTotal.value = res.total
    refPage.value = 1
  } catch {
    refList.value = []
  } finally {
    refLoading.value = false
  }
}

async function loadRefMore() {
  if (refLoading.value) return
  const page = refPage.value + 1
  refLoading.value = true
  try {
    const res = await getContentList({ current: page, size: 10, title: refKeyword.value })
    refList.value = [...refList.value, ...res.records]
    refPage.value = page
  } catch {
    // 全局拦截器已提示
  } finally {
    refLoading.value = false
  }
}

function toggleRef(code: string) {
  refContentCode.value = refContentCode.value === code ? '' : code
}

async function loadTemplates() {
  try {
    templates.value = await getAiTemplates()
  } catch {
    templates.value = []
  }
}

async function loadKbDocs() {
  kbLoading.value = true
  kbDocsError.value = ''
  try {
    kbDocs.value = await getKnowledgeDocs(kbKeyword.value)
  } catch (e) {
    kbDocs.value = []
    kbDocsError.value = '知识库加载失败'
  } finally {
    kbLoading.value = false
  }
}

function toggleKbDoc(fileId: string) {
  const idx = kbFileIds.value.indexOf(fileId)
  if (idx >= 0) kbFileIds.value.splice(idx, 1)
  else kbFileIds.value.push(fileId)
}

async function loadGoods() {
  goodsLoading.value = true
  try {
    goodsList.value = await getGoodsList({ goodsName: goodsKeyword.value })
  } catch {
    goodsList.value = []
  } finally {
    goodsLoading.value = false
  }
}

function toggleGoods(code: string) {
  const idx = goodsCodes.value.indexOf(code)
  if (idx >= 0) goodsCodes.value.splice(idx, 1)
  else goodsCodes.value.push(code)
}

function goNext() {
  if (step.value === 2) {
    doGenerate()
  } else {
    step.value += 1
  }
}

const FALLBACK_STAGES = ['正在准备素材…', '正在检索知识库…', '正在创作，约需 30-60 秒…']

function buildPayload() {
  return {
    contentType: contentType.value,
    styleCode: styleCode.value,
    audience: audience.value,
    refContentCode: refContentCode.value || undefined,
    kbFileIds: kbFileIds.value,
    goodsCodes: goodsCodes.value,
    topic: topic.value || undefined
  }
}

async function doGenerate() {
  if (generating.value) return
  generating.value = true
  result.value = null
  streamText.value = ''
  step.value = 3
  // #ifdef H5
  await doGenerateStream()
  // #endif
  // #ifndef H5
  await doGenerateFallback()
  // #endif
}

// #ifdef H5
async function doGenerateStream() {
  stageText.value = '正在准备素材…'
  try {
    await postSseStream('/agent-api/ai/generate/stream', buildPayload(), {
      onEvent: (name, data) => {
        // 单事件解析失败仅跳过该事件，不中断后续流处理
        let parsed: any
        try {
          parsed = JSON.parse(data)
        } catch {
          return
        }
        if (name === 'stage') {
          stageText.value = parsed.message
        } else if (name === 'delta') {
          streamText.value += parsed.text
          streamScrollTop.value += 9999
        } else if (name === 'done') {
          result.value = parsed
          streamText.value = ''
        } else if (name === 'error') {
          uni.showToast({ title: parsed.message || '生成失败', icon: 'none' })
        }
      }
    })
  } catch {
    uni.showToast({ title: '生成失败，请稍后重试', icon: 'none' })
  } finally {
    generating.value = false
  }
}
// #endif

// #ifndef H5
async function doGenerateFallback() {
  let idx = 0
  stageText.value = FALLBACK_STAGES[0]
  const timer = setInterval(() => {
    idx = Math.min(idx + 1, FALLBACK_STAGES.length - 1)
    stageText.value = FALLBACK_STAGES[idx]
  }, 8000)
  try {
    result.value = await generateAiContent(buildPayload())
  } catch {
    // 全局拦截器已提示
  } finally {
    clearInterval(timer)
    generating.value = false
  }
}
// #endif

async function copyResult() {
  if (!result.value) return
  try {
    await copyText(htmlToText(result.value.contentBody))
    uni.showToast({ title: '已复制正文', icon: 'success' })
  } catch {
    uni.showToast({ title: '复制失败', icon: 'none' })
  }
}

async function regenerate() {
  await doGenerate()
}

async function save() {
  if (!result.value || saving.value) return
  saving.value = true
  try {
    const id = await saveAiContent({
      title: result.value.title,
      summary: result.value.summary,
      contentType: result.value.contentType,
      contentBody: result.value.contentBody,
      styleCode: styleCode.value,
      audience: audience.value,
      refContentCode: refContentCode.value || undefined,
      refKbFiles: kbFileIds.value.length
        ? JSON.stringify(kbDocs.value.filter((d) => kbFileIds.value.includes(d.fileId)).map((d) => ({ fileId: d.fileId, fileName: d.fileName })))
        : undefined,
      refGoodsCodes: goodsCodes.value.length ? JSON.stringify(goodsCodes.value) : undefined
    })
    uni.showToast({ title: '已保存到我的内容', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: '/pages/acquisition/content/mine' })
    }, 800)
  } finally {
    saving.value = false
  }
}

onLoad(() => {
  loadTemplates()
  loadRefList()
  loadKbDocs()
  loadGoods()
})
</script>

<style lang="scss" scoped>
.page { padding: 24rpx 24rpx 160rpx; background: $bg-page; min-height: 100vh; }
.steps { display: flex; justify-content: space-around; padding: 24rpx 0 8rpx; }
.step { display: flex; flex-direction: column; align-items: center; gap: 8rpx; opacity: .5; }
.step.active, .step.done { opacity: 1; }
.step-dot { width: 48rpx; height: 48rpx; border-radius: 50%; background: #fff; border: 2rpx solid #c0c4cc; display: flex; align-items: center; justify-content: center; font-size: 26rpx; color: #606266; }
.step.active .step-dot { background: $brand-primary; border-color: $brand-primary; color: #fff; }
.step.done .step-dot { background: #67c23a; border-color: #67c23a; color: #fff; }
.step-label { font-size: 24rpx; color: #606266; }
.section { margin-top: 24rpx; }
.section-head { display: flex; align-items: baseline; gap: 16rpx; margin-bottom: 16rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #303133; }
.section-sub { font-size: 22rpx; color: #909399; }
.search-row { display: flex; gap: 16rpx; margin-bottom: 16rpx; }
.search-input { flex: 1; background: #fff; border-radius: 12rpx; padding: 0 20rpx; height: 68rpx; font-size: 26rpx; }
.btn-search { background: $brand-primary; color: #fff; border-radius: 12rpx; padding: 0 28rpx; display: flex; align-items: center; font-size: 26rpx; }
.pick-list { background: #fff; border-radius: 16rpx; overflow: hidden; }
.pick-item { display: flex; align-items: center; justify-content: space-between; padding: 24rpx; border-bottom: 1rpx solid #f0f0f0; }
.pick-item.picked { background: rgba(64, 158, 255, .06); }
.pick-main { display: flex; flex-direction: column; gap: 8rpx; flex: 1; margin-right: 16rpx; }
.pick-title { font-size: 28rpx; color: #303133; }
.pick-tag { font-size: 22rpx; color: #909399; }
.pick-check { font-size: 24rpx; color: #909399; }
.pick-item.picked .pick-check { color: $brand-primary; }
.pick-more { text-align: center; padding: 20rpx; font-size: 24rpx; color: #909399; }
.empty-hint { text-align: center; color: #c0c4cc; font-size: 24rpx; padding: 32rpx 0; }
.summary-hint { margin-top: 24rpx; padding: 20rpx 24rpx; background: rgba(64, 158, 255, .08); border-radius: 12rpx; font-size: 24rpx; color: $brand-primary; }
.option-grid { display: flex; flex-wrap: wrap; gap: 20rpx; }
.option-card { width: calc(50% - 10rpx); background: #fff; border-radius: 16rpx; padding: 24rpx; border: 2rpx solid transparent; }
.option-card.picked { border-color: $brand-primary; background: rgba(64, 158, 255, .06); }
.option-label { display: block; font-size: 28rpx; font-weight: 600; color: #303133; margin-bottom: 8rpx; }
.option-desc { font-size: 22rpx; color: #909399; line-height: 1.5; }
.topic-input { width: 100%; background: #fff; border-radius: 16rpx; padding: 24rpx; font-size: 26rpx; min-height: 160rpx; box-sizing: border-box; }
.generating { display: flex; flex-direction: column; align-items: center; padding: 120rpx 0; gap: 24rpx; }
.generating-icon { font-size: 80rpx; }
.group-label { font-size: 24rpx; color: #909399; padding: 20rpx 24rpx 8rpx; }
.group-label.standalone { padding: 8rpx 8rpx 8rpx; margin-bottom: 8rpx; }
.tpl-list { margin-bottom: 8rpx; }
.pick-tag.one-line { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.stage-text { font-size: 28rpx; color: #606266; }
.stream-preview { margin-top: 24rpx; width: 100%; height: 560rpx; background: #fff; border-radius: 16rpx; padding: 24rpx; box-sizing: border-box; }
.stream-text { font-size: 26rpx; color: #606266; line-height: 1.8; word-break: break-all; }
.warn-box { background: rgba(230, 162, 60, .12); border-radius: 12rpx; padding: 20rpx 24rpx; margin-top: 24rpx; }
.warn-line { display: block; font-size: 24rpx; color: #e6a23c; line-height: 1.6; }
.result-card { background: #fff; border-radius: 16rpx; padding: 32rpx; margin-top: 24rpx; }
.result-title { display: block; font-size: 34rpx; font-weight: 600; color: #303133; margin-bottom: 12rpx; }
.result-summary { display: block; font-size: 26rpx; color: #606266; margin-bottom: 16rpx; }
.result-body { font-size: 28rpx; color: #303133; line-height: 1.8; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; display: flex; gap: 20rpx; padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom)); background: #fff; box-shadow: 0 -4rpx 16rpx rgba(0,0,0,.04); }
.btn-primary { flex: 1; background: $brand-primary; color: #fff; border-radius: 12rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; font-size: 30rpx; }
.btn-primary.disabled { opacity: .5; }
.btn-plain { flex: 1; background: #f5f7fa; color: #606266; border-radius: 12rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; font-size: 30rpx; }
</style>
