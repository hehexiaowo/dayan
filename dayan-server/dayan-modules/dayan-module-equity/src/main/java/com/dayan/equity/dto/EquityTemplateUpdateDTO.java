package com.dayan.equity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 权益模板修改入参（字段可选更新）。
 *
 * <p>{@code templateCode}、{@code equityType} 不可改（被权益卡冗余引用，避免数据不一致）。
 */
@Data
public class EquityTemplateUpdateDTO {

    private String templateName;
    private Integer equityLevel;
    private BigDecimal equityValue;
    private BigDecimal costPrice;
    private String contentDescription;
    private String serviceItems;
    private String applicableParks;
    private String applicableCities;
    private Integer validDays;
    private Integer shelfLifeDays;
    private Integer isTransferable;
    private Integer isStackable;
    private Integer maxUseCount;
    private String coverImage;
    private String cardDesignUrl;
    private String terms;
    private Integer sortOrder;
    /** 状态：0=停用/1=启用/2=已下架 */
    private Integer status;
    private String remark;
}
