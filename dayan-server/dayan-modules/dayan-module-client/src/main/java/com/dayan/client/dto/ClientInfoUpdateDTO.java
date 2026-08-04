package com.dayan.client.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 客户信息更新入参（clientCode/channelCode 不可改，由路径参数提供）。
 */
@Data
public class ClientInfoUpdateDTO {

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
    private Integer isVip;
    private Integer status;
    private String remark;
}
