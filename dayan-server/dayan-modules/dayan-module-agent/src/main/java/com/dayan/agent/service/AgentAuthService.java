package com.dayan.agent.service;

import com.dayan.agent.dto.AgentLoginDTO;
import com.dayan.agent.dto.SmsLoginDTO;
import com.dayan.agent.vo.AgentLoginVO;
import com.dayan.agent.vo.ChannelOptionVO;

import java.util.List;

/**
 * Agent 代理人端认证服务。
 *
 * <p>支持"选渠道"特性：先按手机号/OpenID 检索关联渠道，再选定渠道登录。
 * <p>支持三种登录方式：密码登录、短信验证码登录、微信授权登录。
 */
public interface AgentAuthService {

    /**
     * 按手机号/用户名或微信 OpenID 检索关联的所有渠道（去重 channel_code）。
     *
     * @param mobile 手机号或用户名（与 openId 至少传一个）
     * @param openId 微信 OpenID（与 mobile 至少传一个）
     * @return 渠道可选项列表
     */
    List<ChannelOptionVO> listChannels(String mobile, String openId);

    /**
     * 密码登录：channelCode + identifier（手机号/OpenID/用户名）+ 密码。
     *
     * @return 登录成功信息（含 Token）
     */
    AgentLoginVO login(AgentLoginDTO dto);

    /**
     * 验证码登录：channelCode + 手机号 + 短信验证码。
     *
     * @param dto 验证码登录请求
     * @return 登录成功信息（含 Token）
     */
    AgentLoginVO smsLogin(SmsLoginDTO dto);

    /**
     * 微信授权登录：channelCode + 微信 code → openId 匹配 agent_account。
     *
     * @param code        微信登录凭证
     * @param channelCode 渠道编码
     * @return 登录成功信息（含 Token）
     */
    AgentLoginVO wxLogin(String code, String channelCode);

    /**
     * 登出。
     */
    void logout();

    /**
     * 获取当前登录人信息。
     */
    AgentLoginVO current();
}
