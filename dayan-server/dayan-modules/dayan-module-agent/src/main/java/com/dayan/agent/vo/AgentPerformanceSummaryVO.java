package com.dayan.agent.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 代理人业绩汇总 VO。
 *
 * <p>跨周期汇总：把若干条业绩记录的次数/金额相加。
 */
@Data
public class AgentPerformanceSummaryVO {

    private String agentCode;
    /** 累计权益赠送次数 */
    private Long totalEquityGrantCount;
    /** 累计权益赠送金额 */
    private BigDecimal totalEquityGrantAmount;
    /** 累计场景订单数 */
    private Long totalSceneOrderCount;
    /** 累计场景订单金额 */
    private BigDecimal totalSceneOrderAmount;
    /** 累计课程订单数 */
    private Long totalCourseOrderCount;
    /** 累计课程订单金额 */
    private BigDecimal totalCourseOrderAmount;
    /** 参与汇总的记录数 */
    private Long recordCount;
}
