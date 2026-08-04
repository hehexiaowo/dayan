package com.dayan.supplier.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商评估视图对象。
 */
@Data
public class SupplierEvaluationVO {

    private Long id;
    private String supplierCode;
    private String evalPeriod;
    private Integer evalType;
    private BigDecimal serviceQualityScore;
    private BigDecimal facilityQualityScore;
    private BigDecimal cooperationScore;
    private BigDecimal complaintRate;
    private Integer totalOrderCount;
    private Integer complaintCount;
    private BigDecimal totalScore;
    private Integer scoreLevel;
    private String evalContent;
    private String improvementSuggestions;
    private String evaluatorCode;
    private String evaluatorName;
    private LocalDate evalDate;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
