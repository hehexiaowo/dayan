package com.dayan.equity.dto;

import lombok.Data;

/**
 * 权益卡/函（equity_depot）查询入参（分页 + 多条件）。
 */
@Data
public class EquityDepotQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String equityCode;
    private String equityNo;
    private String goodsCode;
    private String batchCode;
    private String channelCode;
    private String agentCode;
    private String clientCode;
    private Integer carrierType;
    /** 权益状态：0=库存中 / 1=已出库 / 2=已激活 / 3=使用中 / 4=已完成 / 5=已过期 / 6=已作废 / 7=更换权益人中 */
    private Integer equityStatus;
    private String activateCode;
    private String bindCode;
    /** 模糊搜索关键词（匹配 equityNo 卡号） */
    private String keyword;
}
