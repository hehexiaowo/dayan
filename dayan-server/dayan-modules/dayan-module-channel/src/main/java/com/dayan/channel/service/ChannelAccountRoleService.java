package com.dayan.channel.service;

import java.util.List;

/**
 * 渠道账号-角色关联服务。
 *
 * <p>分配采用"先删后增"全量覆盖语义；关联记录的 channelCode 从账号本身取值。
 */
public interface ChannelAccountRoleService {

    /** 给账号分配角色（全量覆盖） */
    void assignRoles(String accountCode, List<String> roleCodes);

    /** 查询账号的角色编码列表 */
    List<String> listRoles(String accountCode);
}
