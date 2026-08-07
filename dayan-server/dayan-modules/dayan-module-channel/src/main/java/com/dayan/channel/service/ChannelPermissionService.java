package com.dayan.channel.service;

import com.dayan.channel.dto.ChannelPermissionCreateDTO;
import com.dayan.channel.dto.ChannelPermissionQueryDTO;
import com.dayan.channel.dto.ChannelPermissionUpdateDTO;
import com.dayan.channel.entity.ChannelPermission;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 渠道权限服务（P2 简化：基础 CRUD + 全量列表）。
 *
 * <p>{@code channel_permission} 为平台共享的权限字典表（不含 channel_code 字段）。
 */
public interface ChannelPermissionService {

    PageResult<ChannelPermission> page(ChannelPermissionQueryDTO query);

    ChannelPermission getDetail(String permissionCode);

    void create(ChannelPermissionCreateDTO dto);

    void update(String permissionCode, ChannelPermissionUpdateDTO dto);

    void delete(String permissionCode);

    /** 全部启用权限（授权选择用） */
    List<ChannelPermission> listAll();

    /** 权限树（按 parentCode 组装 children，供角色授权 el-tree 用） */
    List<ChannelPermission> tree();
}
