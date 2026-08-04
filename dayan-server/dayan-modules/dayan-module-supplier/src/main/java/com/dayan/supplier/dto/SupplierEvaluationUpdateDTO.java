package com.dayan.supplier.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商评估更新入参（{@code id} 由路径参数提供）。
 */
@Data
public class SupplierEvaluationUpdateDTO {

    private String evalPeriod;
    private Integer evalType;
    private BigDecimal serviceQualityScore;
    private BigDecimal facilityQualityScore;
    private BigDecimal cooperationScore;
    private BigDecimal complaintRate;
    private Integer totalOrderCount;
    private Integer complaintCount;
    /** 显式传入则覆盖自动计算；为空时自动算 */
    private BigDecimal totalScore;
    private Integer scoreLevel;
    private String evalContent;
    private String improvementSuggestions;
    private String evaluatorCode;
    private String evaluatorName;
    private LocalDate evalDate;
    private Integer status;
    private String remark;
}
