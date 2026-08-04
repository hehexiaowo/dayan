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
 * 表 finance_account 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("finance_account")
public class FinanceAccount extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 账目编码 */
    private String accountCode;

    /** 账目方向 */
    private Integer direction;

    /** 对象类型 */
    private String accountType;

    /** 对象编码 */
    private String targetCode;

    /** 对象名称 */
    private String targetName;

    /** 业务类型 */
    private String bizType;

    /** 业务编码 */
    private String bizCode;

    /** 应收/应付总额 */
    private BigDecimal totalAmount;

    /** 已收/已付金额 */
    private BigDecimal receivedAmount;

    /** 未收/未付金额 */
    private BigDecimal remainAmount;

    /** 到期日期 */
    private LocalDate dueDate;

    /** 最近收款/付款时间 */
    private LocalDateTime lastReceiveTime;

    /** 状态 */
    private Integer accountStatus;

    /** 备注 */
    private String remark;
}
