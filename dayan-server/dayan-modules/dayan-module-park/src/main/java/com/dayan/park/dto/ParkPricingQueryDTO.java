package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构统一定价分页查询参数。
 */
@Data
public class ParkPricingQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private Integer chargeType;
    private String refType;
    private String refCode;
    private Integer billingCycle;
    private Integer isCurrent;
    private Integer status;
}
