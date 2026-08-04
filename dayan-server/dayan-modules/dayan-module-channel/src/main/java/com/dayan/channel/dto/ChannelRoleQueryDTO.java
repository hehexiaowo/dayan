package com.dayan.channel.dto;

import lombok.Data;

/**
 * 渠道角色查询入参。
 */
@Data
public class ChannelRoleQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String channelCode;
    private String roleName;
    private Integer roleType;
    private Integer status;
}
