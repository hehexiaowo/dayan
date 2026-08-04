package com.dayan.agent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代理人信息 VO。
 */
@Data
public class AgentInfoVO {

    private Long id;
    private String agentCode;
    private String fullName;
    private Integer gender;
    private String avatar;
    private String phone;
    private String email;
    private String idCard;
    private String channelCode;
    private String companyName;
    private String branchName;
    private String department;
    private String position;
    private String employeeNo;
    private String licenseNo;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String address;
    private String serviceIntro;
    private Integer clientCount;
    private Integer totalOrderCount;
    private BigDecimal totalOrderAmount;
    private Integer agentLevel;
    private Integer isCertified;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
