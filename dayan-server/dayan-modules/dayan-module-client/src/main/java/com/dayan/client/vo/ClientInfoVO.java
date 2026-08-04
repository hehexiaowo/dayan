package com.dayan.client.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户信息 VO。
 */
@Data
public class ClientInfoVO {

    private Long id;
    private String clientCode;
    private String channelCode;
    private String fullName;
    private Integer gender;
    private String avatar;
    private LocalDate birthday;
    private Integer age;
    private String idCard;
    private String phone;
    private String email;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String address;
    private String nationality;
    private String ethnic;
    private Integer education;
    private Integer maritalStatus;
    private String profession;
    private Integer sourceType;
    private String sourceAgentCode;
    private String sourceChannelCode;
    private Integer clientLevel;
    private Integer equityCount;
    private Integer usedEquityCount;
    private Integer serviceCount;
    private BigDecimal totalOrderAmount;
    private LocalDateTime lastServiceTime;
    private LocalDateTime registerTime;
    private LocalDateTime lastLoginTime;
    private Integer isVip;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
