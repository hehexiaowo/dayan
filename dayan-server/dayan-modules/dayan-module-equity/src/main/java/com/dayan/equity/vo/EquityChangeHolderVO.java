package com.dayan.equity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权益更换权益人记录 VO。
 *
 * <p>{@code oldPersonIdCard}/{@code newPersonIdCard} 在管理端按需返回（解密后明文）。
 */
@Data
public class EquityChangeHolderVO {

    private Long id;
    private String equityCode;
    private String oldUsePersonCode;
    private String oldPersonName;
    /** 原权益人身份证号（解密后明文） */
    private String oldPersonIdCard;
    private String newUsePersonCode;
    private String newPersonName;
    /** 新权益人身份证号（解密后明文） */
    private String newPersonIdCard;
    private String changeReason;
    /** 更换状态：0=待处理 / 1=已完成 / 2=已回滚 */
    private Integer changeStatus;
    private LocalDateTime operateTime;
    private String operatorCode;
    private LocalDateTime createdAt;
}
