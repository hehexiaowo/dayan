package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 system_service_change_log 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_service_change_log")
public class SystemServiceChangeLog extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 服务会话编码 */
    private String sessionCode;

    /** 变更类型 */
    private Integer changeType;

    /** 变更前值 */
    private String fromValue;

    /** 变更后值 */
    private String toValue;

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
