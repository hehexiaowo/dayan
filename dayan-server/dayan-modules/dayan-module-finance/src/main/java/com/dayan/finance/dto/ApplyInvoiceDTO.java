package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 申请发票（finance_invoice）入参。
 *
 * <p>发票编号、申请时间由服务端生成，invoice_status 置 0（待审核）。
 */
@Data
public class ApplyInvoiceDTO {

    /** 发票类型：1=增值税普通发票/2=增值税专用发票/3=电子发票 */
    @NotNull(message = "发票类型不能为空")
    private Integer invoiceType;

    /** 关联结算单编码（可空） */
    private String billCode;
    /** 关联订单编码（可空） */
    private String orderCode;

    /** 申请方类型：channel/agent/client */
    @NotBlank(message = "申请方类型不能为空")
    private String applicantType;
    /** 申请方编码 */
    @NotBlank(message = "申请方编码不能为空")
    private String applicantCode;
    /** 申请方名称 */
    @NotBlank(message = "申请方名称不能为空")
    private String applicantName;

    /** 抬头类型：1=企业/2=个人（默认 1） */
    private Integer titleType;
    /** 发票抬头 */
    @NotBlank(message = "发票抬头不能为空")
    private String invoiceTitle;
    /** 纳税人识别号 */
    private String taxNo;
    /** 开户银行 */
    private String bankName;
    /** 银行账号 */
    private String bankAccount;
    /** 注册地址 */
    private String registerAddress;
    /** 注册电话 */
    private String registerPhone;

    /** 开票金额 */
    @NotNull(message = "开票金额不能为空")
    private BigDecimal invoiceAmount;

    /** 发票内容 */
    @NotBlank(message = "发票内容不能为空")
    private String invoiceContent;

    /** 收件人姓名 */
    private String receiverName;
    /** 收件人电话 */
    private String receiverPhone;
    /** 收件地址 */
    private String receiverAddress;
    /** 收件邮箱（电子发票） */
    private String receiverEmail;

    /** 备注 */
    private String remark;
}
