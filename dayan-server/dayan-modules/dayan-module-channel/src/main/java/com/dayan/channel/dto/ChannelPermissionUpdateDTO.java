package com.dayan.channel.dto;

import lombok.Data;

/**
 * 渠道权限更新入参。
 */
@Data
public class ChannelPermissionUpdateDTO {

    private String permissionName;
    private String parentCode;
    private Integer permissionType;
    private String path;
    private String method;
    private Integer sortOrder;
    private Integer status;
}
