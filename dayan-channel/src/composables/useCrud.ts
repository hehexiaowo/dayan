import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { PageQuery, PageResult } from '@/types/common'

/**
 * 通用 CRUD 组合式函数。
 *
 * 封装分页查询 + 增删改的常用样板：loading / tableData / total / 查询条件 / 操作处理。
 * 各业务页面按需传入 api 对象（仅提供实际用到的操作），减少重复代码。
 *
 * @example
 * const { loading, tableData, total, query, handleSearch, handleCreate } = useCrud(api, { initialQuery: { username: '' } })
 *
 * @param api 业务 API 对象
 * @param options initialQuery 初始查询条件（含 current/size）
 */
export function useCrud<T extends Record<string, unknown>, Q extends PageQuery>(
  api: {
    page: (query: Q) => Promise<PageResult<T>>
    create?: (data: Partial<T>) => Promise<unknown>
    update?: (code: string, data: Partial<T>) => Promise<unknown>
    remove?: (code: string) => Promise<unknown>
  },
  options: { initialQuery: Omit<Q, 'current' | 'size'> }
) {
  const loading = ref(false)
  const tableData = ref<T[]>([]) as { value: T[] }
  const total = ref(0)

  const query = reactive({
    current: 1,
    size: 20,
    ...options.initialQuery
  }) as Q

  /** 拉取分页数据 */
  async function loadPage() {
    loading.value = true
    try {
      const res = await api.page(query)
      tableData.value = res.records
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  /** 搜索（重置到第 1 页） */
  function handleSearch() {
    query.current = 1
    loadPage()
  }

  /** 翻页 */
  function handlePageChange(page: number) {
    query.current = page
    loadPage()
  }

  /** 每页条数变化 */
  function handleSizeChange(size: number) {
    query.size = size
    query.current = 1
    loadPage()
  }

  /** 新增（调用 api.create，成功后刷新列表） */
  async function handleCreate(data: Partial<T>, successMsg = '新增成功') {
    if (!api.create) throw new Error('未提供 create 接口')
    await api.create(data)
    ElMessage.success(successMsg)
    loadPage()
  }

  /** 修改（调用 api.update，成功后刷新列表） */
  async function handleUpdate(code: string, data: Partial<T>, successMsg = '修改成功') {
    if (!api.update) throw new Error('未提供 update 接口')
    await api.update(code, data)
    ElMessage.success(successMsg)
    loadPage()
  }

  /** 删除（二次确认后调用 api.remove，成功后刷新列表） */
  async function handleDelete(code: string, confirmMsg = '确定删除该记录？', successMsg = '删除成功') {
    if (!api.remove) throw new Error('未提供 remove 接口')
    await ElMessageBox.confirm(confirmMsg, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.remove(code)
    ElMessage.success(successMsg)
    loadPage()
  }

  return {
    loading,
    tableData,
    total,
    query,
    loadPage,
    handleSearch,
    handlePageChange,
    handleSizeChange,
    handleCreate,
    handleUpdate,
    handleDelete
  }
}
