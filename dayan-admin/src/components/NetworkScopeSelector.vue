<script setup lang="ts">
/**
 * 服务网络范围选择器（通用组件）。
 *
 * 用途：服务项目编辑（定义默认网络，可精确到机构房型）与
 *      商品权益配置 rel 行（收窄商品级范围，可恢复"继承服务项目"）。
 *
 * 交互：
 * - 摘要展示当前范围（全部机构 / N家机构 · M个房型），点击"选择范围"弹窗配置；
 * - 弹窗内：单选 全部/自选；自选时机构树（勾机构=整馆全部房型，
 *   展开可只勾部分房型——随心住类需要精确到房型）+ 机构名搜索；
 * - 确定校验：自选至少 1 家机构；勾选"全部"或"恢复全部"时 emit null。
 */
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { listParks } from '@/api/park'
import { pageRoomTypes } from '@/api/park-room'
import { networkScopeSummary } from '@/types/goods-equity'
import type { NetworkScope, ParkScope } from '@/types/goods-equity'
import type { ParkRoomType } from '@/types/park'

const props = defineProps<{
  modelValue: NetworkScope | null | undefined
  /** "全部"选项的文案（rel 级传"跟随服务项目（默认）"） */
  allLabel?: string
  /** 弹窗标题 */
  title?: string
}>()

const emit = defineEmits<{ (e: 'update:modelValue', v: NetworkScope | null): void }>()

const resolvedAllLabel = computed(() => props.allLabel || '全部（该业态全部在营机构）')

// ---------- 摘要 ----------
const summary = computed(() => networkScopeSummary(props.modelValue))
const isAll = computed(() => summary.value === '全部机构')

// ---------- 弹窗 ----------
const visible = ref(false)
const dialogMode = ref<'all' | 'custom'>('all')
const filterText = ref('')
const treeRef = ref()
const loading = ref(false)

interface TreeNode {
  key: string
  label: string
  parkCode?: string
  roomTypeCode?: string
  isRoom?: boolean
  children?: TreeNode[]
}

const treeData = ref<TreeNode[]>([])

/** 打开弹窗：加载机构+房型树，回填勾选 */
async function openDialog() {
  visible.value = true
  dialogMode.value = isAll.value ? 'all' : 'custom'
  if (treeData.value.length === 0) {
    loading.value = true
    try {
      const [parks, roomPage] = await Promise.all([
        listParks(),
        pageRoomTypes({ current: 1, size: 999 })
      ])
      const rooms: ParkRoomType[] = roomPage?.records || []
      treeData.value = (parks || []).map((p) => {
        const parkCode = p.parkCode!
        const children: TreeNode[] = rooms
          .filter((r) => r.parkCode === parkCode)
          .map((r) => ({
            key: `R:${parkCode}:${r.roomTypeCode}`,
            label: r.roomTypeName,
            parkCode,
            roomTypeCode: r.roomTypeCode,
            isRoom: true
          }))
        return {
          key: `P:${parkCode}`,
          label: p.fullName,
          parkCode,
          children
        }
      })
    } catch {
      ElMessage.error('机构/房型加载失败')
    } finally {
      loading.value = false
    }
  }
  await nextTick()
  applyCheckedKeys()
}

/** 把 modelValue 回填到树勾选（整馆勾 P 键；部分房型勾 R 键） */
function applyCheckedKeys() {
  const tree = treeRef.value
  if (!tree) return
  const scope = props.modelValue
  const keys: string[] = []
  if (scope?.mode === 'custom' && scope.parks) {
    for (const p of scope.parks) {
      if (!p.roomTypeCodes || p.roomTypeCodes.length === 0) {
        keys.push(`P:${p.parkCode}`)
      } else {
        for (const rt of p.roomTypeCodes) keys.push(`R:${p.parkCode}:${rt}`)
      }
    }
  }
  tree.setCheckedKeys(keys, false)
}

watch(filterText, (v) => treeRef.value?.filter(v))

/** 从树勾选收集范围：全勾机构=整馆；半勾机构=其勾中的房型列表 */
function harvest(): ParkScope[] {
  const tree = treeRef.value
  if (!tree) return []
  const checked: TreeNode[] = tree.getCheckedNodes(false, false)
  const half: TreeNode[] = tree.getHalfCheckedNodes()
  const parks: ParkScope[] = []
  for (const node of checked) {
    if (!node.isRoom) parks.push({ parkCode: node.parkCode!, roomTypeCodes: [] })
  }
  for (const node of half) {
    const rooms = checked
      .filter((n) => n.isRoom && n.parkCode === node.parkCode)
      .map((n) => n.roomTypeCode!)
    if (rooms.length > 0) parks.push({ parkCode: node.parkCode!, roomTypeCodes: rooms })
  }
  return parks
}

function handleConfirm() {
  if (dialogMode.value === 'all') {
    emit('update:modelValue', null)
    visible.value = false
    return
  }
  const parks = harvest()
  if (parks.length === 0) {
    ElMessage.warning('自选范围至少勾选 1 家机构')
    return
  }
  emit('update:modelValue', { mode: 'custom', parks })
  visible.value = false
}

function handleClear() {
  emit('update:modelValue', null)
}
</script>

<template>
  <div class="network-scope-selector">
    <el-tag v-if="isAll" size="small" type="info">{{ resolvedAllLabel }}</el-tag>
    <el-tag v-else size="small" type="success" effect="light">{{ summary }}</el-tag>
    <el-button link type="primary" size="small" @click="openDialog">
      {{ isAll ? '自选范围' : '修改范围' }}
    </el-button>
    <el-button v-if="!isAll" link type="danger" size="small" @click="handleClear">恢复全部</el-button>

    <el-dialog
      v-model="visible"
      :title="title || '选择服务网络范围'"
      width="640px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-radio-group v-model="dialogMode">
        <el-radio value="all">{{ resolvedAllLabel }}</el-radio>
        <el-radio value="custom">自选机构范围（可精确到房型）</el-radio>
      </el-radio-group>

      <template v-if="dialogMode === 'custom'">
        <el-input
          v-model="filterText"
          placeholder="搜索机构名称"
          clearable
          size="small"
          style="margin: 10px 0 8px"
        />
        <div v-loading="loading" class="tree-wrap">
          <el-tree
            ref="treeRef"
            :data="treeData"
            show-checkbox
            node-key="key"
            :props="{ label: 'label', children: 'children' }"
            :filter-node-method="(value: string, data: TreeNode) =>
              !value || data.label.includes(value)"
            @check="() => {}"
          />
        </div>
        <div class="hint">
          勾选机构 = 该机构全部房型（整馆）；展开机构可只勾部分房型（随心住类需精确到房型）。
        </div>
      </template>

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.network-scope-selector {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tree-wrap {
  max-height: 360px;
  overflow: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  padding: 4px 8px;
}
.hint {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}
</style>
