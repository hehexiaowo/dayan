package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构服务项创建入参。
 */
@Data
public class ParkServiceItemCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "服务编码不能为空")
    @Size(max = 50)
    private String serviceCode;

    @NotBlank(message = "服务名称不能为空")
    @Size(max = 200)
    private String serviceName;

    private Integer serviceCategory;
    private String serviceDescription;
    private String serviceFrequency;
    private String serviceDuration;
    private String coverImage;
    private Integer sortOrder;
    private Integer status;
}
