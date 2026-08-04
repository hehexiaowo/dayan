package com.dayan.service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 探访记录（service_visit_record）更新入参（按 id 更新）。
 */
@Data
public class ServiceVisitRecordUpdateDTO {

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
}
