import { request } from '@/utils/request'
import type { ServiceItemPageResult, ServiceItem, ServiceItemQuery } from '@/types/service-item'

const BASE = '/admin-api/goods/service-item'

export function pageServiceItems(query: ServiceItemQuery) {
  return request<ServiceItemPageResult>({ url: `${BASE}/page`, method: 'get', params: query })
}

export function listServiceItems(params?: Partial<ServiceItemQuery>) {
  return request<ServiceItem[]>({ url: `${BASE}/list`, method: 'get', params })
}

export function getServiceItem(itemCode: string) {
  return request<ServiceItem>({ url: `${BASE}/${itemCode}`, method: 'get' })
}

export function createServiceItem(data: Partial<ServiceItem>) {
  return request<string>({ url: BASE, method: 'post', data })
}

export function updateServiceItem(itemCode: string, data: Partial<ServiceItem>) {
  return request<void>({ url: `${BASE}/${itemCode}`, method: 'put', data })
}

export function deleteServiceItem(itemCode: string) {
  return request<void>({ url: `${BASE}/${itemCode}`, method: 'delete' })
}
