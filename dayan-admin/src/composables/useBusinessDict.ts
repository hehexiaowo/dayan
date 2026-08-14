import { ref } from 'vue'
import { pageDictBusiness } from '@/api/dictBusiness'
import type { SystemDictBusiness } from '@/types/dict'

/**
 * 业务字典下拉选项加载（按 dictType 取启用项，一次性拉全量）。
 *
 * 用于"分类即字典"场景的分类选择器：
 * - goods_category（商品分类，goods_info.category_code）
 * - course_category（课程分类，course_info.category_code）
 * - learning_category（学习中心分类，learning_content.category）
 *
 * 返回的 options 元素即字典项，展示用 dictName、绑值用 dictCode。
 */
export function useBusinessDictOptions(dictType: string) {
  const options = ref<SystemDictBusiness[]>([])
  const loading = ref(false)

  async function load() {
    loading.value = true
    try {
      const result = await pageDictBusiness({ dictType, current: 1, size: 200 })
      options.value = (result.records ?? []).filter((d) => d.status === 1)
    } catch {
      options.value = []
    } finally {
      loading.value = false
    }
  }

  load()

  return { options, loading, reload: load }
}
