package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构周边信息 VO。
 */
@Data
public class ParkPeripheryVO {

    private Long id;
    private String parkCode;
    private Integer peripheryType;
    private String placeName;
    private String placeAddress;
    private String distance;
    private String detailDescription;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
