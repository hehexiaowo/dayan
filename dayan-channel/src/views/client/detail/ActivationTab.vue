<script setup lang="ts">
/**
 * 客户详情页 - 激活记录 tab（只读分页列表）。
 *
 * 手动 ref 分页调 pageEquityActivates({ clientCode, current, size })，
 * 表格列：激活码 / 权益编码 / 商品编码 / 激活时间 / 过期时间 / 实名状态，无操作列。
 * 接口失败 try/catch 降级为空列表。
 */
import { ref, watch } from 'vue'
import { pageEquityActivates } from '@/api/equity'
import type { EquityActivate } from '@/types/equity'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 客户编码（路由参数） */
  clientCode: string
}>()

const loading = ref(false)
const tableData = ref<EquityActivate[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)

async function loadPage() {
  if (!props.clientCode) return
  loading.value = true
  try {
    const res = await pageEquityActivates({
      clientCode: props.clientCode,
      current: current.value,
      size: size.value
    })
    tableData.value = res.records ?? []
    total.value = res.total ?? 0
  } catch (err) {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[client/detail ActivationTab] 加载激活记录失败（接口可能未实现）:', err)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

loadPage()

/** 翻页 */
function handlePageChange(page: number) {
  current.value = page
  loadPage()
}

/** 每页条数变化 */
function handleSizeChange(s: number) {
  size.value = s
  current.value = 1
  loadPage()
}

// 路由参数变化（同一组件实例复用）时回到第 1 页重查
watch(
  () => props.clientCode,
  () => {
    current.value = 1
    loadPage()
  }
)

/** 实名状态文本（isIdCardVerified：1 已实名 / 0 未实名） */
function idVerifiedText(v?: number): string {
  if (v == null) return '--'
  return v === 1 ? '已实名' : '未实名'
}

/** 实名状态 tag 类型 */
function idVerifiedTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}
</script>

<template>
  <div class="activation-tab">
    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="activateCode" label="激活码" min-width="130" show-overflow-tooltip />
      <el-table-column prop="equityCode" label="权益编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="goodsCode" label="商品编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="activateTime" label="激活时间" min-width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.activateTime) }}</template>
      </el-table-column>
      <el-table-column prop="expireTime" label="过期时间" min-width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
      </el-table-column>
      <el-table-column label="实名状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isIdCardVerified != null" :type="idVerifiedTagType(row.isIdCardVerified)" size="small">
            {{ idVerifiedText(row.isIdCardVerified) }}
          </el-tag>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无数据" />
      </template>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        :current-page="current"
        :page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.activation-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
