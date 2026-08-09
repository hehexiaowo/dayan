package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构素材查询入参。
 */
@Data
public class ParkAssetQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    /** 素材类型（1=图片 2=视频 3=文件 4=VR） */
    private Integer assetType;
    /** 业务分类 */
    private Integer assetCategory;
    /** 是否封面（图片专属） */
    private Integer isCover;
    /** 来源 */
    private String sourceType;
    private Integer status;
}
