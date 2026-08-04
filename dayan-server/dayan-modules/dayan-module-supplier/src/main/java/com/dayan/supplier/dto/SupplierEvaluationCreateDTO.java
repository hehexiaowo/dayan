package com.dayan.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商评估创建入参。
 *
 * <p>{@code totalScore} / {@code scoreLevel} 若不显式传入，由 Service 按公式自动计算：
 * <pre>
 *   totalScore = (service + facility + cooperation) / 3 * (1 - complaintRate/100)   保留 2 位
 *   scoreLevel = 1(A>=90) / 2(B 80-89) / 3(C 70-79) / 4(D<70)
 * </pre>
 */
@Data
public class SupplierEvaluationCreateDTO {

    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;

    private String evalPeriod;
    private Integer evalType;

    private BigDecimal serviceQualityScore;
    private BigDecimal facilityQualityScore;
    private BigDecimal cooperationScore;
    /** 投诉率（百分比，0~100） */
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
