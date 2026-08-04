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
 * 表 finance_payment 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("finance_payment")
public class FinancePayment extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 支付流水号 */
    private String paymentCode;

    /** 订单类型 */
    private Integer orderType;

    /** 订单编号 */
    private String orderCode;

    /** 支付方式 */
    private Integer payType;

    /** 支付金额 */
    private BigDecimal payAmount;

    /** 第三方交易号 */
    private String tradeNo;

    /** 付款方账号 */
    private String payerAccount;

    /** 收款方账号 */
    private String payeeAccount;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 回调通知时间 */
    private LocalDateTime notifyTime;

    /** 支付状态 */
    private Integer payStatus;

    /** 支付说明 */
    private String payDescription;

    /** 扩展数据 */
    private String extraData;
}
