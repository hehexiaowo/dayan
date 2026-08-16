<script setup lang="ts">
/**
 * 线索记录管理页（只读，菜单原名「线索池」）。
 *
 * - 线索由分享追踪自动建档（lead 域 lead_info），后台不提供新增/编辑/删除；
 * - 支持渠道筛选、关键字（姓名/手机号/微信昵称）、仅看已留资、排除已认领；
 * - 行内「时间线」抽屉展示内容/工具/海报三类互动合并视图；
 * - 路由由后端菜单（component='channel/lead/index'）自动解析，无需改路由。
 */
import { ref } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageLeads, getLeadTraces } from '@/api/lead'
import { listChannels } from '@/api/channel'
import type { LeadInfo, LeadInfoQuery, LeadTrace } from '@/types/lead'
import {
  leadSourceTypeLabel,
  leadInteractTypeLabel,
  leadInteractTagType
} from '@/types/lead'
import type { ChannelInfo } from '@/types/channel'
import { formatDateTime } from '@/utils/format'

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<LeadInfo, LeadInfoQuery>(
    { page: pageLeads },
    {
      initialQuery: {
        channelCode: undefined,
        keyword: '',
        onlyWithPhone: undefined,
        excludeClaimed: undefined
      }
    }
  )

loadPage()

// ---------- 渠道选项 ----------
const channelOptions = ref<ChannelInfo[]>([])
async function loadChannels() {
  try {
    channelOptions.value = await listChannels()
  } catch {
    channelOptions.value = []
  }
}
loadChannels()

/** 渠道编码 → 渠道名（选项未加载时回退编码本身） */
function channelName(code?: string): string {
  if (!code) return '--'
  const found = channelOptions.value.find((c) => c.channelCode === code)
  return found ? found.shortName || found.fullName : code
}

// ---------- 互动时间线抽屉 ----------
const drawerVisible = ref(false)
const tracesLoading = ref(false)
const traces = ref<LeadTrace[]>([])
const currentLead = ref<LeadInfo | null>(null)

async function openTraces(row: LeadInfo) {
  if (!row.leadCode) return
  currentLead.value = row
  drawerVisible.value = true
  tracesLoading.value = true
  try {
    traces.value = await getLeadTraces(row.leadCode)
  } finally {
    tracesLoading.value = false
  }
}

/** 展示名：姓名 > 微信昵称 > 匿名访客 */
function displayName(row: LeadInfo): string {
  return row.name || row.wxNickname || '匿名访客'
}
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-select v-model="query.channelCode" placeholder="渠道" clearable filterable style="width: 180px">
          <el-option
            v-for="c in channelOptions"
            :key="c.channelCode"
            :label="c.shortName || c.fullName"
            :value="c.channelCode"
          />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="姓名 / 手机号 / 微信昵称"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-checkbox v-model="query.onlyWithPhone">仅看已留资</el-checkbox>
        <el-checkbox v-model="query.excludeClaimed">排除已认领</el-checkbox>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        </div>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe row-key="leadCode">
        <el-table-column prop="leadCode" label="线索编码" width="110" />
        <el-table-column label="访客" min-width="150">
          <template #default="{ row }">
            <div class="visitor-cell">
              <el-avatar :size="28" :src="row.wxAvatar || undefined">{{ displayName(row).charAt(0) }}</el-avatar>
              <span>{{ displayName(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="130">
          <template #default="{ row }">
            <span v-if="row.phone">{{ row.phone }}</span>
            <el-tag v-else type="info" size="small">未留资</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="渠道" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ channelName(row.channelCode) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="100" align="center">
          <template #default="{ row }">{{ leadSourceTypeLabel(row.sourceType) }}</template>
        </el-table-column>
        <el-table-column label="访客环境" width="90" align="center">
          <template #default="{ row }">{{ row.visitorSource || '--' }}</template>
        </el-table-column>
        <el-table-column prop="interactCount" label="互动次数" width="90" align="center">
          <template #default="{ row }">{{ row.interactCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="最后互动" width="170" align="center">
          <template #default="{ row }">
            <template v-if="row.lastInteractTime">
              <el-tag :type="leadInteractTagType(row.lastInteractType)" size="small">
                {{ leadInteractTypeLabel(row.lastInteractType) }}
              </el-tag>
              {{ formatDateTime(row.lastInteractTime) }}
            </template>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="关联客户" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.clientCode" type="success" size="small">{{ row.clientCode }}</el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="建档时间" width="160" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'lead:info:query'" link type="primary" size="small" @click="openTraces(row)">
              时间线
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          :current-page="query.current"
          :page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 互动时间线抽屉 -->
    <el-drawer v-model="drawerVisible" :title="`互动时间线（${currentLead ? displayName(currentLead) : ''}）`" size="480px">
      <div v-loading="tracesLoading" class="trace-wrap">
        <el-empty v-if="!tracesLoading && traces.length === 0" description="暂无互动记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="t in traces"
            :key="t.id"
            :timestamp="formatDateTime(t.traceTime)"
            placement="top"
          >
            <el-tag :type="leadInteractTagType(t.traceType)" size="small">
              {{ leadInteractTypeLabel(t.traceType) }}
            </el-tag>
            <span class="trace-title">{{ t.bizTitle || t.bizCode || '--' }}</span>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;

  .toolbar-actions {
    display: flex;
    gap: 8px;
    margin-left: auto;
  }
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.visitor-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.trace-wrap {
  padding: 0 4px;
}
.trace-title {
  margin-left: 8px;
}
</style>
