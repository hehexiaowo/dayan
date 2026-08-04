package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 方案定制（service_equity_solution）创建入参。
 *
 * <p>solutionCode(SO+10) 由服务端生成。solutionType 1=推荐/2=备选。
 * demandCode 关联需求编码（NOT NULL）。
 */
@Data
public class ServiceEquitySolutionCreateDTO {

    @NotBlank(message = "会话编码不能为空")
    private String sessionCode;

    @NotBlank(message = "需求编码不能为空")
    private String demandCode;

    @NotBlank(message = "客户编码不能为空")
    private String clientCode;

    private String butlerCode;

    private String solutionName;

    /** 方案类型（1=推荐方案, 2=备选方案） */
    private Integer solutionType;

    /** 推荐机构列表（JSON 数组） */
    private String recommendedParks;
    private String planSummary;
    private String serviceItems;
    private BigDecimal estimatedCost;
    private String costBreakdown;
    private String timeline;
    private String advantages;
    private String risks;
    private String comparison;
    private Integer presentationMethod;
    private String remark;
}
