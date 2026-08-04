package com.dayan.channel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 渠道角色创建入参。
 *
 * <p>{@code roleCode} 由系统生成（RL 前缀），渠道内唯一。
 */
@Data
public class ChannelRoleCreateDTO {

    @NotBlank(message = "渠道编码不能为空")
    private String channelCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private Integer roleType;
    private String description;
    private Integer status;
    private Integer sortOrder;
}
