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
 * 表 finance_refund 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("finance_refund")
public class FinanceRefund extends BaseEntity {

    /** 主键（分片表，雪花ID，MyBatis-Plus 自动分配） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 退款编码 */
    private String refundCode;

    /** 订单类型 */
    private Integer orderType;

    /** 订单编号 */
    private String orderCode;

    /** 原支付记录编码 */
    private String paymentCode;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 退款原因 */
    private String refundReason;

    /** 退款类型 */
    private Integer refundType;

    /** 退款渠道 */
    private Integer refundChannel;

    /** 退款交易号 */
    private String refundTradeNo;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 退款完成时间 */
    private LocalDateTime refundTime;

    /** 审核人编码 */
    private String auditorCode;

    /** 审核人姓名 */
    private String auditorName;

    /** 审核备注 */
    private String auditRemark;

    /** 状态 */
    private Integer refundStatus;

    /** 备注 */
    private String remark;
}
