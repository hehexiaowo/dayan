/**
 * AI 问答人物配置 API（对齐后端 ToolAiQaConfigAdminController，RESTful /admin-api/tool/qa/config）。
 */
import { request } from '@/utils/request'
import { pageKnowledgeRepos } from '@/api/knowledge'
import type { PageResult } from '@/types/common'
import type { ToolAiQaConfig, ToolAiQaConfigQuery } from '@/types/toolQa'

/** 人物分页列表 */
export function pageQaConfigs(query: ToolAiQaConfigQuery): Promise<PageResult<ToolAiQaConfig>> {
  return request<PageResult<ToolAiQaConfig>>({
    url: '/admin-api/tool/qa/config/page',
    method: 'get',
    params: query
  })
}

/** 新增人物 */
export function createQaConfig(data: Partial<ToolAiQaConfig>): Promise<string> {
  return request<string>({ url: '/admin-api/tool/qa/config', method: 'post', data })
}

/** 修改人物 */
export function updateQaConfig(id: number, data: Partial<ToolAiQaConfig>): Promise<void> {
  return request<void>({ url: `/admin-api/tool/qa/config/${id}`, method: 'put', data })
}

/** 删除人物 */
export function deleteQaConfig(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/tool/qa/config/${id}`, method: 'delete' })
}

const REPO_PAGE_SIZE = 100

/**
 * 知识库绑定选项（id + repoName），供人物表单多选下拉使用。
 *
 * 复用已有分页接口 pageKnowledgeRepos 全量拉取（按 total 分页，按 id 去重），
 * 避免依赖不存在的 /admin-api/knowledge/repos/list 接口。
 */
export async function fetchRepoOptions(): Promise<{ id: number; repoName: string }[]> {
  const map = new Map<number, { id: number; repoName: string }>()
  let page = 1
  let total = Infinity
  while (map.size < total) {
    const res = await pageKnowledgeRepos({ current: page, size: REPO_PAGE_SIZE })
    total = res.total ?? 0
    for (const r of res.records) {
      if (r.id == null) continue
      const id = Number(r.id)
      if (Number.isNaN(id)) continue
      map.set(id, { id, repoName: r.repoName })
    }
    if (res.records.length === 0 || map.size >= total) break
    page++
  }
  return Array.from(map.values())
}
