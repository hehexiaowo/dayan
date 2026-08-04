package com.dayan.equity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 equity_change_holder 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equity_change_holder")
public class EquityChangeHolder extends BaseEntity {

    /** 主键（分片表，雪花ID，MyBatis-Plus 自动分配） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 权益编码 */
    private String equityCode;

    /** 原权益使用人编码 */
    private String oldUsePersonCode;

    /** 原权益人姓名 */
    private String oldPersonName;

    /** 原权益人身份证号 */
    private String oldPersonIdCard;

    /** 新权益使用人编码 */
    private String newUsePersonCode;

    /** 新权益人姓名 */
    private String newPersonName;

    /** 新权益人身份证号 */
    private String newPersonIdCard;

    /** 更换原因 */
    private String changeReason;

    /** 更换状态 */
    private Integer changeStatus;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 操作人编码 */
    private String operatorCode;
}
