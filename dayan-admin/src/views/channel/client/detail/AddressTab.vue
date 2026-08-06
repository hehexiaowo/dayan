<script setup lang="ts">
/**
 * 客户详情页 - 收货地址 tab。
 *
 * 数据模式：by-client list（GET /addresses/by-client/{clientCode} 返回 List，非分页）。
 * 采用方案 A：手动 ref<ClientAddress[]> + list 调用 + 增删改/设默认后重新 list。
 *
 * 关键约束：
 * - fullAddress 是后端拼装字段（province+city+district+detail），编辑表单不含，只编辑
 *   provinceCode/cityCode/districtCode/detailAddress，用 RegionSelect 组件。
 * - 默认地址互斥：后端自动处理。前端列表"设为默认"按钮（当 isDefault !== 1 时显示），
 *   调用 PUT /addresses/{id}/default。
 * - 单客户地址上限 20 条（后端校验，前端在新增前可选提示）。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listAddressesByClient,
  createAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress
} from '@/api/client-sub'
import type { ClientAddress } from '@/types/client'
import RegionSelect from '@/components/RegionSelect.vue'

const props = defineProps<{
  /** 客户编码（路由参数） */
  clientCode: string
}>()

/** 单客户地址上限（后端校验，前端新增前可选提示） */
const ADDRESS_LIMIT = 20

// ---------- 列表（by-client list，非分页） ----------
const loading = ref(false)
const tableData = ref<ClientAddress[]>([])

async function loadList() {
  if (!props.clientCode) return
  loading.value = true
  try {
    tableData.value = await listAddressesByClient(props.clientCode)
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

loadList()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ClientAddress>({
  id: undefined,
  clientCode: '',
  receiverName: '',
  receiverPhone: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  detailAddress: '',
  tag: '',
  isDefault: 0
})

const rules: FormRules<ClientAddress> = {
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  receiverPhone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    clientCode: '',
    receiverName: '',
    receiverPhone: '',
    provinceCode: '',
    cityCode: '',
    districtCode: '',
    detailAddress: '',
    tag: '',
    isDefault: 0
  })
}

function openCreate() {
  if (tableData.value.length >= ADDRESS_LIMIT) {
    ElMessage.warning(`单客户地址上限 ${ADDRESS_LIMIT} 条，已达上限`)
    return
  }
  dialogMode.value = 'create'
  resetForm()
  form.clientCode = props.clientCode
  dialogVisible.value = true
}

function openEdit(row: ClientAddress) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    clientCode: row.clientCode,
    receiverName: row.receiverName ?? '',
    receiverPhone: row.receiverPhone ?? '',
    provinceCode: row.provinceCode ?? '',
    cityCode: row.cityCode ?? '',
    districtCode: row.districtCode ?? '',
    detailAddress: row.detailAddress ?? '',
    tag: row.tag ?? '',
    isDefault: row.isDefault ?? 0
  })
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
      await createAddress(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateAddress(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ClientAddress) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该地址吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteAddress(row.id)
  ElMessage.success('删除成功')
  loadList()
}

async function handleSetDefault(row: ClientAddress) {
  if (!row.id) return
  await ElMessageBox.confirm(
    '设为默认地址将自动取消该客户的其他默认地址，是否继续？',
    '设为默认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await setDefaultAddress(row.id)
  ElMessage.success('已设为默认地址')
  loadList()
}

// ---------- 辅助渲染 ----------
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}

defineExpose({ loadList })
</script>

<template>
  <div class="address-tab">
    <div class="toolbar">
      <el-button type="primary" :icon="'Plus'" @click="openCreate">新增地址</el-button>
      <span class="limit-tip">单客户地址上限 {{ ADDRESS_LIMIT }} 条</span>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="receiverName" label="收件人" width="110" show-overflow-tooltip />
      <el-table-column prop="receiverPhone" label="联系电话" width="130" />
      <el-table-column prop="fullAddress" label="完整地址" min-width="280" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.fullAddress || [row.provinceCode, row.cityCode, row.districtCode, row.detailAddress].filter(Boolean).join('') }}
        </template>
      </el-table-column>
      <el-table-column prop="tag" label="标签" width="100" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag v-if="row.tag" size="small">{{ row.tag }}</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="默认地址" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="120" align="center">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.isDefault !== 1"
            link
            type="success"
            size="small"
            @click="handleSetDefault(row)"
          >
            设为默认
          </el-button>
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增地址' : '编辑地址'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="收件人">
              <el-input v-model="form.receiverName" placeholder="收件人姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="receiverPhone">
              <el-input v-model="form.receiverPhone" placeholder="联系电话" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所在地区">
              <RegionSelect
                v-model:province-code="form.provinceCode"
                v-model:city-code="form.cityCode"
                v-model:district-code="form.districtCode"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址" prop="detailAddress">
              <el-input
                v-model="form.detailAddress"
                type="textarea"
                :rows="2"
                placeholder="详细地址（不含省市区）"
                maxlength="200"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地址标签">
              <el-input v-model="form.tag" placeholder="如 家/公司" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否默认">
              <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.address-tab {
  .toolbar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
  }
  .limit-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
