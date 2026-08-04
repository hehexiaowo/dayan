package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 system_order_status_log 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_order_status_log")
public class SystemOrderStatusLog extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 订单类型 */
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

    /** 操作人类型 */
    private String operatorType;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 备注 */
    private String remark;
}
