<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageParks,
  getPark,
  createPark,
  updatePark,
  deletePark,
  transitionPark
} from '@/api/park'
import type { ParkInfo, ParkInfoQuery } from '@/types/park'
import {
  ParkOperateStatus,
  PARK_OPERATE_STATUS_OPTIONS,
  DAYAN_LEVEL_OPTIONS,
  ABILITY_TYPE_OPTIONS,
  NATURE_TYPE_OPTIONS
} from '@/types/park'
import RegionSelect from '@/components/RegionSelect.vue'

/**
 * 养老机构管理页。
 *
 * - 标准 CRUD（搜索 + 表格 + 分页 + 新增/编辑弹窗）；
 * - 机构运营状态由状态机 PARK_SM 驱动，操作列按 operateStatus 动态展示
 *   审核上线 / 下架 / 重新上线 / 暂停营业 / 恢复营业 等 transition 按钮，
 *   均走 POST /park/info/transition?parkCode=&event= 端点。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkInfo,
  ParkInfoQuery
>(
  { page: pageParks },
  {
    initialQuery: {
      fullName: '',
      cityCode: '',
      operateStatus: undefined
    }
  }
)

const router = useRouter()

/** 跳转机构详情页（主从详情页 / tab 式，管理子表） */
function goDetail(row: ParkInfo) {
  if (!row.parkCode) return
  router.push({ name: 'ParkDetail', params: { parkCode: row.parkCode } })
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ParkInfo>({
  parkCode: undefined,
  fullName: '',
  shortName: '',
  supplierCode: '',
  brand: '',
  abilityType: undefined,
  natureType: undefined,
  dayanLevel: undefined,
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  serviceHotline: '',
  totalBeds: undefined,
  availableBeds: undefined,
  baseDescription: '',
  specialtyTag: '',
  remark: ''
})

const rules: FormRules<ParkInfo> = {
  fullName: [{ required: true, message: '请输入机构全称', trigger: 'blur' }],
  supplierCode: [{ required: true, message: '请输入供应商编码', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    parkCode: undefined,
    fullName: '',
    shortName: '',
    supplierCode: '',
    brand: '',
    abilityType: undefined,
    natureType: undefined,
    dayanLevel: undefined,
    provinceCode: '',
    cityCode: '',
    districtCode: '',
    address: '',
    serviceHotline: '',
    totalBeds: undefined,
    availableBeds: undefined,
    baseDescription: '',
    specialtyTag: '',
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: ParkInfo) {
  if (!row.parkCode) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getPark(row.parkCode)
    Object.assign(form, {
      parkCode: detail.parkCode,
      fullName: detail.fullName ?? '',
      shortName: detail.shortName ?? '',
      supplierCode: detail.supplierCode ?? '',
      brand: detail.brand ?? '',
      abilityType: detail.abilityType,
      natureType: detail.natureType,
      dayanLevel: detail.dayanLevel,
      provinceCode: detail.provinceCode ?? '',
      cityCode: detail.cityCode ?? '',
      districtCode: detail.districtCode ?? '',
      address: detail.address ?? '',
      serviceHotline: detail.serviceHotline ?? '',
      totalBeds: detail.totalBeds,
      availableBeds: detail.availableBeds,
      baseDescription: detail.baseDescription ?? '',
      specialtyTag: detail.specialtyTag ?? '',
      remark: detail.remark ?? ''
    })
  } catch {
    // 拉取详情失败时回退到行数据
    Object.assign(form, {
      parkCode: row.parkCode,
      fullName: row.fullName ?? '',
      shortName: row.shortName ?? '',
      supplierCode: row.supplierCode ?? '',
      brand: row.brand ?? '',
      abilityType: row.abilityType,
      natureType: row.natureType,
      dayanLevel: row.dayanLevel,
      provinceCode: row.provinceCode ?? '',
      cityCode: row.cityCode ?? '',
      districtCode: row.districtCode ?? '',
      address: row.address ?? '',
      serviceHotline: row.serviceHotline ?? '',
      totalBeds: row.totalBeds,
      availableBeds: row.availableBeds,
      baseDescription: row.baseDescription ?? '',
      specialtyTag: row.specialtyTag ?? '',
      remark: row.remark ?? ''
    })
  }
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
    if (dialogType.value === 'create') {
      await createPark(form)
      ElMessage.success('新增成功')
    } else if (form.parkCode) {
      await updatePark(form.parkCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: ParkInfo) {
  if (!row.parkCode) return
  await ElMessageBox.confirm(`确定删除机构「${row.fullName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deletePark(row.parkCode)
  ElMessage.success('删除成功')
  loadPage()
}

function handleReset() {
  query.fullName = ''
  query.cityCode = ''
  query.operateStatus = undefined
  handleSearch()
}

// ---------- 状态机 transition 操作 ----------
/**
 * 统一 transition 处理：二次确认后调 transitionPark，成功提示并刷新列表。
 * transitionPark 返回新 operateStatus，无需使用，刷新即可。
 */
async function handleTransition(row: ParkInfo, event: string, actionLabel: string, successMsg: string) {
  if (!row.parkCode) return
  await ElMessageBox.confirm(`确定对机构「${row.fullName}」执行「${actionLabel}」操作吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await transitionPark(row.parkCode, event)
  ElMessage.success(successMsg)
  loadPage()
}

function handleApprove(row: ParkInfo) {
  void handleTransition(row, 'approve', '审核上线', '已审核上线')
}

function handleOffline(row: ParkInfo) {
  void handleTransition(row, 'offline', '下架', '已下架')
}

function handleOnline(row: ParkInfo) {
  void handleTransition(row, 'online', '重新上线', '已重新上线')
}

function handleSuspend(row: ParkInfo) {
  void handleTransition(row, 'suspend', '暂停营业', '已暂停营业')
}

function handleResume(row: ParkInfo) {
  void handleTransition(row, 'resume', '恢复营业', '已恢复营业')
}

// ---------- 辅助渲染 ----------
function operateStatusLabel(s?: number): string {
  const found = PARK_OPERATE_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/** 根据 operateStatus 返回 el-tag type：待审 warning / 上线 success / 下架 info / 暂停 danger。 */
function operateStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case ParkOperateStatus.ONLINE:
      return 'success'
    case ParkOperateStatus.PENDING:
      return 'warning'
    case ParkOperateStatus.SUSPENDED:
      return 'danger'
    case ParkOperateStatus.OFFLINE:
    default:
      return 'info'
  }
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="机构名称">
          <el-input v-model="query.fullName" placeholder="机构全称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="query.cityCode" placeholder="城市名称/编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="运营状态">
          <el-select v-model="query.operateStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in PARK_OPERATE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>机构列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增机构</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="parkCode">
        <el-table-column prop="parkCode" label="机构编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="fullName" label="机构名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="supplierCode" label="供应商编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="city" label="城市" min-width="110" show-overflow-tooltip />
        <el-table-column prop="totalBeds" label="总床位" width="90" align="center" />
        <el-table-column prop="availableBeds" label="可用床位" width="100" align="center" />
        <el-table-column prop="operateStatus" label="运营状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="operateStatusTagType(row.operateStatus)">
              {{ operateStatusLabel(row.operateStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isPublished" label="已发布" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isPublished === 1 ? 'success' : 'info'">
              {{ row.isPublished === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.operateStatus === ParkOperateStatus.PENDING"
              link
              type="success"
              size="small"
              @click="handleApprove(row)"
            >
              审核上线
            </el-button>
            <el-button
              v-if="row.operateStatus === ParkOperateStatus.ONLINE"
              link
              type="warning"
              size="small"
              @click="handleOffline(row)"
            >
              下架
            </el-button>
            <el-button
              v-if="row.operateStatus === ParkOperateStatus.OFFLINE"
              link
              type="success"
              size="small"
              @click="handleOnline(row)"
            >
              重新上线
            </el-button>
            <el-button
              v-if="row.operateStatus === ParkOperateStatus.ONLINE"
              link
              type="warning"
              size="small"
              @click="handleSuspend(row)"
            >
              暂停营业
            </el-button>
            <el-button
              v-if="row.operateStatus === ParkOperateStatus.SUSPENDED"
              link
              type="success"
              size="small"
              @click="handleResume(row)"
            >
              恢复营业
            </el-button>
            <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增机构' : '编辑机构'"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="机构编码">
              <el-input v-model="form.parkCode" placeholder="系统自动生成" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构全称" prop="fullName">
              <el-input v-model="form.fullName" placeholder="机构全称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构简称">
              <el-input v-model="form.shortName" placeholder="机构简称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商编码" prop="supplierCode">
              <el-input v-model="form.supplierCode" placeholder="供应商编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="form.brand" placeholder="品牌" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="特色标签">
              <el-input v-model="form.specialtyTag" placeholder="特色标签" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="能力类型">
              <el-select v-model="form.abilityType" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in ABILITY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="性质类型">
              <el-select v-model="form.natureType" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in NATURE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="大雁等级">
              <el-select v-model="form.dayanLevel" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in DAYAN_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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
            <el-form-item label="详细地址">
              <el-input v-model="form.address" placeholder="详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务热线">
              <el-input v-model="form.serviceHotline" placeholder="服务热线" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="总床位">
              <el-input-number
                v-model="form.totalBeds"
                :min="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="可用床位">
              <el-input-number
                v-model="form.availableBeds"
                :min="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="基地简介">
              <el-input v-model="form.baseDescription" type="textarea" :rows="3" placeholder="基地简介" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="内部备注（可选）" />
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
