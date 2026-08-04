package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 代理人业绩创建入参。
 */
@Data
public class AgentPerformanceCreateDTO {

    @NotBlank(message = "代理人编码不能为空")
    @Size(max = 50)
    private String agentCode;

    /** 所属渠道编码（不传则取当前登录上下文） */
    @Size(max = 50)
    private String channelCode;

    /** 统计周期（1=日, 2=周, 3=月, 4=季, 5=年） */
    @NotNull(message = "统计周期不能为空")
    private Integer periodType;

    @NotBlank(message = "周期值不能为空")
    @Size(max = 20)
    private String periodValue;

    private Integer equityGrantCount;
    private BigDecimal equityGrantAmount;
    private Integer sceneOrderCount;
    private BigDecimal sceneOrderAmount;
    private Integer courseOrderCount;
    private BigDecimal courseOrderAmount;
}
