package com.dayan.system.dto;

import lombok.Data;

/**
 * 系统素材查询入参。
 */
@Data
public class SystemAssetQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    /** 归属机构编码（空=全部，含平台素材） */
    private String parkCode;
    /** 名称/URL 模糊搜索 */
    private String keyword;
    /** 素材类型（1=图片 2=视频 3=文件 4=VR） */
    private Integer assetType;
    /** 存储方式（1=本地OSS 2=外链） */
    private Integer storageType;
    /** 业务分类 */
    private Integer assetCategory;
    /** 是否封面（图片专属） */
    private Integer isCover;
    /** 来源 */
    private String sourceType;
    private Integer status;
}
