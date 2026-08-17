<script setup lang="ts">
/**
 * 渠道详情页 - 分发配置 tab。
 *
 * 与其他子表完全不同的模式：list + save 全量覆盖（非逐条 CRUD）。
 * 一个 tab 内嵌套 el-tabs 切 3 类配置（content / scene / goods），每类：
 *   - loadXxxConfigs(channelCode) 拉整张表
 *   - 可编辑 el-table（行内新增/删除，纯前端操作）
 *   - 「保存配置」按钮调 saveXxxConfigs(channelCode, List) 全量覆盖（后端先删后增）
 *
 * 后端 save 语义：setId(null) 忽略入参 id，强制 channelCode。前端新增行不需带 id。
 *
 * 注：sceneCode/goodsCode/contentCode 是关联到主档案的业务编码，本次用 el-input 手填
 * （后续可优化为 el-select 联动 scene/goods 主数据，但需额外拉取主档案列表，本次先简版）。
 */
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listContentConfigs,
  saveContentConfigs,
  listSceneConfigs,
  saveSceneConfigs,
  listGoodsConfigs,
  saveGoodsConfigs
} from '@/api/channel-sub'
import {
  CHANNEL_CONTENT_TYPE_OPTIONS,
  CHANNEL_APP_TYPE_OPTIONS,
  CHANNEL_CONTENT_POSITION_OPTIONS,
  CHANNEL_CONFIG_STATUS_OPTIONS,
  CHANNEL_YES_NO_OPTIONS
} from '@/types/channel'
import type {
  ChannelConfigContent,
  ChannelConfigScene,
  ChannelConfigGoods
} from '@/types/channel'

const props = defineProps<{
  channelCode: string
}>()

const activeSubTab = ref<'content' | 'scene' | 'goods'>('content')

// ==================== 内容配置 ====================
const contentLoading = ref(false)
const contentSaving = ref(false)
const contentList = ref<ChannelConfigContent[]>([])

async function loadContent() {
  contentLoading.value = true
  try {
    contentList.value = await listContentConfigs(props.channelCode)
  } catch {
    contentList.value = []
  } finally {
    contentLoading.value = false
  }
}

function addContentRow() {
  contentList.value.push({
    channelCode: props.channelCode,
    contentCode: '',
    contentType: 1,
    appType: 'agent',
    position: 'recommend',
    sortOrder: 0,
    isTop: 0,
    status: 1
  })
}

function removeContentRow(index: number) {
  contentList.value.splice(index, 1)
}

async function saveContent() {
  // 校验：contentCode 必填
  const invalid = contentList.value.find((c) => !c.contentCode)
  if (invalid) {
    ElMessage.warning('存在内容编码为空的行，请补全或删除')
    return
  }
  contentSaving.value = true
  try {
    await saveContentConfigs(props.channelCode, contentList.value)
    ElMessage.success('内容配置已保存')
    await loadContent()
  } finally {
    contentSaving.value = false
  }
}

// ==================== 场景配置 ====================
const sceneLoading = ref(false)
const sceneSaving = ref(false)
const sceneList = ref<ChannelConfigScene[]>([])

async function loadScene() {
  sceneLoading.value = true
  try {
    sceneList.value = await listSceneConfigs(props.channelCode)
  } catch {
    sceneList.value = []
  } finally {
    sceneLoading.value = false
  }
}

function addSceneRow() {
  sceneList.value.push({
    channelCode: props.channelCode,
    sceneCode: '',
    isExclusive: 0,
    customName: '',
    customPrice: undefined,
    sortOrder: 0,
    status: 1
  })
}

function removeSceneRow(index: number) {
  sceneList.value.splice(index, 1)
}

async function saveScene() {
  const invalid = sceneList.value.find((s) => !s.sceneCode)
  if (invalid) {
    ElMessage.warning('存在场景编码为空的行，请补全或删除')
    return
  }
  sceneSaving.value = true
  try {
    await saveSceneConfigs(props.channelCode, sceneList.value)
    ElMessage.success('场景配置已保存')
    await loadScene()
  } finally {
    sceneSaving.value = false
  }
}

// ==================== 商品配置 ====================
const goodsLoading = ref(false)
const goodsSaving = ref(false)
const goodsList = ref<ChannelConfigGoods[]>([])

async function loadGoods() {
  goodsLoading.value = true
  try {
    goodsList.value = await listGoodsConfigs(props.channelCode)
  } catch {
    goodsList.value = []
  } finally {
    goodsLoading.value = false
  }
}

