package com.dayan.system.dto;

import lombok.Data;

/**
 * 系统素材查询入参。
 */
@Data
public class SystemAssetQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    /** 名称/URL 模糊搜索 */
    private String keyword;
    /** 素材类型（1=图片 2=视频 3=文件 4=VR） */
    private Integer assetType;
    /** 类型1：业务维度（park/platform/goods/content/course/scene） */
    private String refType1;
    /** 类型2：细分分类（字典 asset_ref_type2） */
    private String refType2;
    /** 关联编码（业务实体编码，如机构编码） */
    private String refCode;
    /** 存储方式（1=本地OSS 2=外链） */
    private Integer storageType;
    /** 是否封面（图片专属） */
    private Integer isCover;
    private Integer status;
}
