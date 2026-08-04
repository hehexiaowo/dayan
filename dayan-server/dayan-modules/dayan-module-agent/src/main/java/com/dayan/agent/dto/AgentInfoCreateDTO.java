package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代理人创建入参。
 *
 * <p>agent_code 由服务端按 "AG + 时间戳后 5 位 + 随机 3 位" 生成（渠道内唯一）；
 * channel_code 默认从当前登录上下文取值（TenantLineHandler 自动注入），也可显式传入。
 */
@Data
public class AgentInfoCreateDTO {

    @NotBlank(message = "代理人姓名不能为空")
    @Size(max = 50)
    private String fullName;

    /** 性别（0=未知, 1=男, 2=女） */
    private Integer gender;

    @Size(max = 500)
    private String avatar;

    @NotBlank(message = "手机号不能为空")
    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String idCard;

    /** 所属渠道编码（不传则取当前登录上下文） */
    @Size(max = 50)
    private String channelCode;

    @Size(max = 200)
    private String companyName;

    @Size(max = 200)
    private String branchName;

    @Size(max = 100)
    private String department;

    @Size(max = 100)
    private String position;

    /** 保险公司工号（渠道内唯一，若提供则校验） */
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

    /** 等级（1=普通, 2=银牌, 3=金牌, 4=钻石） */
    private Integer agentLevel;

    /** 是否认证（0=否, 1=是） */
    private Integer isCertified;

    /** 状态（0=禁用, 1=正常, 2=冻结） */
    private Integer status;

    @Size(max = 500)
    private String remark;
}
