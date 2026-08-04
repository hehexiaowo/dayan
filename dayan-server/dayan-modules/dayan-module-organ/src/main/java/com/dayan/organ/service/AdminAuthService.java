package com.dayan.organ.service;

import com.dayan.organ.dto.AuthLoginDTO;
import com.dayan.organ.vo.AuthLoginVO;

/**
 * Admin 运营端认证服务。
 */
public interface AdminAuthService {

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
