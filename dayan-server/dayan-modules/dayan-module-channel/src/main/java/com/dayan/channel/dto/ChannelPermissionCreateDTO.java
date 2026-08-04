package com.dayan.channel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 渠道权限创建入参。
 *
 * <p>{@code permissionCode} 全局唯一，按"模块:资源:动作"约定（如 channel:info:list）。
 */
@Data
public class ChannelPermissionCreateDTO {

    @NotBlank(message = "权限编码不能为空")
    private String permissionCode;

    @NotBlank(message = "权限名称不能为空")
    private String permissionName;

    private String parentCode;

    /** 权限类型：1菜单 2按钮 3接口 4数据 */
    private Integer permissionType;

    private String path;
    private String method;
    private Integer sortOrder;
    private Integer status;
}
