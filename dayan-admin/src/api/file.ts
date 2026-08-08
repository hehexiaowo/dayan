import { request } from '@/utils/request'

/** 文件上传返回结构 */
export interface FileUploadDTO {
  url: string
  key: string
  originalName: string
  size: number
}

/** 上传文件：POST /admin-api/v1/files/upload */
export function uploadFile(file: File, module?: string): Promise<FileUploadDTO> {
  const formData = new FormData()
  formData.append('file', file)
  if (module) formData.append('module', module)
  return request<FileUploadDTO>({
    url: '/admin-api/v1/files/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}
