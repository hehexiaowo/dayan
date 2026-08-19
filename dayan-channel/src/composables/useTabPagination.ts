import { ref, watch } from 'vue'
import type { PageQuery, PageResult } from '@/types/common'

/**
 * Tab 子组件通用分页逻辑。
 * 监听外部标识 prop 变化时自动重置页码并重新加载。
 *
 * @param fetchFn  分页请求函数（接收 PageQuery，返回 PageResult）
 * @param identify 外部标识的 getter（如 () => props.agentCode）
 * @param pageSize 默认每页条数（默认 10）
 */
export function useTabPagination<T>(
  fetchFn: (query: PageQuery) => Promise<PageResult<T>>,
  identify: () => string | number | undefined,
  pageSize = 10,
) {
  const tableData = ref<T[]>([]) as { value: T[] }
  const total = ref(0)
  const current = ref(1)
  const size = ref(pageSize)
  const loading = ref(false)

  async function loadPage() {
    loading.value = true
    try {
      const res = await fetchFn({ current: current.value, size: size.value })
      tableData.value = res.records ?? []
      total.value = res.total ?? 0
    } catch (err) {
      console.warn('[useTabPagination] 加载失败:', err)
      tableData.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  function handlePageChange(page: number) {
    current.value = page
    loadPage()
  }

  function handleSizeChange(sz: number) {
    size.value = sz
    current.value = 1
    loadPage()
  }

  // 外部标识变化时重置并重新加载
  watch(identify, () => {
    current.value = 1
    loadPage()
  })

  return {
    tableData,
    total,
    current,
    size,
    loading,
    loadPage,
    handlePageChange,
    handleSizeChange,
  }
}
