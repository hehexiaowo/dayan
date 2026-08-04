package com.dayan.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户信息创建入参。
 */
@Data
public class ClientInfoCreateDTO {

    @NotBlank(message = "渠道编码不能为空")
    @Size(max = 64)
    private String channelCode;

    @NotBlank(message = "客户姓名不能为空")
    @Size(max = 64)
    private String fullName;

    /** 性别 */
    private Integer gender;

    @Size(max = 512)
    private String avatar;

    private LocalDate birthday;

    private Integer age;

    @Size(max = 32)
    private String idCard;

    @Size(max = 32)
    private String phone;

    @Size(max = 128)
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

    /** 注册时间（可选，默认取当前时间） */
    private LocalDateTime registerTime;
}
