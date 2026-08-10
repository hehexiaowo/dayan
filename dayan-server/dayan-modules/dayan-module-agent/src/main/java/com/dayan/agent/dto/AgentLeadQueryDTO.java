package com.dayan.agent.dto;

import lombok.Data;

/**
 * 代理人线索查询入参。
 */
@Data
public class AgentLeadQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    /** 关键字（模糊搜索姓名/手机号） */
    private String keyword;

    /** 线索状态（1=新线索, 2=跟进中, 3=意向, 4=已转化, 5=已流失） */
    private Integer leadStatus;

    /** 来源类型 */
    private Integer sourceType;

    /** 意向等级 */
    private Integer intentionLevel;
}
