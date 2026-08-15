<template>
  <div class="goods-page-config-tab">
    <el-tabs v-model="activeName" type="card">
      <el-tab-pane label="展示配置" name="display-config" lazy>
        <div v-if="activeName === 'display-config'" v-loading="loading" class="display-config">
          <div class="row">
            <span class="row-label">轮播图</span>
            <div class="chosen-row">
              <div
                v-for="(key, i) in configData.banners"
                :key="key"
                class="chosen-cell"
                :class="{ 'is-thumb': key === thumbOf() }"
                @click="setThumbnail(key)"
              >
                <el-image :src="formatFileUrl(key)" fit="cover" class="chosen-img" :preview-src-list="[]" />
                <span class="order-badge">{{ i + 1 }}</span>
                <span v-if="key === thumbOf()" class="thumb-badge">缩略图</span>
                <div class="cell-ops" @click.stop>
                  <el-icon :class="{ disabled: i === 0 }" @click="moveBanner(i, -1)"><ArrowLeft /></el-icon>
                  <el-icon :class="{ disabled: i === configData.banners.length - 1 }" @click="moveBanner(i, 1)"><ArrowRight /></el-icon>
                  <el-icon @click="removeBanner(i)"><Delete /></el-icon>
                </div>
              </div>
              <el-button :icon="Plus" plain @click="openPicker">从图片库选择</el-button>
            </div>
          </div>

          <div class="row hint-row">
            <span class="row-label">缩略图</span>
            <span class="hint">点击上方任一轮播图设为缩略图；未手动指定时默认第一张，仍为空则 C 端回退商品封面图。</span>
          </div>

          <div class="save-bar">
            <el-button type="primary" :loading="saving" @click="onSave">保存配置</el-button>
            <el-button @click="loadData">重置</el-button>
          </div>

          <AssetPicker
            v-model="pickerVisible"
            type="image"
            multiple
            :limit="9"
            ref-type1="goods"
            :ref-code="goodsCode"
            @select="onPicked"
          />
        </div>
      </el-tab-pane>
      <el-tab-pane label="展示板块" name="display-block" lazy>
        <GoodsDisplayBlockPane v-if="activeName === 'display-block'" :goods-code="goodsCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
/**
 * 商品详情页 - 页面配置 tab（容器组件）。
 *
 * 汇总商品 C/Agent 端详情页展示相关的两个子域，内层 el-tabs 切换：
 * - 展示配置：轮播图（已选横排：排序/删除/从图片库选择）+ 缩略图（点击轮播图点选，默认第一张）
 *   → goods_info.display_config 存 JSON {banners:[key...], thumbnail:"key"}
 * - 展示板块：详情页结构化 tab 内容（类型+富文本正文+配图），goods_display_block 独立表
 */
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Delete, Plus } from '@element-plus/icons-vue'
import { getGoods, updateGoods } from '@/api/goods'
import { formatFileUrl } from '@/utils/file'
import AssetPicker from '@/components/AssetPicker/index.vue'
import GoodsDisplayBlockPane from './GoodsDisplayBlockPane.vue'

const props = defineProps<{ goodsCode: string }>()

const activeName = ref('display-config')
const loading = ref(true)
const saving = ref(false)

interface DisplayConfig {
  banners: string[]
  thumbnail: string
}

const configData = reactive<DisplayConfig>({ banners: [], thumbnail: '' })

const pickerVisible = ref(false)

function thumbOf(): string {
  if (configData.thumbnail && configData.banners.includes(configData.thumbnail)) return configData.thumbnail
  return configData.banners[0] || ''
}

function setThumbnail(key: string) {
  configData.thumbnail = thumbOf() === key ? '' : key
}

function openPicker() {
  pickerVisible.value = true
}

function onPicked(keys: string[]) {
  // 防御：AssetPicker 对脏数据（assetUrl 为空）可能 emit ''，合并前过滤
  const picked = keys.filter((k) => k)
  if (!picked.length) return
  const merged = [...new Set([...configData.banners, ...picked])]
  configData.banners = merged.slice(0, 12)
  if (merged.length > configData.banners.length) ElMessage.info('轮播图最多 12 张，超出部分已忽略')
}

function moveBanner(index: number, dir: -1 | 1) {
  const arr = configData.banners
  const target = index + dir
  if (target < 0 || target >= arr.length) return
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
}

function removeBanner(index: number) {
  const arr = configData.banners
  const removed = arr[index]
  arr.splice(index, 1)
  if (configData.thumbnail === removed) configData.thumbnail = ''
}

function parseConfig(raw?: string): DisplayConfig {
  if (!raw) return { banners: [], thumbnail: '' }
  try {
    const parsed = JSON.parse(raw)
    return {
      banners: Array.isArray(parsed.banners) ? parsed.banners.map(String) : [],
      thumbnail: parsed.thumbnail ? String(parsed.thumbnail) : ''
    }
  } catch {
    return { banners: [], thumbnail: '' }
  }
}

async function loadData() {
  loading.value = true
  try {
    const goods = await getGoods(props.goodsCode)
    const cfg = parseConfig(goods.displayConfig)
    configData.banners = cfg.banners
    configData.thumbnail = cfg.thumbnail
  } catch {
    configData.banners = []
    configData.thumbnail = ''
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    await updateGoods(props.goodsCode, { displayConfig: JSON.stringify(configData) })
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

loadData()
</script>

<style scoped lang="scss">
.goods-page-config-tab {
  margin-top: 8px;
}

.display-config {
  padding: 8px 4px;

  .row {
    display: flex;
    align-items: flex-start;
    margin-bottom: 16px;

    .row-label {
      width: 90px;
      flex-shrink: 0;
      line-height: 32px;
      color: #606266;
      font-size: 14px;
    }

    .chosen-row {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      align-items: center;
    }

    .chosen-cell {
      position: relative;
      width: 120px;
      height: 120px;
      border: 2px solid transparent;
      border-radius: 6px;
      overflow: hidden;
      cursor: pointer;

      &:hover {
        .cell-ops {
          opacity: 1;
        }
      }

      &.is-thumb {
        border-color: var(--el-color-primary);
      }

      .chosen-img {
        width: 100%;
        height: 100%;
        display: block;
      }

      .order-badge {
        position: absolute;
        left: 4px;
        top: 4px;
        background: rgba(0, 0, 0, 0.55);
        color: #fff;
        font-size: 12px;
        padding: 0 6px;
        border-radius: 4px;
      }

      .thumb-badge {
        position: absolute;
        right: 4px;
        top: 4px;
        background: var(--el-color-primary);
        color: #fff;
        font-size: 12px;
        padding: 0 6px;
        border-radius: 4px;
      }

      .cell-ops {
        position: absolute;
        left: 0;
        right: 0;
        bottom: 0;
        display: flex;
        justify-content: center;
        gap: 10px;
        padding: 4px 0;
        background: rgba(0, 0, 0, 0.55);
        opacity: 0;
        transition: opacity 0.2s;

        .el-icon {
          color: #fff;
          cursor: pointer;

          &.disabled {
            opacity: 0.35;
            cursor: not-allowed;
          }
        }
      }
    }
  }

  .hint-row {
    .hint {
      line-height: 32px;
      color: #909399;
      font-size: 13px;
    }
  }

  .save-bar {
    margin-top: 8px;
  }
}
</style>
