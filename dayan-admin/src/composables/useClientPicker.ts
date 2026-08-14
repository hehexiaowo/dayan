import { ref } from 'vue'
import { pageClients } from '@/api/client'
import type { ClientInfo, ClientInfoQuery } from '@/types/client'

/**
 * 客户远程搜索选择器（pageClients 数据源，按 fullName 模糊查）。
 *
 * 用于管家子表（绑定客户 / 服务记录）中 clientCode 的下拉选择——客户量可能较大，
 * 故用 el-select filterable + remote 而非一次性全量加载。
 *
 * 用法：
 *   const { clientOptions, clientLoading, searchClients, ensureClient } = useClientPicker()
 *   <el-select v-model="form.clientCode" filterable remote :remote-method="searchClients"
 *              :loading="clientLoading">
 *     <el-option v-for="c in clientOptions" :key="c.clientCode"
 *                :label="`${c.fullName}（${c.clientCode}）`" :value="c.clientCode!" />
 *   </el-select>
 */
export function useClientPicker() {
  const clientOptions = ref<ClientInfo[]>([])
  const clientLoading = ref(false)

  /** 远程搜索：keyword 为空时加载首页候选。 */
  async function searchClients(keyword: string) {
    clientLoading.value = true
    try {
      const res = await pageClients({
        fullName: keyword || '',
        current: 1,
        size: 30
      } as ClientInfoQuery)
      clientOptions.value = res.records ?? []
    } catch {
      clientOptions.value = []
    } finally {
      clientLoading.value = false
    }
  }

  /**
   * 编辑回填时确保已选客户出现在候选列表（否则 select 只显示 code 不显示名）。
   * 仅当候选里没有该 clientCode 时追加一条占位项。
   */
  function ensureClient(clientCode?: string, fullName?: string) {
    if (!clientCode) return
    if (clientOptions.value.some((c) => c.clientCode === clientCode)) return
    clientOptions.value.unshift({
      clientCode,
      fullName: fullName || clientCode
    } as ClientInfo)
  }

  // 初始加载首页候选（打开弹窗即可见）
  searchClients('')

  return { clientOptions, clientLoading, searchClients, ensureClient }
}
