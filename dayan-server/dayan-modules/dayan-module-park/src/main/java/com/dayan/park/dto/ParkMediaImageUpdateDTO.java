package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构图片更新入参（按字段非空更新）。
 */
@Data
public class ParkMediaImageUpdateDTO {

    private String imageUrl;
    private String imageName;
    private Integer imageType;
    private String imageDescription;
    private Integer width;
    private Integer height;
    private Integer fileSize;
    private Integer sortOrder;
    private Integer isCover;
    private Integer status;
}
