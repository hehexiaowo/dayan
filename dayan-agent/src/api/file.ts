import { getToken } from '@/utils/request';

export interface FileUploadResult {
  key: string;
  url: string;
  originalName?: string;
  size?: number;
}

/**
 * 上传文件（uni.uploadFile 直传 POST /agent-api/v1/files/upload）。
 * 返回 OSS key（存 DB 用）；展示用 formatFileUrl(key)。
 */
export function uploadFile(filePath: string, module = 'avatar'): Promise<FileUploadResult> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: '/agent-api/v1/files/upload',
      filePath,
      name: 'file',
      formData: { module },
      header: { 'Agent-Token': getToken() },
      success: (res) => {
        try {
          const body = JSON.parse(res.data);
          if (body.code === 0) {
            resolve(body.data as FileUploadResult);
          } else {
            uni.showToast({ title: body.message || '上传失败', icon: 'none' });
            reject(new Error(body.message));
          }
        } catch (e) {
          reject(e);
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' });
        reject(err);
      },
    });
  });
}
