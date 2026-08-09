package com.dayan.equity.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 权益使用人 VO。
 *
 * <p>{@code usePersonIdCard} 在管理端按需返回（默认从加密存储解密后回传）。
 *
 * <p>{@code id} 序列化为字符串：雪花ID超过 JS Number.MAX_SAFE_INTEGER，
 * 前端按数字解析会丢精度（如 102026144974372868 变成 102026144974372860），
 * 导致回传后端时 Long.parseLong 不匹配。
 */
@Data
public class EquityUsePersonVO {

    @JsonSerialize(using = ToStringSerializer.class)
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
