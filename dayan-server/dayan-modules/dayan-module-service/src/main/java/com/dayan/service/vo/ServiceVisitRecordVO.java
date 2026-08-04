package com.dayan.service.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 探访记录 VO。
 */
@Data
public class ServiceVisitRecordVO {

    private Long id;
    private String butlerCode;
    private String parkCode;
    private LocalDate visitDate;
    private Integer visitPurpose;
    private String facilityCheck;
    private String serviceCheck;
    private String hygieneCheck;
    private String foodCheck;
    private String safetyCheck;
    private BigDecimal overallScore;
    private String issuesFound;
    private String improvementSuggestions;
    private String images;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
