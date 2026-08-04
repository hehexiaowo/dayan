package com.dayan.scene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 场景日程创建入参。
 *
 * <p>按 {@code sceneCode} 维度管理日程，{@code currentPerson} 必须 ≤ {@code maxPerson}，
 * 否则抛 BusinessException("已报名人数不能超过最大参与人数")。
 */
@Data
public class SceneScheduleCreateDTO {

    @NotBlank(message = "场景编码不能为空")
    private String sceneCode;

    @NotNull(message = "活动日期不能为空")
    private LocalDate scheduleDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @NotNull(message = "最大参与人数不能为空")
    private Integer maxPerson;

    /** 已报名人数，默认 0，必须 ≤ maxPerson */
    private Integer currentPerson;

    private BigDecimal priceOverride;
    private String remark;
    /** 状态：1开放 / 2已约满 / 3关闭，默认 1 */
    private Integer status;
}
