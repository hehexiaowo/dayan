package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代理人-客户绑定入参。
 *
 * <p>bind_type 默认 1（权益赠送绑定）；同一 agent_code + client_code 仅允许一条 status=1 的有效绑定。
 */
@Data
public class AgentClientRelBindDTO {

    @NotBlank(message = "代理人编码不能为空")
    @Size(max = 50)
    private String agentCode;

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 50)
    private String clientCode;

    /** 绑定类型（1=权益赠送绑定, 2=活动邀请绑定, 3=自主绑定） */
    private Integer bindType;
}
