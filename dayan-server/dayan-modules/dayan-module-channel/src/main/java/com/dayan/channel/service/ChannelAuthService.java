package com.dayan.channel.service;

import com.dayan.channel.dto.AuthLoginDTO;
import com.dayan.channel.vo.AuthLoginVO;

/**
 * Channel 渠道端认证服务。
 */
public interface ChannelAuthService {

    /**
     * 登录：用户名/手机号/邮箱 + 密码。
     *
     * @return 登录成功信息（含 Token）
     */
    AuthLoginVO login(AuthLoginDTO dto);

    /**
     * 登出。
     */
    void logout();

    /**
     * 获取当前登录人信息。
     */
    AuthLoginVO current();
}
