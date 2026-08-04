package com.dayan.channel.dto;

import lombok.Data;

/**
 * 渠道权限查询入参。
 */
@Data
public class ChannelPermissionQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String permissionName;
    private Integer permissionType;
    private Integer status;
}
