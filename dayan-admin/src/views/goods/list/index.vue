<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageGoods,
  getGoods,
  createGoods,
  updateGoods,
  deleteGoods,
  shelfGoods
} from '@/api/goods'
import type { GoodsInfo, GoodsInfoQuery } from '@/types/goods'
import {
  GoodsType,
  GoodsStatus,
  GoodsAuditStatus,
  GOODS_TYPE_OPTIONS,
  GOODS_STATUS_OPTIONS,
  GOODS_AUDIT_STATUS_OPTIONS
} from '@/types/goods'
import FileUploader from '@/components/FileUploader/index.vue'
import { useBusinessDictOptions } from '@/composables/useBusinessDict'

/** 商品分类选项（业务字典 goods_category，管理入口：系统管理-业务字典） */
const { options: categoryOptions } = useBusinessDictOptions('goods_category')

/** 分类编码 → 分类名（未命中字典时原样展示编码） */
function categoryName(code?: string): string {
  if (!code) return '--'
  return categoryOptions.value.find((o) => o.dictCode === code)?.dictName ?? code
}

/**
 * 商品管理页。
 *
 * - 标准 CRUD（搜索 + 表格 + 分页 + 新增/编辑弹窗）；
 * - 上下架流：shelf（上架/下架切换）；
 * - 主键 goodsCode 由服务端 CodeGenerator 生成，新增表单不含该字段；
 * - 修改走 PUT path（goodsCode 在 URL 上，与 distributor/supplier 的 query string 不同）。
 * - 详情入口：操作列「详情」按钮跳转 GoodsDetail（按 goodsType 显示对应 SKU 子表）。
 *
 * 状态约定（对齐 DDL，5 态 / 4 值）：
 * - goodsType：1权益商品 / 2场景商品 / 3课程商品 / 4旅居商品
 * - goodsStatus：0草稿 / 1待上架 / 2已上架 / 3已下架 / 4已售罄
 * - auditStatus：0待审 / 1通过 / 2驳回
 *
 * shelf 接口语义偏差（已知遗留）：shelf 传 0/1（0下架/1上架），DDL 的 1 是"待上架"而非"已上架"。
 * 前端列表按 DDL 5 态展示，判断是否上架用 `goodsStatus === 2`（ON_SHELF），shelf 按契约传 0/1。
 */

const router = useRouter()

/** 跳转商品详情页（主从详情页 / 动态 tab，按 goodsType 显示对应 SKU 子表） */
function goDetail(row: GoodsInfo) {
  if (!row.goodsCode) return
  router.push({ name: 'GoodsDetail', params: { goodsCode: row.goodsCode } })
}

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<GoodsInfo, GoodsInfoQuery>(
  { page: pageGoods },
  {
    initialQuery: {
      goodsCode: '',
      goodsName: '',
      goodsType: undefined,
      categoryCode: '',
      goodsStatus: undefined,
      auditStatus: undefined
    }
  }
)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<GoodsInfo>({
  goodsCode: undefined,
  goodsName: '',
  goodsShortName: '',
  goodsType: GoodsType.EQUITY,
  categoryCode: '',
  brandName: '',
  coverImage: '',
  imageUrls: '',
  videoUrl: '',
  goodsDescription: '',
  summary: '',
  originalPrice: undefined,
  salePrice: undefined,
  costPrice: undefined,
  priceUnit: '',
  stock: undefined,
  isHot: 0,
  isNew: 0,
  isRecommend: 0,
  sortOrder: 0,
  goodsStatus: GoodsStatus.DRAFT,
  remark: ''
})

const rules: FormRules<GoodsInfo> = {
  goodsName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  goodsType: [{ required: true, message: '请选择商品类型', trigger: 'change' }]
}

/** imageUrls：后端是 string（逗号分隔或 JSON 数组），FileUploader 多图用 string[] */
const imageUrlsModel = computed<string[]>({
  get() {
    const raw = form.imageUrls
    if (!raw) return []
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) return parsed.filter((x) => typeof x === 'string')
    } catch {
      // 非 JSON，按逗号分隔
    }
    return raw.split(',').map((s) => s.trim()).filter(Boolean)
  },
  set(val: string[]) {
    form.imageUrls = val.length > 0 ? JSON.stringify(val) : ''
  }
})

