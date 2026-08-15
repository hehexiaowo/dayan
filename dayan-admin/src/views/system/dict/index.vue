<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listDictTypes,
  listAllDictByType,
  createDict,
  updateDict,
  deleteDict
} from '@/api/dict'
import { DICT_TYPE_OPTIONS, type SystemDict } from '@/types/dict'

/**
 * 字典管理页（统一单表 system_dict，54 迁移合并原通用/业务双字典）。
 *
 * - 左侧字典类型从后端 listDictTypes() 动态加载（根治硬编码脱节）；
 * - 右侧展示该类型全部字典项（含禁用，管理用），支持新增/编辑/删除；
 * - 业务语义类型以「业务域」标注所属域（通用字典留空）；
 * - 两级关联分类：二级条目以「父级编码」挂到一级编码（可跨类型，
 *   如 asset_ref_type2.room_type → asset_ref_type1.park；父级为空=顶级/通用）；
 * - 切换类型显式 loadData（不再 watch+click 双触发）。
 */

const loading = ref(false)
const tableData = ref<SystemDict[]>([])
/** 字典类型列表（动态加载，失败时回退到预设） */
const dictTypes = ref<string[]>(DICT_TYPE_OPTIONS.map((o) => o.value))
/** 当前选中的字典类型 */
const currentType = ref<string>(dictTypes.value[0])

/** 拉取当前类型的全部字典项（含禁用） */
async function loadData() {
  if (!currentType.value) {
    tableData.value = []
    return
  }
  loading.value = true
  try {
    tableData.value = await listAllDictByType(currentType.value)
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

/** 加载字典类型枚举 */
async function loadTypes() {
  try {
    const types = await listDictTypes()
    if (types.length) dictTypes.value = types
  } catch {
    // 接口失败时保留 DICT_TYPE_OPTIONS 回退
  }
  // 当前类型不在列表中则切到第一个
  if (dictTypes.value.length && !dictTypes.value.includes(currentType.value)) {
    currentType.value = dictTypes.value[0]
  }
  loadData()
}

/** 切换字典类型（显式加载，避免重复请求） */
function handleTypeChange(type: string) {
  currentType.value = type
  loadData()
}

// ---------------- 新增/编辑弹窗 ----------------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

function defaultForm(): SystemDict {
  return {
    dictType: currentType.value,
    dictCode: '',
    dictName: '',
    dictValue: '',
    parentCode: null,
    level: 1,
    domain: null,
    sortOrder: 0,
    icon: null,
    cssClass: null,
    status: 1,
    isDefault: 0,
    remark: null
  }
}

const form = reactive<SystemDict>(defaultForm())

const rules: FormRules<SystemDict> = {
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }]
}

function openCreate() {
  dialogMode.value = 'create'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEdit(row: SystemDict) {
  dialogMode.value = 'edit'
  Object.assign(form, defaultForm(), row)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createDict(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateDict(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

async function onDelete(row: SystemDict) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确定删除字典项「${row.dictName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteDict(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => {
  loadTypes()
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
          <el-menu :default-active="currentType" class="type-menu" @select="handleTypeChange">
            <el-menu-item v-for="t in dictTypes" :key="t" :index="t">
              {{ t }}
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <!-- 右侧：字典项列表（可增删改） -->
      <el-col :span="19" :xs="24">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">字典项（{{ currentType }}）</span>
              <el-button type="primary" size="small" @click="openCreate">新增字典项</el-button>
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
            <el-table-column label="业务域" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.domain" size="small" type="success">{{ row.domain }}</el-tag>
                <span v-else class="text-muted">通用</span>
              </template>
            </el-table-column>
            <el-table-column prop="parentCode" label="父级编码" width="110" align="center" show-overflow-tooltip>
              <template #default="{ row }">{{ row.parentCode || '—' }}</template>
            </el-table-column>
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
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增字典项' : '编辑字典项'"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="字典类型">
              <el-input v-model="form.dictType" :disabled="dialogMode === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字典编码" prop="dictCode">
              <el-input v-model="form.dictCode" :disabled="dialogMode === 'edit'" placeholder="类型内唯一" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字典名称" prop="dictName">
              <el-input v-model="form.dictName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字典值" prop="dictValue">
              <el-input v-model="form.dictValue" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务域">
              <el-input v-model="form.domain" placeholder="如 park/content（通用字典留空）" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父级编码">
              <el-input v-model="form.parentCode" placeholder="二级分类填对应一级编码，顶级留空" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="层级">
              <el-input-number v-model="form.level" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否默认">
              <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
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
  }

  .text-muted {
    color: #c0c4cc;
  }
}
</style>
