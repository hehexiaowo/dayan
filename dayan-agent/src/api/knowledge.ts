import request from '@/utils/request'
import type { KnowledgeDocOption, KnowledgeRepoOption } from '@/types/aiContent'

/** 当前渠道可见知识仓库（GET /agent-api/knowledge/repos） */
export function getMyKnowledgeRepos(): Promise<KnowledgeRepoOption[]> {
  return request<KnowledgeRepoOption[]>({ url: '/knowledge/repos', method: 'GET' })
}

/** 可见仓库文档合并列表（GET /agent-api/knowledge/docs?keyword=） */
export function getKnowledgeDocs(keyword?: string): Promise<KnowledgeDocOption[]> {
  return request<KnowledgeDocOption[]>({
    url: '/knowledge/docs',
    method: 'GET',
    data: keyword ? { keyword } : {}
  })
}

/** 知识检索引用（text 片段 + 相关度） */
export interface KnowledgeCitation {
  text: string
  score?: number
}

/** 知识检索（POST /agent-api/knowledge/retrieve，AI 创作前端供材；可见性同 /repos） */
export function retrieveKnowledge(params: {
  repoId: number
  query: string
  docFileIds?: string[]
  topK?: number
}): Promise<KnowledgeCitation[]> {
  return request<KnowledgeCitation[]>({ url: '/knowledge/retrieve', method: 'POST', data: params })
}
