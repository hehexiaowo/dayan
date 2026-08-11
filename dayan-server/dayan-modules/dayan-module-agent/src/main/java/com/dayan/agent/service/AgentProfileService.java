package com.dayan.agent.service;

import com.dayan.agent.dto.AgentProfileUpdateDTO;
import com.dayan.agent.vo.AgentProfileVO;
import com.dayan.agent.vo.SmsSendVO;

/**
 * Agent 端个人资料服务（当前登录代理人自服务）。
 */
public interface AgentProfileService {

    /** 查询我的资料（聚合 agent_account + agent_info + 渠道/区划名称） */
    AgentProfileVO getProfile();

    /** 更新基础资料（白名单字段；已认证代理人不可改姓名） */
    void updateProfile(AgentProfileUpdateDTO dto);

    /** 换绑手机号：给新手机号发验证码（60s 冷却） */
    SmsSendVO sendPhoneChangeCode(String mobile);

    /** 换绑手机号：校验验证码，同事务更新 agent_account + agent_info */
    void changePhone(String mobile, String code);
}
