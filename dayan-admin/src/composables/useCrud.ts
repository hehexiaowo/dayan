import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { PageQuery, PageResult } from '@/types/common'

/**
 * 通用 CRUD 组合式函数。
 *
 * 封装分页查询 + 增删改的常用样板：loading / tableData / total / 查询条件 / 操作处理。
 * 各业务页面按需传入 api 对象（仅提供实际用到的操作），减少重复代码。
 *
 * 支持两种主键模式：
 * - code 模式（默认）：主表用业务编码（如 supplierCode/parkCode），update/remove 接收 string。
 * - id 模式：子表用自增 id（如 park_room_type.id），传 idKey='id' 后 update/remove 接收该字段值。
 *
 * 支持固定查询参数 fixedParams：详情页 tab 场景下自动携带（如 parkCode），
 * 调用方无需每次手动塞进 query。
 *
 * @example
 * const { loading, tableData, total, query, handleSearch, handleCreate } = useCrud(api, { initialQuery: { username: '' } })
 *
 * @param api 业务 API 对象
 * @param options initialQuery 初始查询条件（含 current/size）；idKey 主键字段名（默认 'code'）；fixedParams 固定查询参数
 */
export function useCrud<T extends Record<string, any>, Q extends PageQuery, K extends string | number = string>(
  api: {
    page: (query: Q) => Promise<PageResult<T>>
    create?: (data: Partial<T>) => Promise<unknown>
    update?: (key: K, data: Partial<T>) => Promise<unknown>
    remove?: (key: K) => Promise<unknown>
  },
  options: {
    initialQuery: Omit<Q, 'current' | 'size'>
    /** 主键字段名：默认 'code'（主表业务编码），子表用自增 id 时传 'id' */
    idKey?: string
    /** 固定查询参数（如详情页 tab 的 parkCode），每次查询自动合并 */
    fixedParams?: Partial<Q>
  }
) {
  const idKey = options.idKey ?? 'code'
  const loading = ref(false)
  const tableData = ref<T[]>([]) as { value: T[] }
  const total = ref(0)

  const query = reactive({
    current: 1,
    size: 20,
    ...options.initialQuery,
    ...(options.fixedParams ?? {})
  }) as Q

  /** 拉取分页数据 */
  async function loadPage() {
    loading.value = true
    try {
      // fixedParams 每次查询时重新合并，防止调用方改 query 时丢掉固定参数
      const merged = { ...query, ...(options.fixedParams ?? {}) } as Q
      const res = await api.page(merged)
      tableData.value = res.records
      total.value = res.total
    } catch {
      // 请求失败：清空旧数据避免误导（错误提示由全局响应拦截器统一处理）
      tableData.value = []
      total.value = 0
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

  /** 修改（调用 api.update，成功后刷新列表）。key 为主键值（code 或 id） */
  async function handleUpdate(key: K, data: Partial<T>, successMsg = '修改成功') {
    if (!api.update) throw new Error('未提供 update 接口')
    await api.update(key, data)
    ElMessage.success(successMsg)
    loadPage()
  }

  /** 删除（二次确认后调用 api.remove，成功后刷新列表）。key 为主键值（code 或 id） */
  async function handleDelete(key: K, confirmMsg = '确定删除该记录？', successMsg = '删除成功') {
    if (!api.remove) throw new Error('未提供 remove 接口')
    await ElMessageBox.confirm(confirmMsg, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.remove(key)
    ElMessage.success(successMsg)
    loadPage()
  }

  return {
    loading,
    tableData,
    total,
    query,
    idKey,
    loadPage,
    handleSearch,
    handlePageChange,
    handleSizeChange,
    handleCreate,
    handleUpdate,
    handleDelete
  }
}
