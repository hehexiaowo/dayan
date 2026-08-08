package com.dayan.agent.dto;

import lombok.Data;

/**
 * 代理人-客户绑定查询入参。
 */
@Data
public class AgentClientRelQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String agentCode;
    private String clientCode;
    /** 绑定类型 */
    private Integer bindType;
    /** 状态（0=已解绑, 1=服务中） */
    private Integer status;
    /** 渠道隔离用：本渠道代理人编码集合（Controller 注入，非前端入参） */
    private java.util.List<String> agentCodes;
}
