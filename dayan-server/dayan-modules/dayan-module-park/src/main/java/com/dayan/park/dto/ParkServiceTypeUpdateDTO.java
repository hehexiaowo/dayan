package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构服务类型更新入参。
 */
@Data
public class ParkServiceTypeUpdateDTO {

    private String serviceTypeName;
    private Integer serviceTypeCategory;
    private String serviceTypeDescription;
    private String serviceTypeFrequency;
    private String serviceTypeDuration;
    private String coverImage;
    private Integer sortOrder;
    private Integer status;
}
