package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 机构设施创建入参。
 */
@Data
public class ParkFacilityCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "设施编码不能为空")
    @Size(max = 50)
    private String facilityCode;

    @NotBlank(message = "设施名称不能为空")
    @Size(max = 200)
    private String facilityName;

    private Integer facilityCategory;
    private String buildingName;
    private String floor;
    private BigDecimal area;
    private Integer capacity;
    private String openTime;
    private String facilityDescription;
    private String coverImage;
    private String images;
    private Integer isFree;
    private String feeDescription;
    private Integer sortOrder;
    private Integer status;
}
