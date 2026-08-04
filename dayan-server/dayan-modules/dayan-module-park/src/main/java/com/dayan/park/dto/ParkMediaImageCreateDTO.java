package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构图片创建入参。
 */
@Data
public class ParkMediaImageCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "图片URL不能为空")
    @Size(max = 500)
    private String imageUrl;

    @Size(max = 200)
    private String imageName;
    /** 图片类型（11 类分类） */
    private Integer imageType;
    private String imageDescription;
    private Integer width;
    private Integer height;
    private Integer fileSize;
    private Integer sortOrder;
    /** 是否封面（1=是） */
    private Integer isCover;
    private Integer status;
}
