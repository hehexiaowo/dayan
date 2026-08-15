package com.dayan.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ServiceItemVO {

    private Long id;
    private String itemCode;
    private String itemName;
    private Integer itemCategory;
    private Integer itemSubtype;
    private BigDecimal itemValue;
    private Integer costBearing;
    /** 服务网络范围（结构化；null=业态全部机构，custom=自选可精确到房型） */
    private com.dayan.goods.model.NetworkScope networkScope;
    private String coveredItems;
    private Integer validDays;
    private Integer maxUseCount;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
