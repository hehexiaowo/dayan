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
const REPO_MAX_PAGES = 5

/**
 * 知识库绑定选项（id + repoName），供人物表单多选下拉使用。
 *
 * 复用已有分页接口 pageKnowledgeRepos 拉取（最多 5 页 × 100），
 * 避免依赖不存在的 /admin-api/knowledge/repos/list 接口。
 */
export async function fetchRepoOptions(): Promise<{ id: number; repoName: string }[]> {
  const list: { id: number; repoName: string }[] = []
  for (let page = 1; page <= REPO_MAX_PAGES; page++) {
    const res = await pageKnowledgeRepos({ current: page, size: REPO_PAGE_SIZE })
    list.push(...res.records.map((r) => ({ id: Number(r.id), repoName: r.repoName })))
    if (list.length >= res.total || res.records.length === 0) break
  }
  return list
}