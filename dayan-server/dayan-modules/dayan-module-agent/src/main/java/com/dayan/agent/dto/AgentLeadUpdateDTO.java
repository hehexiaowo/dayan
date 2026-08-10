package com.dayan.agent.dto;

import lombok.Data;

/**
 * 代理人线索更新入参（状态变更 + 信息修改）。
 */
@Data
public class AgentLeadUpdateDTO {

    private String name;
    private String phone;
    private Integer gender;
    private Integer age;

    /** 线索状态（1=新线索, 2=跟进中, 3=意向, 4=已转化, 5=已流失） */
    private Integer leadStatus;

    /** 意向等级 */
    private Integer intentionLevel;
    private String interestType;
    private String region;
    private String remark;
}
