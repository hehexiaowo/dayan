package com.dayan.equity.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 权益使用人修改入参（字段可选更新）。
 */
@Data
public class EquityUsePersonUpdateDTO {

    private String usePersonName;
    private Integer usePersonGender;
    private LocalDate usePersonBirthday;
    private Integer usePersonAge;
    private String usePersonPhone;
    /** 身份证号（明文，更新时加密） */
    private String usePersonIdCard;
    private String relationWithHolder;
    private String healthStatus;
    private String careNeed;
    private String remark;
}
