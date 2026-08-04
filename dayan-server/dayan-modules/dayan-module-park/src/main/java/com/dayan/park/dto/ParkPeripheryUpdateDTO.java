package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构周边信息更新入参。
 */
@Data
public class ParkPeripheryUpdateDTO {

    private Integer peripheryType;
    private String placeName;
    private String placeAddress;
    private String distance;
    private String detailDescription;
    private Integer sortOrder;
    private Integer status;
}
