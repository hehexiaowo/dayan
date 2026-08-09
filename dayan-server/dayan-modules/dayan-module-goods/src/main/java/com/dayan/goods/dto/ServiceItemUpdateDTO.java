package com.dayan.goods.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceItemUpdateDTO {

    private String itemName;
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
