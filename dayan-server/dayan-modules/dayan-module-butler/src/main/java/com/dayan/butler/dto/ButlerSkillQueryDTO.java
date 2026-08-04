package com.dayan.butler.dto;

import lombok.Data;

/**
 * 管家技能查询入参。
 */
@Data
public class ButlerSkillQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String butlerCode;
    private String skillCode;
    private String skillName;
    private Integer isCertified;
}
