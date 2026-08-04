package com.dayan.butler.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 管家技能修改入参（非空字段才更新）。
 */
@Data
public class ButlerSkillUpdateDTO {

    private String skillName;
    private Integer proficiency;
    private Integer isCertified;
    private String certificateNo;
    private LocalDate obtainDate;
    private Integer sortOrder;
}
