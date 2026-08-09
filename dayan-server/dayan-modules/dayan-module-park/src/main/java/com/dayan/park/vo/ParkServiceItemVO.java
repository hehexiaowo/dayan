package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构服务项 VO。
 */
@Data
public class ParkServiceItemVO {

    private Long id;
    private String parkCode;
    private String serviceCode;
    private String serviceName;
    private Integer serviceCategory;
    private String serviceDescription;
    private String serviceFrequency;
    private String serviceDuration;
    private String coverImage;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
