package com.dayan.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * 表 finance_bill 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("finance_bill")
public class FinanceBill extends BaseEntity {

    /** 主键（分片表，雪花ID，MyBatis-Plus 自动分配） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 结算单编号 */
    private String billCode;

    /** 结算类型 */
    private Integer billType;

    /** 结算对象类型 */
    private String targetType;

    /** 结算对象编码 */
    private String targetCode;

    /** 结算对象名称 */
    private String targetName;

    /** 结算周期开始 */
    private LocalDate periodStart;

    /** 结算周期结束 */
    private LocalDate periodEnd;

    /** 订单数量 */
    private Integer orderCount;

    /** 结算总额 */
    private BigDecimal totalAmount;

    /** 分销手续费金额 */
    private BigDecimal commissionAmount;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 调整金额 */
    private BigDecimal adjustAmount;

    /** 最终结算金额 */
    private BigDecimal finalAmount;

    /** 关联流水ID列表 */
    private String flowIds;

    /** 结算方式 */
    private Integer settlementMethod;

    /** 收款银行信息 */
    private String bankInfo;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 结算完成时间 */
    private LocalDateTime settleTime;

    /** 审核人编码 */
    private String auditorCode;

    /** 审核人姓名 */
    private String auditorName;

    /** 审核备注 */
    private String auditRemark;

    /** 状态 */
    private Integer billStatus;

    /** 备注 */
    private String remark;
}
