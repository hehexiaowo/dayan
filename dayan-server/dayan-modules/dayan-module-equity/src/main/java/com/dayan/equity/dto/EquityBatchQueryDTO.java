package com.dayan.equity.dto;

import lombok.Data;

/**
 * 权益批次查询入参（分页 + 多条件）。
 */
@Data
public class EquityBatchQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String batchCode;
    private String batchName;
    private String goodsCode;
    private String channelCode;
    private Integer batchStatus;
}
