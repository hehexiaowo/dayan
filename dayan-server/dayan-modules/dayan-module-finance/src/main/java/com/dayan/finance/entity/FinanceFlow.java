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
 * 表 finance_flow 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("finance_flow")
public class FinanceFlow extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 流水编号 */
    private String flowCode;

    /** 流水类型 */
    private Integer flowType;

    /** 业务类型 */
    private String bizType;

    /** 业务编码 */
    private String bizCode;

    /** 账号类型 */
    private String accountType;

    /** 账号编码 */
    private String accountCode;

    /** 流水金额 */
    private BigDecimal flowAmount;

    /** 变动前余额 */
    private BigDecimal balanceBefore;

    /** 变动后余额 */
    private BigDecimal balanceAfter;

    /** 支付方式 */
    private Integer payType;

    /** 交易流水号 */
    private String tradeNo;

    /** 对方类型 */
    private String counterpartyType;

    /** 对方编码 */
    private String counterpartyCode;

    /** 对方名称 */
    private String counterpartyName;

    /** 流水描述 */
    private String flowDescription;

    /** 流水时间 */
    private LocalDateTime flowTime;

    /** 是否已结算 */
    private Integer isSettled;

    /** 结算单编码 */
    private String settleCode;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
