package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构 VR 更新入参。
 */
@Data
public class ParkMediaVrUpdateDTO {

    private String vrUrl;
    private String vrProvider;
    private String vrName;
    private Integer vrType;
    private String thumbnailUrl;
    private String vrDescription;
    private Integer sortOrder;
    private Integer status;
}
