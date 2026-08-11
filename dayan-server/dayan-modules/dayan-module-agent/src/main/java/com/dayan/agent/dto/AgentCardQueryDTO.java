package com.dayan.agent.dto;

import lombok.Data;

/**
 * 代理人名片查询入参。
 */
@Data
public class AgentCardQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    /** 关键字（模糊搜索名片名称/显示姓名/手机号） */
    private String keyword;

    /** 状态：1=启用 0=停用（null=全部） */
    private Integer status;
}
