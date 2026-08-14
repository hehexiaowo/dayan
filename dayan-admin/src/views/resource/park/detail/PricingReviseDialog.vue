<script setup lang="ts">
/**
 * 定价调价弹窗（规格 §F.4）：以现记录为基线新建价格版本。
 * 生效日期<=今天=立即生效；未来=预约生效（每小时调度器到点翻转）。
 * 历史版本查看：子面板价格展开行（isCurrent desc 排序即版本历史）。
 */
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { revisePricing } from '@/api/park-pricing'

defineOptions({ name: 'PricingReviseDialog' })

const props = defineProps<{
  modelValue: boolean
  /** 基线价格记录（当前价） */
  pricing?: { id?: number; salePrice?: number; originalPrice?: number; refName?: string; planName?: string }
}>()

const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void; (e: 'revived'): void }>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

const submitLoading = ref(false)
const form = ref({ salePrice: undefined as number | undefined, effectiveDate: '', priceChangeReason: '' })

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      form.value = {
        salePrice: props.pricing?.salePrice,
        effectiveDate: new Date().toISOString().slice(0, 10),
        priceChangeReason: ''
      }
    }
  }
)

const isImmediate = computed(() => !!form.value.effectiveDate && form.value.effectiveDate <= new Date().toISOString().slice(0, 10))

async function handleSubmit() {
  if (!props.pricing?.id || form.value.salePrice === undefined) {
    ElMessage.warning('请填写新售价')
    return
  }
  submitLoading.value = true
  try {
    await revisePricing(props.pricing.id, {
      salePrice: form.value.salePrice,
      effectiveDate: form.value.effectiveDate,
      priceChangeReason: form.value.priceChangeReason
    })
    ElMessage.success(isImmediate.value ? '调价已生效（新版本为当前价）' : '预约调价已登记，到点自动生效')
    visible.value = false
    emit('revived')
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="调价（新建价格版本）" width="480px" :close-on-click-modal="false">
    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      调价不会修改历史记录：{{ isImmediate ? '保存后立即生效为当前价' : '到达生效日期后自动切换为当前价' }}。历史版本可在价格展开行查看。
    </el-alert>
    <el-form label-width="90px">
      <el-form-item label="调价对象">
        <span>{{ pricing?.refName || pricing?.planName || '--' }}（现售价 {{ pricing?.salePrice ?? '--' }}）</span>
      </el-form-item>
      <el-form-item label="新售价" required>
        <el-input-number v-model="form.salePrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
      </el-form-item>
      <el-form-item label="生效日期" required>
        <el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="调价原因">
        <el-input v-model="form.priceChangeReason" type="textarea" :rows="2" maxlength="200" placeholder="选填，记录在版本历史" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认调价</el-button>
    </template>
  </el-dialog>
</template>
