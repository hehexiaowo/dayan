package com.dayan.equity.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 权益使用人 VO。
 *
 * <p>{@code usePersonIdCard} 在管理端按需返回（默认从加密存储解密后回传）。
 */
@Data
public class EquityUsePersonVO {

    private Long id;
    private String equityCode;
    private String clientCode;
    private String usePersonName;
    private Integer usePersonGender;
    private LocalDate usePersonBirthday;
    private Integer usePersonAge;
    private String usePersonPhone;
    /** 身份证号（解密后明文） */
    private String usePersonIdCard;
    private String relationWithHolder;
    private String healthStatus;
    private String careNeed;
    private Integer isDefaultHolder;
    private String remark;
    private LocalDateTime createdAt;
}
