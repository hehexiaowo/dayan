import request from '@/utils/request'
import type { KnowledgeDocOption, KnowledgeRepoOption } from '@/types/aiContent'

/** 当前渠道可见知识仓库（GET /agent-api/system/knowledge/repos） */
export function getMyKnowledgeRepos(): Promise<KnowledgeRepoOption[]> {
  return request<KnowledgeRepoOption[]>({ url: '/system/knowledge/repos', method: 'GET' })
}

/** 可见仓库文档合并列表（GET /agent-api/system/knowledge/docs?keyword=） */
export function getKnowledgeDocs(keyword?: string): Promise<KnowledgeDocOption[]> {
  return request<KnowledgeDocOption[]>({
    url: '/system/knowledge/docs',
    method: 'GET',
    data: keyword ? { keyword } : {}
  })
}

/** 知识检索引用（text 片段 + 相关度） */
export interface KnowledgeCitation {
  text: string
  score?: number
}

/** 知识检索（POST /agent-api/system/knowledge/retrieve，AI 创作前端供材；可见性同 /repos） */
export function retrieveKnowledge(params: {
  repoId: number
  query: string
  docFileIds?: string[]
  topK?: number
}): Promise<KnowledgeCitation[]> {
  return request<KnowledgeCitation[]>({ url: '/system/knowledge/retrieve', method: 'POST', data: params })
}
