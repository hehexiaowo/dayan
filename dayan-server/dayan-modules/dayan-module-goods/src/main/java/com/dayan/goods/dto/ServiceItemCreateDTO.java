package com.dayan.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceItemCreateDTO {

    @NotBlank(message = "服务项目名称不能为空")
    private String itemName;

    @NotNull(message = "项目大类不能为空")
    private Integer itemCategory;

    private Integer itemSubtype;

    private BigDecimal itemValue;

    private Integer costBearing;

    private String serviceNetwork;

    private String coveredItems;

    private Integer validDays;

    private Integer maxUseCount;

    private String description;

    private Integer sortOrder;

    private Integer status;
}
