package com.dayan.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 表 order_status_change_record 对应实体。
 *
 * <p>订单状态变更记录（原 system_order_status_log，38 号迁移起归入 order 域）。
 * 主键 {@link IdType#AUTO}（该表 AUTO_INCREMENT）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_status_change_record")
public class OrderStatusChangeRecord extends BaseEntity {

    /** 主键（共享表 AUTO_INCREMENT） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单类型（1权益/2场景/3课程/4旅居 等） */
    private Integer orderType;

    /** 订单编号 */
    private String orderCode;

    /** 原状态 */
    private Integer fromStatus;

    /** 新状态 */
    private Integer toStatus;

    /** 变更原因 */
    private String changeReason;

    /** 操作人编码 */
    private String operatorCode;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作人类型（admin/system/channel/agent/client/distributor 等） */
    private String operatorType;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 备注 */
    private String remark;
}
