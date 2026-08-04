package com.dayan.equity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 权益批次修改入参（字段可选更新）。
 *
 * <p>统计字段、{@code batchCode}、{@code templateCode} 不可改（被 depot 引用且由联动维护）。
 */
@Data
public class EquityBatchUpdateDTO {

    private String batchName;
    private String channelCode;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private LocalDate produceDate;
    private LocalDate expireDate;
    /** 批次状态：0=待生产/1=生产中/2=已完成/3=已出库/4=已关闭 */
    private Integer batchStatus;
    private String remark;
}
