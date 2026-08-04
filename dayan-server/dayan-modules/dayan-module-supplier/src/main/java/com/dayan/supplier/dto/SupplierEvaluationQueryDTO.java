package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商评估查询入参。
 */
@Data
public class SupplierEvaluationQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String supplierCode;
    /** 评价周期 YYYYQN（如 2026Q3） */
    private String evalPeriod;
    private Integer evalType;
    private Integer scoreLevel;
    private Integer status;
}
