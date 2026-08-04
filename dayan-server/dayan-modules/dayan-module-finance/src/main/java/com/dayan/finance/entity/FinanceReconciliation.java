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
 * 表 finance_reconciliation 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("finance_reconciliation")
public class FinanceReconciliation extends BaseEntity {

    /** 主键（分片表，雪花ID，MyBatis-Plus 自动分配） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 对账编码 */
    private String reconCode;

    /** 对账类型 */
    private Integer reconType;

    /** 对账对象编码 */
    private String targetCode;

    /** 对账对象名称 */
    private String targetName;

    /** 对账周期开始 */
    private LocalDate periodStart;

    /** 对账周期结束 */
    private LocalDate periodEnd;

    /** 我方订单数 */
    private Integer ourOrderCount;

    /** 我方总金额 */
    private BigDecimal ourTotalAmount;

    /** 对方订单数 */
    private Integer theirOrderCount;

    /** 对方总金额 */
    private BigDecimal theirTotalAmount;

    /** 差异订单数 */
    private Integer diffCount;

    /** 差异金额 */
    private BigDecimal diffAmount;

    /** 差异明细 */
    private String diffDetail;

    /** 对账结果 */
    private Integer reconResult;

    /** 差异处理结果 */
    private String handleResult;

    /** 对账时间 */
    private LocalDateTime reconTime;

    /** 操作人编码 */
    private String operatorCode;

    /** 操作人姓名 */
    private String operatorName;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
