package com.dayan.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 表 finance_invoice 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("finance_invoice")
public class FinanceInvoice extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 发票编码 */
    private String invoiceCode;

    /** 发票类型 */
    private Integer invoiceType;

    /** 关联结算单编码 */
    private String billCode;

    /** 关联订单编码 */
    private String orderCode;

    /** 申请方类型 */
    private String applicantType;

    /** 申请方编码 */
    private String applicantCode;

    /** 申请方名称 */
    private String applicantName;

    /** 抬头类型 */
    private Integer titleType;

    /** 发票抬头 */
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
    private BigDecimal invoiceAmount;

    /** 发票内容 */
    private String invoiceContent;

    /** 收件人姓名 */
    private String receiverName;

    /** 收件人电话 */
    private String receiverPhone;

    /** 收件地址 */
    private String receiverAddress;

    /** 收件邮箱 */
    private String receiverEmail;

    /** 发票号码 */
    private String invoiceNo;

    /** 发票文件URL */
    private String invoiceUrl;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 开票时间 */
    private LocalDateTime issueTime;

    /** 寄出时间 */
    private LocalDateTime sendTime;

    /** 状态 */
    private Integer invoiceStatus;

    /** 备注 */
    private String remark;
}
