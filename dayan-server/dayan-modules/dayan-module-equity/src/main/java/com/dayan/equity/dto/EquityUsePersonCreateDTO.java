package com.dayan.equity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 权益使用人登记入参。
 *
 * <p>应用层校验：同 equity_code 下使用人 ≤3、身份证号唯一（解密后比对）、
 * 设为默认时旧默认置 0。
 */
@Data
public class EquityUsePersonCreateDTO {

    @NotBlank(message = "权益编码不能为空")
    private String equityCode;

    @NotBlank(message = "权益持有人编码不能为空")
    private String clientCode;

    @NotBlank(message = "使用人姓名不能为空")
    private String usePersonName;

    private Integer usePersonGender;
    private LocalDate usePersonBirthday;
    private Integer usePersonAge;
    private String usePersonPhone;

    /** 身份证号（明文，AES-GCM 加密后存储） */
    private String usePersonIdCard;

    private String relationWithHolder;
    private String healthStatus;
    private String careNeed;

    /** 是否默认权益人：1=是 / 0=否（默认 0） */
    private Integer isDefaultHolder;

    private String remark;
}
