package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 system_state_machine 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_state_machine")
public class SystemStateMachine extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 状态机编码 */
    private String machineCode;

    /** 状态机名称 */
    private String machineName;

    /** 业务类型 */
    private String bizType;

    /** 源主状态值 */
    private Integer fromState;

    /** 源主状态名称 */
    private String fromStateName;

    /** 源子状态值 */
    private String fromSubState;

    /** 目标主状态值 */
    private Integer toState;

    /** 目标主状态名称 */
    private String toStateName;

    /** 目标子状态值 */
    private String toSubState;

    /** 触发事件编码 */
    private String eventCode;

    /** 触发事件名称 */
    private String eventName;

    /** 流转条件表达式 */
    private String conditionExpr;

    /** 流转执行器 */
    private String actionBean;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
