import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listDictByType } from '@/api/dict'
import type { SystemDict } from '@/types/dict'

/**
 * 字典下拉选项加载（统一字典 system_dict，按 dictType 取启用项）。
 *
 * 用于"分类即字典"场景的分类选择器：
 * - content_category（内容分类）/ course_category（课程分类）
 * - goods_category（商品分类）
 * - vr_provider（VR 提供商）/ asset_ref_type2（素材二级分类）等
 *
 * 返回的 options 元素即字典项，展示用 dictName、绑值用 dictCode；
 * 后端 listDictByType 仅返回启用项并带 Redis 缓存。
 */
export function useDictOptions(dictType: string) {
  const options = ref<SystemDict[]>([])
  const loading = ref(false)

  async function load() {
    loading.value = true
    try {
      options.value = await listDictByType(dictType)
    } catch {
      options.value = []
      ElMessage.error('字典加载失败')
    } finally {
      loading.value = false
    }
  }

  load()

  return { options, loading, reload: load }
}
