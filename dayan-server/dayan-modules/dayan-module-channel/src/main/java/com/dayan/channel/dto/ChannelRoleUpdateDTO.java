package com.dayan.channel.dto;

import lombok.Data;

/**
 * 渠道角色更新入参。
 */
@Data
public class ChannelRoleUpdateDTO {

    private String roleName;
    private Integer roleType;
    private String description;
    private Integer status;
    private Integer sortOrder;
}
