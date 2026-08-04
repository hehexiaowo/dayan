package com.dayan.channel.service;

import com.dayan.channel.dto.ChannelRoleCreateDTO;
import com.dayan.channel.dto.ChannelRoleQueryDTO;
import com.dayan.channel.dto.ChannelRoleUpdateDTO;
import com.dayan.channel.entity.ChannelRole;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 渠道角色服务（P2 简化：基础 CRUD + 授权）。
 *
 * <p>角色编码 {@code roleCode} 全局唯一（RL 前缀），归属于某 channelCode。
 * RBAC 查询逻辑（ChannelStpInterface）后置实现。
 */
public interface ChannelRoleService {

    PageResult<ChannelRole> page(ChannelRoleQueryDTO query);

    ChannelRole getDetail(String roleCode);

    String create(ChannelRoleCreateDTO dto);

    void update(String roleCode, ChannelRoleUpdateDTO dto);

    void delete(String roleCode);

    /** 给角色授权（全量覆盖） */
    void assignPermissions(String roleCode, List<String> permissionCodes);

    /** 查询角色权限码列表 */
    List<String> listPermissions(String roleCode);
}
