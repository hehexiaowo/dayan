package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构周边信息创建入参。
 */
@Data
public class ParkPeripheryCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    /** 周边类型 */
    private Integer peripheryType;

    @NotBlank(message = "地点名称不能为空")
    @Size(max = 200)
    private String placeName;

    private String placeAddress;
    private String distance;
    private String detailDescription;
    private Integer sortOrder;
    private Integer status;
}
