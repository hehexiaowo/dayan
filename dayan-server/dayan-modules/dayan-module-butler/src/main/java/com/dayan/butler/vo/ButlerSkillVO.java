package com.dayan.butler.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管家技能 VO。
 */
@Data
public class ButlerSkillVO {

    private Long id;
    /** 管家编码 */
    private String butlerCode;
    /** 技能编码 */
    private String skillCode;
    /** 技能名称 */
    private String skillName;
    /** 熟练度 */
    private Integer proficiency;
    /** 是否持证：0=否 / 1=是 */
    private Integer isCertified;
    /** 证书编号 */
    private String certificateNo;
    /** 取得日期 */
    private LocalDate obtainDate;
    /** 排序号 */
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
