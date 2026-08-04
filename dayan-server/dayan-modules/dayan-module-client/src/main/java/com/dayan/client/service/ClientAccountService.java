package com.dayan.client.service;

import com.dayan.client.dto.ClientAccountCreateDTO;
import com.dayan.client.dto.ClientAccountQueryDTO;
import com.dayan.client.dto.ClientAccountUpdateDTO;
import com.dayan.client.vo.ClientAccountVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 客户账号服务（按 channel_code 隔离）。
 */
public interface ClientAccountService {

    PageResult<ClientAccountVO> page(ClientAccountQueryDTO query);

    /**
     * 新增账号（BCrypt 哈希密码）。
     *
     * @return clientCode
     */
    String create(ClientAccountCreateDTO dto);

    /**
     * 修改账号（不允许通过 update 改密码）。
     */
    void update(String clientCode, ClientAccountUpdateDTO dto);

    /**
     * 重置密码为默认值。
     */
    void resetPassword(String clientCode);

    /**
     * 删除账号。
     */
    void delete(String clientCode);
}
