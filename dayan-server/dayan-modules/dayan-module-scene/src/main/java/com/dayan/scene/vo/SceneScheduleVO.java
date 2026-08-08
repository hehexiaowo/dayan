package com.dayan.scene.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 场景日程 VO。
 */
@Data
public class SceneScheduleVO {

    private Long id;
    private String sceneCode;
    /**
     * 场景名称（渠道端回填，非 scene_schedule 原生字段）。
     * 由 Controller 分页后按 sceneCode 批量查 scene_info 组装 Map 回填。
     */
    private String sceneName;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPerson;
    private Integer currentPerson;
    private BigDecimal priceOverride;
    private String remark;
    /**
     * 状态（DDL 权威：0=已取消 / 1=可预约 / 2=已约满 / 3=进行中 / 4=已结束）。
     */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
