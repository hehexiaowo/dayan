package com.dayan.equity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 equity_batch 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equity_batch")
public class EquityBatch extends BaseEntity {

    /** 主键（分片表，雪花ID，MyBatis-Plus 自动分配） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 批次编码 */
    private String batchCode;

    /** 批次名称 */
    private String batchName;

    /** 权益模板编码 */
    private String templateCode;

    /** 分配渠道编码 */
    private String channelCode;

    /** 总数量 */
    private Integer totalQuantity;

    /** 已生成数量 */
    private Integer producedCount;

    /** 已分配数量 */
    private Integer allocatedCount;

    /** 已出库数量 */
    private Integer outboundCount;

    /** 已激活数量 */
    private Integer activatedCount;

    /** 已使用数量 */
    private Integer usedCount;

    /** 已过期数量 */
    private Integer expiredCount;

    /** 已作废数量 */
    private Integer voidedCount;

    /** 剩余可用数量 */
    private Integer remainCount;

    /** 单位成本 */
    private BigDecimal unitCost;

    /** 批次总成本 */
    private BigDecimal totalCost;

    /** 生产日期 */
    private LocalDate produceDate;

    /** 批次有效期 */
    private LocalDate expireDate;

    /** 批次状态 */
    private Integer batchStatus;

    /** 备注 */
    private String remark;
}
