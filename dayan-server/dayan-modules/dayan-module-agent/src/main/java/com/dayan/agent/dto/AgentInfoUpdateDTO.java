package com.dayan.agent.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代理人更新入参（agentCode 不可改，由路径参数提供）。
 */
@Data
public class AgentInfoUpdateDTO {

    @Size(max = 50)
    private String fullName;
    private Integer gender;

    @Size(max = 500)
    private String avatar;

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String idCard;

    @Size(max = 200)
    private String companyName;

    @Size(max = 200)
    private String branchName;

    @Size(max = 100)
    private String department;

    @Size(max = 100)
    private String position;

    @Size(max = 50)
    private String employeeNo;

    @Size(max = 50)
    private String licenseNo;

    private String provinceCode;
    private String cityCode;
    private String districtCode;

    @Size(max = 500)
    private String address;

    private String serviceIntro;
    private Integer agentLevel;
    private Integer isCertified;
    private Integer status;

    @Size(max = 500)
    private String remark;
}
