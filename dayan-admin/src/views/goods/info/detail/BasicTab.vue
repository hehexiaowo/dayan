<script setup lang="ts">
/**
 * 商品详情页 - 基本信息 tab。
 *
 * 只读展示 GoodsInfo 关键字段（el-descriptions），提供"编辑"按钮打开 el-dialog + el-form
 * 修改主表（提交 updateGoods）。
 *
 * 字段约束：
 * - goodsType 创建后不可改（UpdateDTO 无此字段），编辑表单 disabled。
 * - salesCount / viewCount / collectCount / createdAt 只读展示（统计字段）。
 * - goodsStatus 由 shelf 接口控制（列表页上下架按钮），编辑表单不含该字段；
 *   auditStatus / isHot / isNew / isRecommend 用 el-select / el-switch。
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getGoods, updateGoods } from '@/api/goods'
import {
  GoodsType,
  GOODS_TYPE_OPTIONS,
  GOODS_AUDIT_STATUS_OPTIONS,
  goodsTypeLabel,
  goodsStatusLabel,
  goodsStatusTagType
} from '@/types/goods'
import type { GoodsInfo } from '@/types/goods'
import { formatDate, formatDateTime } from '@/utils/format'
import FileUploader from '@/components/FileUploader/index.vue'
import { formatFileUrl } from '@/utils/file'

const props = defineProps<{
  /** 商品编码（从详情页路由 prop 带入） */
  goodsCode: string
}>()

const loading = ref(false)
const goodsInfo = ref<GoodsInfo | null>(null)

async function loadDetail() {
  if (!props.goodsCode) return
  loading.value = true
  try {
    goodsInfo.value = await getGoods(props.goodsCode)
  } catch {
    goodsInfo.value = null
  } finally {
    loading.value = false
  }
}

loadDetail()

// ---------- 辅助渲染 ----------
function auditStatusLabel(s?: number): string {
  const found = GOODS_AUDIT_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

// ---------- 编辑弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<GoodsInfo>({
  goodsCode: undefined,
  goodsName: '',
  goodsShortName: '',
  goodsType: undefined,
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
  saleStartTime: '',
  saleEndTime: '',
  isHot: 0,
  isNew: 0,
  isRecommend: 0,
  sortOrder: 0,
  auditStatus: undefined,
  remark: ''
})

const rules: FormRules<GoodsInfo> = {
  goodsName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }]
}

function openEdit() {
  if (!goodsInfo.value) return
  const g = goodsInfo.value
  Object.assign(form, {
    goodsCode: g.goodsCode,
    goodsName: g.goodsName ?? '',
    goodsShortName: g.goodsShortName ?? '',
    // goodsType 创建后不可改，编辑表单只读展示（disabled），但仍回填用于提交时占位
    goodsType: g.goodsType ?? GoodsType.EQUITY,
    categoryCode: g.categoryCode ?? '',
    brandName: g.brandName ?? '',
    coverImage: g.coverImage ?? '',
    imageUrls: g.imageUrls ?? '',
    videoUrl: g.videoUrl ?? '',
    goodsDescription: g.goodsDescription ?? '',
    summary: g.summary ?? '',
    originalPrice: g.originalPrice,
    salePrice: g.salePrice,
    costPrice: g.costPrice,
    priceUnit: g.priceUnit ?? '',
    stock: g.stock,
    saleStartTime: g.saleStartTime ?? '',
    saleEndTime: g.saleEndTime ?? '',
    isHot: g.isHot ?? 0,
    isNew: g.isNew ?? 0,
    isRecommend: g.isRecommend ?? 0,
    sortOrder: g.sortOrder ?? 0,
    auditStatus: g.auditStatus,
    remark: g.remark ?? ''
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
  if (!form.goodsCode) return
  submitLoading.value = true
  try {
    await updateGoods(form.goodsCode, form)
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

/** 暴露刷新方法，供详情页外部刷新 */
defineExpose({ loadDetail })
</script>

<template>
  <div v-loading="loading">
    <template v-if="goodsInfo">
      <div class="basic-toolbar">
        <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="商品编码">{{ goodsInfo.goodsCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ goodsInfo.goodsName }}</el-descriptions-item>
        <el-descriptions-item label="商品简称">{{ goodsInfo.goodsShortName ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="商品类型">
          <el-tag size="small" type="info">{{ goodsTypeLabel(goodsInfo.goodsType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="商品状态">
          <el-tag size="small" :type="goodsStatusTagType(goodsInfo.goodsStatus)">
            {{ goodsStatusLabel(goodsInfo.goodsStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">{{ auditStatusLabel(goodsInfo.auditStatus) }}</el-descriptions-item>
        <el-descriptions-item label="分类编码">{{ goodsInfo.categoryCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ goodsInfo.brandName ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="价格单位">{{ goodsInfo.priceUnit ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="原价">{{ goodsInfo.originalPrice ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="售价">{{ goodsInfo.salePrice ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="成本价">{{ goodsInfo.costPrice ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="库存">{{ goodsInfo.stock ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="销量（只读）">{{ goodsInfo.salesCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="浏览量（只读）">{{ goodsInfo.viewCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="收藏量（只读）">{{ goodsInfo.collectCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="是否热门">{{ goodsInfo.isHot === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="是否新品">{{ goodsInfo.isNew === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="是否推荐">{{ goodsInfo.isRecommend === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="排序号">{{ goodsInfo.sortOrder ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="开售时间">{{ goodsInfo.saleStartTime ? formatDate(goodsInfo.saleStartTime) : '--' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ goodsInfo.saleEndTime ? formatDate(goodsInfo.saleEndTime) : '--' }}</el-descriptions-item>
        <el-descriptions-item label="封面图" :span="3">
          <el-image
            v-if="goodsInfo.coverImage"
            :src="formatFileUrl(goodsInfo.coverImage)"
            :preview-src-list="[formatFileUrl(goodsInfo.coverImage)]"
            fit="cover"
            style="width: 80px; height: 80px"
          />
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="视频" :span="3">{{ goodsInfo.videoUrl ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="3">{{ goodsInfo.summary ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="商品描述" :span="3">{{ goodsInfo.goodsDescription ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(goodsInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ goodsInfo.remark ?? '--' }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-else-if="!loading" description="未加载到商品信息" />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑商品基本信息"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品编码">
              <el-input v-model="form.goodsCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品名称" prop="goodsName">
              <el-input v-model="form.goodsName" placeholder="商品名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品类型">
              <!-- goodsType 创建后不可改（UpdateDTO 无此字段），编辑时 disabled -->
              <el-select v-model="form.goodsType" placeholder="商品类型" style="width: 100%" disabled>
                <el-option v-for="o in GOODS_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品简称">
              <el-input v-model="form.goodsShortName" placeholder="商品简称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类编码">
              <el-input v-model="form.categoryCode" placeholder="分类编码" maxlength="50" />
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
          <el-col :span="12">
            <el-form-item label="开售时间">
              <el-date-picker
                v-model="form.saleStartTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择开售时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-date-picker
                v-model="form.saleEndTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择结束时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="goods" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图集">
              <el-input v-model="form.imageUrls" type="textarea" :rows="2" placeholder="图集 URL（逗号分隔或 JSON）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="视频">
              <el-input v-model="form.videoUrl" placeholder="视频 URL" maxlength="255" />
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
            <el-form-item label="审核状态">
              <el-select v-model="form.auditStatus" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in GOODS_AUDIT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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

<style scoped>
.basic-toolbar {
  margin-bottom: 16px;
}
</style>
