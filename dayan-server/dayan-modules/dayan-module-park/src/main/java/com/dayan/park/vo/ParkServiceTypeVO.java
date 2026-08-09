package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构服务类型 VO。
 */
@Data
public class ParkServiceTypeVO {

    private Long id;
    private String parkCode;
    private String serviceTypeCode;
    private String serviceTypeName;
    private Integer serviceTypeCategory;
    private String serviceTypeDescription;
    private String serviceTypeFrequency;
    private String serviceTypeDuration;
    private String coverImage;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
