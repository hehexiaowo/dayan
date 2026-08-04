package com.dayan.agent.service;

import com.dayan.agent.dto.AgentLoginDTO;
import com.dayan.agent.vo.AgentLoginVO;
import com.dayan.agent.vo.ChannelOptionVO;

import java.util.List;

/**
 * Agent 代理人端认证服务。
 *
 * <p>支持"选渠道"特性：先按手机号/OpenID 检索关联渠道，再选定渠道登录。
 */
public interface AgentAuthService {

    /**
     * 按手机号或微信 OpenID 检索关联的所有渠道（去重 channel_code）。
     *
     * @param mobile 手机号（与 openId 至少传一个）
     * @param openId 微信 OpenID（与 mobile 至少传一个）
     * @return 渠道可选项列表
     */
    List<ChannelOptionVO> listChannels(String mobile, String openId);

    /**
     * 登录：channelCode + identifier（手机号或 OpenID）+ 密码。
     *
     * @return 登录成功信息（含 Token）
     */
    AgentLoginVO login(AgentLoginDTO dto);

    /**
     * 登出。
     */
    void logout();

    /**
     * 获取当前登录人信息。
     */
    AgentLoginVO current();
}
