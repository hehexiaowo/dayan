package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构服务项更新入参。
 */
@Data
public class ParkServiceItemUpdateDTO {

    private String serviceName;
    private Integer serviceCategory;
    private String serviceDescription;
    private String serviceFrequency;
    private String serviceDuration;
    private String coverImage;
    private Integer sortOrder;
    private Integer status;
}
