package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Agent 端收藏请求体（agentCode 从登录上下文注入，不由客户端传）。
 */
@Data
public class AgentFavoriteAgentDTO {

    /** 收藏对象类型（1=养老机构, 2=场景, 3=课程, 4=内容） */
    @NotNull(message = "收藏对象类型不能为空")
    private Integer targetType;

    /** 收藏对象编码 */
    @NotBlank(message = "收藏对象编码不能为空")
    private String targetCode;
}
