package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 机构设施类型创建入参。
 */
@Data
public class ParkFacilityTypeCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "设施类型编码不能为空")
    @Size(max = 50)
    private String facilityTypeCode;

    @NotBlank(message = "设施类型名称不能为空")
    @Size(max = 200)
    private String facilityTypeName;

    private Integer facilityTypeCategory;
    private String buildingName;
    private String floor;
    private BigDecimal area;
    private Integer capacity;
    private String openTime;
    private String facilityTypeDescription;
    private String coverImage;
    private String images;
    private Integer sortOrder;
    private Integer status;
}
