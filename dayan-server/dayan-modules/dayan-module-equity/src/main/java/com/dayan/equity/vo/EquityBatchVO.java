package com.dayan.equity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 权益批次 VO（含 8 个统计字段）。
 */
@Data
public class EquityBatchVO {

    private Long id;
    private String batchCode;
    private String batchName;
    private String goodsCode;
    private String channelCode;
    private Integer totalQuantity;
    private Integer producedCount;
    private Integer allocatedCount;
    private Integer outboundCount;
    private Integer activatedCount;
    private Integer usedCount;
    private Integer expiredCount;
    private Integer voidedCount;
    private Integer remainCount;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private LocalDate produceDate;
    private LocalDate expireDate;
    /** 批次状态：0=待生产 / 1=生产中 / 2=已完成 / 3=已出库 / 4=已关闭 */
    private Integer batchStatus;
    private String remark;
    private LocalDateTime createdAt;
}
