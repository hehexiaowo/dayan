package com.dayan.equity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权益激活记录 VO。
 */
@Data
public class EquityActivateVO {

    private Long id;
    private String activateCode;
    private String equityCode;
    private String goodsCode;
    private String clientCode;
    private String clientFullName;
    private String clientPhone;
    /** 激活渠道：1=APP / 2=小程序 / 3=H5 / 4=管家代激活 / 5=代理人代激活 */
    private Integer activateChannel;
    private String activateSourceCode;
    private LocalDateTime activateTime;
    private LocalDateTime expireTime;
    private Integer isIdCardVerified;
    private Integer isAgreementSigned;
    private String ipAddress;
    private String deviceInfo;
    private String remark;
    private LocalDateTime createdAt;
}
