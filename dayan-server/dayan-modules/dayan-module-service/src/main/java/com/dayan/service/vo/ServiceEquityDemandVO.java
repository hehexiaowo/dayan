package com.dayan.service.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 需求收集 VO。
 */
@Data
public class ServiceEquityDemandVO {

    private Long id;
    private String demandCode;
    private String sessionCode;
    private String clientCode;
    private String butlerCode;
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
    private LocalDateTime collectTime;
    private String demandSummary;
    private String demandImages;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
