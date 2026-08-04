package com.dayan.equity.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 出库（outbound）入参。
 *
 * <p>对每张权益校验 equity_status=0（库存中），经状态机 outbound:0→1，
 * 写 outbound_time/outbound_channel_code/outbound_agent_code/logistics_no；
 * 联动 batch.outboundCount += N。
 */
@Data
public class OutboundDTO {

    /** 出库的权益编码列表（至少 1 条） */
    @NotEmpty(message = "权益编码列表不能为空")
    private List<String> equityCodes;

    /** 出库寄送渠道编码（可空） */
    private String outboundChannelCode;
    /** 出库寄送代理人编码（可空） */
    private String outboundAgentCode;
    /** 物流单号（可空） */
    private String logisticsNo;
}
