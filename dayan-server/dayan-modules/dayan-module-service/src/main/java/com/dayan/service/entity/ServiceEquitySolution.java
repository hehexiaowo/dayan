package com.dayan.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 表 service_equity_solution 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_equity_solution")
public class ServiceEquitySolution extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 服务会话编码 */
    private String sessionCode;

    /** 关联需求编码 */
    private String demandCode;

    /** 客户编码 */
    private String clientCode;

    /** 管家编码 */
    private String butlerCode;

    /** 方案编码 */
    private String solutionCode;

    /** 方案名称 */
    private String solutionName;

    /** 方案类型 */
    private Integer solutionType;

    /** 推荐机构列表 */
    private String recommendedParks;

    /** 方案概述 */
    private String planSummary;

    /** 服务项目明细 */
    private String serviceItems;

    /** 预估费用 */
    private BigDecimal estimatedCost;

    /** 费用明细 */
    private String costBreakdown;

    /** 服务时间安排 */
    private String timeline;

    /** 方案优势 */
    private String advantages;

    /** 注意事项/风险提示 */
    private String risks;

    /** 与备选方案对比说明 */
    private String comparison;

    /** 方案呈现时间 */
    private LocalDateTime presentationTime;

    /** 呈现方式 */
    private Integer presentationMethod;

    /** 客户反馈 */
    private String clientFeedback;

    /** 客户是否接受 */
    private Integer isAccepted;

    /** 调整次数 */
    private Integer adjustCount;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
