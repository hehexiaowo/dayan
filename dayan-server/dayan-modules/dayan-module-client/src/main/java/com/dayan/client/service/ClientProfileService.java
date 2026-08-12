package com.dayan.client.service;

import com.dayan.client.dto.ClientProfileUpdateDTO;
import com.dayan.client.vo.ClientProfileVO;

/**
 * Client 端个人资料自助服务（查看 / 编辑）。
 *
 * <p>当前登录客户由 {@code ContextHolder.getAccountCode()}（= clientCode）定位，防越权。
 */
public interface ClientProfileService {

    /** 获取当前登录客户的完整资料（含脱敏 + 渠道/区划名回填） */
    ClientProfileVO getProfile();

    /** 更新当前登录客户的基础资料（白名单字段） */
    void updateProfile(ClientProfileUpdateDTO dto);
}
