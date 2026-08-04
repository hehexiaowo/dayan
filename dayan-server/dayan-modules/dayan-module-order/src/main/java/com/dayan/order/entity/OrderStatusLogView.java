package com.dayan.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 订单状态变更日志只读 View（跨域共享表 {@code system_order_status_log}）。
 *
 * <p>该表属系统域（共享表，AUTO_INCREMENT），订单域需向其写日志，但<strong>不依赖系统域模块</strong>
 * （避免循环依赖）。采用 P3/P5 已确立的 View 模式：在订单域内以独立实体映射同一张物理表，
 * 直接读写，{@link com.dayan.order.mapper.OrderStatusLogViewMapper}。
 *
 * <p>字段与 {@code SystemOrderStatusLog} 完全对齐。主键 {@link IdType#AUTO}（该表 AUTO_INCREMENT）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_order_status_log")
public class OrderStatusLogView extends BaseEntity {

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
