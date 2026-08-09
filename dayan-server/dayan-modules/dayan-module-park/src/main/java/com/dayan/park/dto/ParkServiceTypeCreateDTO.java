package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构服务类型创建入参。
 */
@Data
public class ParkServiceTypeCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "服务类型编码不能为空")
    @Size(max = 50)
    private String serviceTypeCode;

    @NotBlank(message = "服务类型名称不能为空")
    @Size(max = 200)
    private String serviceTypeName;

    private Integer serviceTypeCategory;
    private String serviceTypeDescription;
    private String serviceTypeFrequency;
    private String serviceTypeDuration;
    private String coverImage;
    private Integer sortOrder;
    private Integer status;
}
