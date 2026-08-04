package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构图片 VO。
 */
@Data
public class ParkMediaImageVO {

    private Long id;
    private String parkCode;
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
    private LocalDateTime createdAt;
}
