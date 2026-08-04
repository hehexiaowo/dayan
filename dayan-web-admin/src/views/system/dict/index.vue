<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { listDictByType } from '@/api/dict'
import { DICT_TYPE_OPTIONS, type SystemDictCommon } from '@/types/dict'

/**
 * 字典管理页（只读）。
 *
 * 后端字典 controller 仅提供查询（无 CRUD，字典由 seed 初始化）。
 * 左侧选择字典类型，右侧展示该类型的字典项列表。
 */

const loading = ref(false)
const tableData = ref<SystemDictCommon[]>([])
/** 当前选中的字典类型 */
const currentType = ref<string>(DICT_TYPE_OPTIONS[0].value)

/** 拉取当前类型的字典项 */
async function loadData() {
  if (!currentType.value) {
    tableData.value = []
    return
  }
  loading.value = true
  try {
    tableData.value = await listDictByType(currentType.value)
  } catch (err) {
    // 错误消息已由响应拦截器统一提示
    tableData.value = []
    void err
  } finally {
    loading.value = false
  }
}

/** 切换字典类型 */
function handleTypeChange(type: string) {
  currentType.value = type
  loadData()
}

// 类型变化时重新拉取（兜底，确保 watch 与点击两条路径都能触发）
watch(currentType, () => {
  loadData()
})

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="dict-page">
    <el-row :gutter="16" class="dict-row">
      <!-- 左侧：字典类型选择 -->
      <el-col :span="5" :xs="24">
        <el-card shadow="never" class="type-card">
          <template #header>
            <span class="card-title">字典类型</span>
          </template>
          <el-menu
            :default-active="currentType"
            class="type-menu"
            @select="handleTypeChange"
          >
            <el-menu-item
              v-for="item in DICT_TYPE_OPTIONS"
              :key="item.value"
              :index="item.value"
            >
              {{ item.label }}
              <span class="type-code">{{ item.value }}</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <!-- 右侧：字典项列表（只读） -->
      <el-col :span="19" :xs="24">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">字典项（{{ currentType }}）</span>
              <el-tag type="info" size="small">只读</el-tag>
            </div>
          </template>

          <el-table
            v-loading="loading"
            :data="tableData"
            border
            stripe
            empty-text="暂无字典数据"
          >
            <el-table-column prop="dictCode" label="编码" min-width="140" />
            <el-table-column prop="dictName" label="名称" min-width="140" />
            <el-table-column prop="dictValue" label="值" min-width="120" />
            <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="默认" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault === 1" type="warning" size="small">默认</el-tag>
                <span v-else class="text-muted">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.dict-page {
  .dict-row {
    align-items: stretch;
  }

  .type-card {
    min-height: 100%;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .card-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2329;
  }

  .type-menu {
    border-right: none;

    .el-menu-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .type-code {
      font-size: 12px;
      color: #8a8f99;
      margin-left: 8px;
    }
  }

  .text-muted {
    color: #c0c4cc;
  }
}
</style>
