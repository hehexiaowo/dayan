package com.dayan.finance.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发票（finance_invoice）视图对象。
 */
@Data
public class FinanceInvoiceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String invoiceCode;
    /** 发票类型：1=增值税普通发票/2=增值税专用发票/3=电子发票 */
    private Integer invoiceType;
    private String billCode;
    private String orderCode;
    private String applicantType;
    private String applicantCode;
    private String applicantName;
    /** 抬头类型：1=企业/2=个人 */
    private Integer titleType;
    private String invoiceTitle;
    private String taxNo;
    private String bankName;
    private String bankAccount;
    private String registerAddress;
    private String registerPhone;
    private BigDecimal invoiceAmount;
    private String invoiceContent;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String receiverEmail;
    private String invoiceNo;
    private String invoiceUrl;
    private LocalDateTime applyTime;
    private LocalDateTime issueTime;
    private LocalDateTime sendTime;
    /** 状态：0=待审核/1=已审核/2=已开票/3=已寄出/4=已完成/5=已作废/6=已红冲 */
    private Integer invoiceStatus;
    private String remark;
    private LocalDateTime createdAt;
}
