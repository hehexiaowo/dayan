<script setup lang="ts">
import { onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageClients } from '@/api/client'
import {
  CLIENT_TYPE_OPTIONS,
  GENDER_OPTIONS,
  type Client,
  type ClientQuery
} from '@/types/client'

/**
 * 客户管理页。
 *
 * - 搜索栏：客户名称 / 手机号 / 客户类型；
 * - el-table：clientCode / clientName / phone / clientType / gender；
 * - 后端 GET /channel-api/clients 未实现，onMounted 失败时降级（空表，不弹 toast）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  Client,
  ClientQuery
>(
  { page: pageClients },
  {
    initialQuery: {
      clientCode: '',
      clientName: '',
      phone: '',
      clientType: undefined
    }
  }
)

function handleReset() {
  query.clientCode = ''
  query.clientName = ''
  query.phone = ''
  query.clientType = undefined
  handleSearch()
}

function clientTypeText(v?: number) {
  const opt = CLIENT_TYPE_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

function genderText(v?: number) {
  const opt = GENDER_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '未知'
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[client] 加载客户列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="客户编码">
          <el-input v-model="query.clientCode" placeholder="客户编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.clientName" placeholder="客户名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="手机号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="客户类型">
          <el-select v-model="query.clientType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in CLIENT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>客户列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="clientCode">
        <el-table-column prop="clientCode" label="客户编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="clientName" label="客户名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="clientType" label="客户类型" width="100" align="center">
          <template #default="{ row }">{{ clientTypeText(row.clientType) }}</template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
