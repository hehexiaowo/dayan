package com.dayan.scene.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
/**
 * 表 scene_schedule 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scene_schedule")
public class SceneSchedule extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 场景编码 */
    private String sceneCode;

    /** 活动日期 */
    private LocalDate scheduleDate;

    /** 开始时间 */
    private LocalTime startTime;

    /** 结束时间 */
    private LocalTime endTime;

    /** 最大参与人数 */
    private Integer maxPerson;

    /** 已报名人数 */
    private Integer currentPerson;

    /** 当日特殊价格 */
    private BigDecimal priceOverride;

    /** 备注 */
    private String remark;

    /** 状态 */
    private Integer status;
}
