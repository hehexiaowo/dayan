package com.dayan.agent.vo;

import lombok.Builder;
import lombok.Data;

/**
 * Agent 代理人端登录成功响应。
 */
@Data
@Builder
public class AgentLoginVO {

    /** Sa-Token 签发的 Token 值 */
    private String token;
    /** Token 请求头名称（Agent-Token） */
    private String tokenName;
    /** 代理人编码 */
    private String agentCode;
    /** 真实姓名（agent_account 无 real_name 字段，预留扩展） */
    private String realName;
    /** 所属渠道编码 */
    private String channelCode;
}
