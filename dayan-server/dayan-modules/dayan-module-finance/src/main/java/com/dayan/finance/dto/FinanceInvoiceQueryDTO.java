package com.dayan.finance.dto;

import lombok.Data;

/**
 * 发票（finance_invoice）查询入参（分页 + 多条件）。
 */
@Data
public class FinanceInvoiceQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String invoiceCode;
    /** 发票类型：1=增值税普通发票/2=增值税专用发票/3=电子发票 */
    private Integer invoiceType;
    private String billCode;
    private String orderCode;
    private String applicantType;
    private String applicantCode;
    /** 发票号码（税务号码，非系统编码） */
    private String invoiceNo;
    /** 状态：0=待审核/1=已审核/2=已开票/3=已寄出/4=已完成/5=已作废/6=已红冲 */
    private Integer invoiceStatus;
}
