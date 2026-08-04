package com.dayan.butler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalTime;
/**
 * 表 butler_schedule 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("butler_schedule")
public class ButlerSchedule extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 管家编码 */
    private String butlerCode;

    /** 排班日期 */
    private LocalDate scheduleDate;

    /** 排班类型 */
    private Integer scheduleType;

    /** 上班时间 */
    private LocalTime startTime;

    /** 下班时间 */
    private LocalTime endTime;

    /** 状态 */
    private Integer status;
}