function addGoodsRow() {
  goodsList.value.push({
    channelCode: props.channelCode,
    goodsCode: '',
    goodsType: 1,
    customName: '',
    customPrice: undefined,
    customDescription: '',
    isExclusive: 0,
    purchaseLimit: undefined,
    sortOrder: 0,
    status: 1
  })
}

function removeGoodsRow(index: number) {
  goodsList.value.splice(index, 1)
}

async function saveGoods() {
  const invalid = goodsList.value.find((g) => !g.goodsCode)
  if (invalid) {
    ElMessage.warning('存在商品编码为空的行，请补全或删除')
    return
  }
  goodsSaving.value = true
  try {
    await saveGoodsConfigs(props.channelCode, goodsList.value)
    ElMessage.success('商品配置已保存')
    await loadGoods()
  } finally {
    goodsSaving.value = false
  }
}

// ==================== 整体清空 ====================
async function clearAll(type: 'content' | 'scene' | 'goods') {
  await ElMessageBox.confirm('确定清空当前渠道的该类配置吗？保存后生效。', '清空配置', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning'
  })
  if (type === 'content') contentList.value = []
  else if (type === 'scene') sceneList.value = []
  else goodsList.value = []
  ElMessage.success('已清空，点击「保存配置」生效')
}

// 初始加载（tab 懒加载，首次进入 content 子 tab 时加载）
watch(
  () => props.channelCode,
  (code) => {
    if (code) loadContent()
  },
  { immediate: true }
)

// 切换子 tab 时按需加载（避免一次拉 3 类）
function handleSubTabChange(name: string | number) {
  if (name === 'scene' && sceneList.value.length === 0 && !sceneLoading.value) loadScene()
  else if (name === 'goods' && goodsList.value.length === 0 && !goodsLoading.value) loadGoods()
}
</script>

