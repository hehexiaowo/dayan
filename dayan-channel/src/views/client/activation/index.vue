<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageEquityActivates } from '@/api/equity'
import { activateChannelLabel, activateChannelTagType, type EquityActivate, type EquityActivateQuery } from '@/types/equity'
import { formatDateTime } from '@/utils/format'

/**
 * 激活记录页（只读列表）。
 *
 * - 数据源：pageEquityActivates（/channel-api/equity-activates，任务 6 新建）。
 * - 搜索：激活码 / 权益码 / 客户编码。
 * - 表格：activateCode / equityCode / goodsCode / clientFullName / clientPhone / activateChannel(tag) /
 *   activateTime / expireTime / isIdCardVerified(tag) / isAgreementSigned(tag)。
 * - 行内详情弹窗（行数据展开）：el-descriptions 展示全量字段，不调新接口。
 * - 后端端点未实现时降级（空表 + 控制台 warn，不弹 toast）。
 *
 * 说明：useCrud 不返回 handleReset（已核实 composables/useCrud.ts），
 * 故在本页面内自定义 handleReset（与 agent/account 等页面一致）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  EquityActivate,
  EquityActivateQuery
>(
  { page: pageEquityActivates },
  { initialQuery: { activateCode: '', equityCode: '', clientCode: '' } }
)

function handleReset() {
  query.activateCode = ''
  query.equityCode = ''
  query.clientCode = ''
  handleSearch()
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const currentRow = ref<EquityActivate | null>(null)

function openDetail(row: EquityActivate) {
  currentRow.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[client/activation] 加载激活记录列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input v-model="query.activateCode" placeholder="激活码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-input v-model="query.equityCode" placeholder="权益码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-input v-model="query.clientCode" placeholder="客户编码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header><div class="card-header"><span class="card-title">激活记录列表</span></div></template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="activateCode">
        <el-table-column prop="activateCode" label="激活码" min-width="140" fixed="left" show-overflow-tooltip />
        <el-table-column prop="equityCode" label="权益码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="goodsCode" label="商品编码" min-width="120" show-overflow-tooltip />
        <el-table-column label="客户姓名" min-width="100">
          <template #default="{ row }">{{ row.clientFullName || row.clientCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="clientPhone" label="手机" min-width="120" show-overflow-tooltip />
        <el-table-column prop="activateChannel" label="激活渠道" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="activateChannelTagType(row.activateChannel)">{{ activateChannelLabel(row.activateChannel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="activateTime" label="激活时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.activateTime) }}</template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
        </el-table-column>
        <el-table-column prop="isIdCardVerified" label="实名核验" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isIdCardVerified === 1 ? 'success' : 'info'">{{ row.isIdCardVerified === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isAgreementSigned" label="签署协议" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isAgreementSigned === 1 ? 'success' : 'info'">{{ row.isAgreementSigned === 1 ? '已签' : '未签' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无数据" /></template>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
:current-page="query.current" :page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" background
          @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="激活记录详情" width="720px">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="激活码">{{ currentRow.activateCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="权益码">{{ currentRow.equityCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="商品编码">{{ currentRow.goodsCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="客户编码">{{ currentRow.clientCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ currentRow.clientFullName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentRow.clientPhone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="激活渠道">{{ currentRow.activateChannel ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="激活来源编码">{{ currentRow.activateSourceCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="激活时间">{{ formatDateTime(currentRow.activateTime) }}</el-descriptions-item>
        <el-descriptions-item label="过期时间">{{ formatDateTime(currentRow.expireTime) }}</el-descriptions-item>
        <el-descriptions-item label="实名认证">
          <el-tag v-if="currentRow.isIdCardVerified != null" :type="currentRow.isIdCardVerified === 1 ? 'success' : 'info'">
            {{ currentRow.isIdCardVerified === 1 ? '是' : '否' }}
          </el-tag>
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="协议签署">
          <el-tag v-if="currentRow.isAgreementSigned != null" :type="currentRow.isAgreementSigned === 1 ? 'success' : 'info'">
            {{ currentRow.isAgreementSigned === 1 ? '是' : '否' }}
          </el-tag>
          <span v-else>--</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card { :deep(.el-card__body) { padding-bottom: 2px; } }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
