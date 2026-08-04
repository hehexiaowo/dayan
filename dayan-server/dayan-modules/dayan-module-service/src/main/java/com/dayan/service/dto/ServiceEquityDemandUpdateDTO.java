package com.dayan.service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 需求收集（service_equity_demand）更新入参（按 id 更新）。
 */
@Data
public class ServiceEquityDemandUpdateDTO {

    private Integer demandType;
    private String usePersonName;
    private Integer usePersonAge;
    private Integer usePersonGender;
    private String healthSummary;
    private Integer careLevelNeed;
    private String cityPreference;
    private String areaPreference;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String roomPreference;
    private String foodPreference;
    private String specialNeeds;
    private LocalDate expectedTime;
    private Integer contactPreference;
    private Integer collectMethod;
    private String demandSummary;
    private String demandImages;
    private Integer status;
    private String remark;
}