<template>
  <div class="config-tab">
    <el-alert
      type="info"
      :closable="false"
      title="分发配置采用「整体保存」模式：可新增/删除行（仅前端），点击「保存配置」后端先删后增全量覆盖。contentCode/sceneCode/goodsCode 为关联主档案的业务编码。"
      show-icon
      style="margin-bottom: 12px"
    />

    <el-tabs v-model="activeSubTab" type="card" @tab-change="handleSubTabChange">
      <!-- ========== 内容配置 ========== -->
      <el-tab-pane label="内容配置" name="content">
        <div class="sub-toolbar">
          <span class="count">共 {{ contentList.length }} 条</span>
          <div class="sub-toolbar-actions">
            <el-button type="primary" :icon="'Plus'" @click="addContentRow">新增行</el-button>
            <el-button :icon="'Delete'" size="small" @click="clearAll('content')">清空</el-button>
            <el-button type="success" :icon="'Check'" size="small" :loading="contentSaving" @click="saveContent">
              保存配置
            </el-button>
          </div>
        </div>
        <el-table v-loading="contentLoading" :data="contentList" border stripe size="small" row-key="contentCode">
          <el-table-column label="内容编码" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.contentCode" placeholder="内容编码" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="内容类型" width="120">
            <template #default="{ row }">
              <el-select v-model="row.contentType" size="small" style="width: 100%">
                <el-option v-for="o in CHANNEL_CONTENT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="展示端" width="110">
            <template #default="{ row }">
              <el-select v-model="row.appType" size="small" style="width: 100%">
                <el-option v-for="o in CHANNEL_APP_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="展示位置" width="120">
            <template #default="{ row }">
              <el-select v-model="row.position" size="small" clearable style="width: 100%">
                <el-option v-for="o in CHANNEL_CONTENT_POSITION_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="置顶" width="90" align="center">
            <template #default="{ row }">
              <el-select v-model="row.isTop" size="small" style="width: 100%">
                <el-option v-for="o in CHANNEL_YES_NO_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="排序" width="90" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.sortOrder" :min="0" :max="9999" size="small" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-select v-model="row.status" size="small" style="width: 100%">
                <el-option v-for="o in CHANNEL_CONFIG_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="生效时间" width="160">
            <template #default="{ row }">
              <el-date-picker v-model="row.effectiveTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" size="small" style="width: 100%" placeholder="选择" />
            </template>
          </el-table-column>
          <el-table-column label="失效时间" width="160">
            <template #default="{ row }">
              <el-date-picker v-model="row.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" size="small" style="width: 100%" placeholder="选择" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="removeContentRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!contentLoading && contentList.length === 0" description="暂无内容配置，点击「新增行」" />
      </el-tab-pane>

      <!-- ========== 场景配置 ========== -->
      <el-tab-pane label="场景配置" name="scene">
        <div class="sub-toolbar">
          <span class="count">共 {{ sceneList.length }} 条</span>
          <div class="sub-toolbar-actions">
            <el-button type="primary" :icon="'Plus'" @click="addSceneRow">新增行</el-button>
            <el-button :icon="'Delete'" size="small" @click="clearAll('scene')">清空</el-button>
            <el-button type="success" :icon="'Check'" size="small" :loading="sceneSaving" @click="saveScene">
              保存配置
            </el-button>
          </div>
        </div>
        <el-table v-loading="sceneLoading" :data="sceneList" border stripe size="small" row-key="sceneCode">
          <el-table-column label="场景编码" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.sceneCode" placeholder="关联场景编码" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="自定义名称" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.customName" placeholder="渠道定制名称（可选）" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="自定义价格" width="130" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.customPrice" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" placeholder="不限" />
            </template>
          </el-table-column>
          <el-table-column label="渠道专属" width="110" align="center">
            <template #default="{ row }">
              <el-select v-model="row.isExclusive" size="small" style="width: 100%">
                <el-option v-for="o in CHANNEL_YES_NO_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="排序" width="90" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.sortOrder" :min="0" :max="9999" size="small" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-select v-model="row.status" size="small" style="width: 100%">
                <el-option v-for="o in CHANNEL_CONFIG_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="生效时间" width="160">
            <template #default="{ row }">
              <el-date-picker v-model="row.effectiveTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" size="small" style="width: 100%" placeholder="选择" />
            </template>
          </el-table-column>
          <el-table-column label="失效时间" width="160">
            <template #default="{ row }">
              <el-date-picker v-model="row.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" size="small" style="width: 100%" placeholder="选择" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="removeSceneRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!sceneLoading && sceneList.length === 0" description="暂无场景配置，点击「新增行」" />
      </el-tab-pane>

      <!-- ========== 商品配置 ========== -->
      <el-tab-pane label="商品配置" name="goods">
        <div class="sub-toolbar">
          <span class="count">共 {{ goodsList.length }} 条</span>
          <div class="sub-toolbar-actions">
            <el-button type="primary" :icon="'Plus'" @click="addGoodsRow">新增行</el-button>
            <el-button :icon="'Delete'" size="small" @click="clearAll('goods')">清空</el-button>
            <el-button type="success" :icon="'Check'" size="small" :loading="goodsSaving" @click="saveGoods">
              保存配置
            </el-button>
          </div>
        </div>
        <el-table v-loading="goodsLoading" :data="goodsList" border stripe size="small" row-key="goodsCode">
          <el-table-column label="商品编码" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.goodsCode" placeholder="关联商品编码" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="商品类型" width="110" align="center">
            <template #default="{ row }">
              <el-select v-model="row.goodsType" size="small" style="width: 100%">
                <el-option :value="1" label="养老权益" />
                <el-option :value="2" label="场景营销" />
                <el-option :value="3" label="培训课程" />
                <el-option :value="4" label="旅游短居" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="自定义名称" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.customName" placeholder="渠道定制名称（可选）" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="自定义价格" width="130" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.customPrice" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="渠道专属" width="100" align="center">
            <template #default="{ row }">
              <el-select v-model="row.isExclusive" size="small" style="width: 100%">
                <el-option v-for="o in CHANNEL_YES_NO_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="采购限制" width="110" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.purchaseLimit" :min="0" size="small" controls-position="right" style="width: 100%" placeholder="不限" />
            </template>
          </el-table-column>
          <el-table-column label="自定义描述" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.customDescription" placeholder="渠道定制描述（可选）" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="排序" width="90" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.sortOrder" :min="0" :max="9999" size="small" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-select v-model="row.status" size="small" style="width: 100%">
                <el-option v-for="o in CHANNEL_CONFIG_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="removeGoodsRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!goodsLoading && goodsList.length === 0" description="暂无商品配置，点击「新增行」" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped lang="scss">
.config-tab {
  .sub-toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    .count {
      color: var(--el-text-color-secondary);
      font-size: 13px;
    }
    .sub-toolbar-actions {
      display: flex;
      gap: 8px;
      margin-left: auto;
    }
  }
}
</style>
