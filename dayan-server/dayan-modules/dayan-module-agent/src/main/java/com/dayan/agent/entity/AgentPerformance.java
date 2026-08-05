package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 agent_performance 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_performance")
public class AgentPerformance extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 代理人编码 */
    private String agentCode;

    /** 所属渠道编码 */
    private String channelCode;

    /** 统计周期 */
    private Integer periodType;

    /** 周期值 */
    private String periodValue;

    /** 权益赠送次数 */
    private Integer equityGrantCount;

    /** 权益赠送金额 */
    private BigDecimal equityGrantAmount;

    /** 场景订单数 */
    private Integer sceneOrderCount;

    /** 场景订单金额 */
    private BigDecimal sceneOrderAmount;

    /** 课程订单数 */
    private Integer courseOrderCount;

    /** 课程订单金额 */
    private BigDecimal courseOrderAmount;
}
