package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代理人收藏入参。
 */
@Data
public class AgentFavoriteAddDTO {

    @NotBlank(message = "代理人编码不能为空")
    @Size(max = 50)
    private String agentCode;

    /** 收藏对象类型（1=养老机构, 2=场景, 3=课程, 4=内容） */
    @NotNull(message = "收藏对象类型不能为空")
    private Integer targetType;

    @NotBlank(message = "收藏对象编码不能为空")
    @Size(max = 50)
    private String targetCode;
}
