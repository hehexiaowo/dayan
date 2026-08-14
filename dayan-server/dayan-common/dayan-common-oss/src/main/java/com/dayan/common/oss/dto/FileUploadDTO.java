package com.dayan.common.oss.dto;

import lombok.Data;

/**
 * 文件上传返回结构。
 * url 用于上传后即时预览；key 是 DB 持久化值（端无关/环境无关）。
 */
@Data
public class FileUploadDTO {
    /** 访问 URL，如 /admin-api/v1/files/preview/{key} */
    private String url;
    /** 对象 key，如 goods/day001/2026/08/08/abc.jpg（存入 DB） */
    private String key;
    /** 原始文件名 */
    private String originalName;
    /** 文件大小（字节） */
    private long size;
    /** 完整 URL（富文本内嵌资源用；agent/client rich-text 渲染不做 URL 改写） */
    private String absoluteUrl;
}