function resetForm() {
  Object.assign(form, {
    goodsCode: undefined,
    goodsName: '',
    goodsShortName: '',
    goodsType: GoodsType.EQUITY,
    categoryCode: '',
    brandName: '',
    coverImage: '',
    imageUrls: '',
    videoUrl: '',
    goodsDescription: '',
    summary: '',
    originalPrice: undefined,
    salePrice: undefined,
    costPrice: undefined,
    priceUnit: '',
    stock: undefined,
    isHot: 0,
    isNew: 0,
    isRecommend: 0,
    sortOrder: 0,
    goodsStatus: GoodsStatus.DRAFT,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

/** 将详情/行数据回填到表单（缺省值兜底，避免 undefined 渲染问题）。 */
function fillForm(detail: GoodsInfo) {
  Object.assign(form, {
    goodsCode: detail.goodsCode,
    goodsName: detail.goodsName ?? '',
    goodsShortName: detail.goodsShortName ?? '',
    goodsType: detail.goodsType ?? GoodsType.EQUITY,
    categoryCode: detail.categoryCode ?? '',
    brandName: detail.brandName ?? '',
    coverImage: detail.coverImage ?? '',
    imageUrls: detail.imageUrls ?? '',
    videoUrl: detail.videoUrl ?? '',
    goodsDescription: detail.goodsDescription ?? '',
    summary: detail.summary ?? '',
    originalPrice: detail.originalPrice,
    salePrice: detail.salePrice,
    costPrice: detail.costPrice,
    priceUnit: detail.priceUnit ?? '',
    stock: detail.stock,
    isHot: detail.isHot ?? 0,
    isNew: detail.isNew ?? 0,
    isRecommend: detail.isRecommend ?? 0,
    sortOrder: detail.sortOrder ?? 0,
    goodsStatus: detail.goodsStatus ?? GoodsStatus.DRAFT,
    remark: detail.remark ?? ''
  })
}

async function openEdit(row: GoodsInfo) {
  if (!row.goodsCode) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getGoods(row.goodsCode)
    fillForm(detail)
  } catch {
    // 拉取详情失败时回退到行数据
    fillForm(row)
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
      await createGoods(form)
      ElMessage.success('新增成功')
    } else if (form.goodsCode) {
      await updateGoods(form.goodsCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  query.goodsCode = ''
  query.goodsName = ''
  query.goodsType = undefined
  query.categoryCode = ''
  query.goodsStatus = undefined
  query.auditStatus = undefined
  handleSearch()
}

async function handleDeleteRow(row: GoodsInfo) {
  if (!row.goodsCode) return
  await ElMessageBox.confirm(`确定删除「${row.goodsName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteGoods(row.goodsCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 上下架 ----------
// shelf 接口入参 0=下架 / 1=上架 / 4=售罄（后端映射到 DDL 5 态落库）。
// 判断是否上架用 DDL 的 2（ON_SHELF）。
async function handleShelf(row: GoodsInfo) {
  if (!row.goodsCode) return
  const isOnShelf = row.goodsStatus === GoodsStatus.ON_SHELF
  const action = isOnShelf ? '下架' : '上架'
  // 上架传 1，下架传 0（shelf 接口契约）
  const targetStatus = isOnShelf ? 0 : 1
  await ElMessageBox.confirm(`确定${action}「${row.goodsName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await shelfGoods({
    goodsCode: row.goodsCode,
    goodsStatus: targetStatus
  })
  ElMessage.success(`${action}成功`)
  loadPage()
}

/** 置售罄（shelf 传 4，仅已上架商品可操作） */
async function handleSoldOut(row: GoodsInfo) {
  if (!row.goodsCode) return
  await ElMessageBox.confirm(`确定将「${row.goodsName}」标记为售罄吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await shelfGoods({ goodsCode: row.goodsCode, goodsStatus: 4 })
  ElMessage.success('已标记售罄')
  loadPage()
}

// ---------- 辅助渲染 ----------
function goodsTypeLabel(t?: number): string {
  const found = GOODS_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function goodsStatusLabel(s?: number): string {
  const found = GOODS_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

function auditStatusLabel(s?: number): string {
  const found = GOODS_AUDIT_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/**
 * 根据商品状态返回 el-tag type（DDL 5 态）。
 * 已上架=success / 待上架=warning / 已售罄=danger / 草稿/已下架=info。
 */
function goodsStatusTagType(status?: number): 'success' | 'info' | 'warning' | 'danger' {
  switch (status) {
    case GoodsStatus.ON_SHELF:
      return 'success'
    case GoodsStatus.PENDING:
      return 'warning'
    case GoodsStatus.SOLD_OUT:
      return 'danger'
    case GoodsStatus.DRAFT:
    case GoodsStatus.OFF_SHELF:
    default:
      return 'info'
  }
}

/** 根据审核状态返回 el-tag type：通过 success / 待审 warning / 驳回 danger。 */
function auditStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case GoodsAuditStatus.PASS:
      return 'success'
    case GoodsAuditStatus.PENDING:
      return 'warning'
    case GoodsAuditStatus.REJECT:
      return 'danger'
    default:
      return 'info'
  }
}

/**
 * 售价显示（含单位）。
 *
 * 后端 priceUnit 是自由字符串，可能存英文（如 "yuan/month"），运营看起来不直观。
 * 这里把常见英文单位映射为中文，未命中时原样回显。
 */
const PRICE_UNIT_CN_MAP: Record<string, string> = {
  yuan: '元',
  rmb: '元',
  'yuan/month': '元/月',
  'rmb/month': '元/月',
  'yuan/year': '元/年',
  'rmb/year': '元/年',
  'yuan/piece': '元/件',
  'yuan/time': '元/次',
  'yuan/day': '元/天'
}

function normalizePriceUnit(unit?: string): string {
  if (!unit) return ''
  const key = unit.trim().toLowerCase()
  return PRICE_UNIT_CN_MAP[key] ?? unit
}

function priceLabel(row: GoodsInfo): string {
  if (row.salePrice == null) return '--'
  const unit = normalizePriceUnit(row.priceUnit)
  return unit ? `${row.salePrice} ${unit}` : String(row.salePrice)
}

/**
 * 库存显示：后端用 -1 约定"不限/无限库存"，直接给运营看 -1 会困惑，转成"不限"。
 */
function stockLabel(stock?: number): string {
  if (stock == null) return '--'
  return stock < 0 ? '不限' : String(stock)
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="商品编码">
          <el-input
            v-model="query.goodsCode"
            placeholder="商品编码"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="商品名称">
          <el-input
            v-model="query.goodsName"
            placeholder="商品名称关键字"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="商品类型">
          <el-select v-model="query.goodsType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in GOODS_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品状态">
          <el-select v-model="query.goodsStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in GOODS_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in GOODS_AUDIT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>商品列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增商品</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="goodsCode">
        <el-table-column prop="goodsCode" label="商品编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="goodsName" label="商品名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="goodsShortName" label="简称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="goodsType" label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ goodsTypeLabel(row.goodsType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="商品分类" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ categoryName(row.categoryCode) }}</template>
        </el-table-column>
        <el-table-column prop="brandName" label="品牌" min-width="120" show-overflow-tooltip />
        <el-table-column label="售价" width="120" align="center">
          <template #default="{ row }">
            {{ priceLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" align="center">
          <template #default="{ row }">{{ stockLabel(row.stock) }}</template>
        </el-table-column>
        <el-table-column prop="goodsStatus" label="商品状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="goodsStatusTagType(row.goodsStatus)">
              {{ goodsStatusLabel(row.goodsStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditStatus" label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="auditStatusTagType(row.auditStatus)">
              {{ auditStatusLabel(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.goodsStatus === GoodsStatus.ON_SHELF"
              link
              type="warning"
              size="small"
              @click="handleShelf(row)"
            >
              下架
            </el-button>
            <el-button
              v-if="row.goodsStatus === GoodsStatus.ON_SHELF"
              link
              type="danger"
              size="small"
              @click="handleSoldOut(row)"
            >
              置售罄
            </el-button>
            <el-button
              v-else
              link
              type="success"
              size="small"
              @click="handleShelf(row)"
            >
              上架
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
      :title="dialogType === 'create' ? '新增商品' : '编辑商品'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="商品名称" prop="goodsName">
              <el-input v-model="form.goodsName" placeholder="商品名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品简称">
              <el-input v-model="form.goodsShortName" placeholder="商品简称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品类型" prop="goodsType">
              <el-select v-model="form.goodsType" placeholder="商品类型" style="width: 100%">
                <el-option v-for="o in GOODS_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品分类">
              <el-select v-model="form.categoryCode" placeholder="选择分类" clearable filterable style="width: 100%">
                <el-option
                  v-for="o in categoryOptions"
                  :key="o.dictCode"
                  :label="o.dictName"
                  :value="o.dictCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="form.brandName" placeholder="品牌名称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价">
              <el-input-number v-model="form.salePrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成本价">
              <el-input-number v-model="form.costPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="价格单位">
              <el-input v-model="form.priceUnit" placeholder="元/件 等" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存">
              <el-input-number v-model="form.stock" :min="0" :max="9999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="goods" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图集">
              <FileUploader v-model="imageUrlsModel" type="image" multiple module="goods" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="视频">
              <FileUploader v-model="form.videoUrl" type="video" module="goods" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否热门">
              <el-switch v-model="form.isHot" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否新品">
              <el-switch v-model="form.isNew" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否推荐">
              <el-switch v-model="form.isRecommend" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品状态">
              <el-select v-model="form.goodsStatus" placeholder="商品状态" style="width: 100%">
                <el-option v-for="o in GOODS_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="摘要">
              <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="商品摘要" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品描述">
              <el-input v-model="form.goodsDescription" type="textarea" :rows="3" placeholder="商品描述" />
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
