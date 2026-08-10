package com.dayan.agent.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代理人线索新增入参。
 */
@Data
public class AgentLeadCreateDTO {

    @Size(max = 100, message = "姓名不能超过100字")
    private String name;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    /** 性别（0=未知, 1=男, 2=女） */
    private Integer gender;

    private Integer age;

    /** 来源类型（默认1=手工录入） */
    private Integer sourceType;

    /** 意向等级（1=低, 2=中, 3=高） */
    private Integer intentionLevel;

    @Size(max = 200, message = "关注养老类型不能超过200字")
    private String interestType;

    @Size(max = 200, message = "关注区域不能超过200字")
    private String region;

    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}
