package com.dayan.equity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 权益模板 VO。
 */
@Data
public class EquityTemplateVO {

    private Long id;
    private String templateCode;
    private String templateName;
    private Integer equityType;
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
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
