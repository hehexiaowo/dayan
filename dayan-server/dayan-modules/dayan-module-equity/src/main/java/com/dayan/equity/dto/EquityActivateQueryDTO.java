package com.dayan.equity.dto;

import lombok.Data;

/**
 * 权益激活记录查询入参。
 */
@Data
public class EquityActivateQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String activateCode;
    private String equityCode;
    private String goodsCode;
    private String clientCode;
    private Integer activateChannel;

    /** 渠道隔离用：本渠道权益编码集合 */
    private java.util.List<String> equityCodes;
}
