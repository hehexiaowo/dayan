package com.dayan.butler.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 管家技能创建入参。
 */
@Data
public class ButlerSkillCreateDTO {

    @NotBlank(message = "管家编码不能为空")
    private String butlerCode;

    @NotBlank(message = "技能编码不能为空")
    private String skillCode;

    private String skillName;
    private Integer proficiency;
    private Integer isCertified;
    private String certificateNo;
    private LocalDate obtainDate;
    private Integer sortOrder;
}
