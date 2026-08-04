package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 探访记录（service_visit_record）创建入参。
 *
 * <p>按 butlerCode/parkCode 聚合；overallScore 综合评分；6 项检查（facility/service/hygiene/food/safety
 * 文本 + issuesFound 发现问题）。主键为 ASSIGN_ID 雪花，由服务端生成。
 */
@Data
public class ServiceVisitRecordCreateDTO {

    @NotBlank(message = "管家编码不能为空")
    private String butlerCode;

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    private LocalDate visitDate;

    /** 探访目的（1=常规巡检, 2=投诉核实, 3=入住回访, 4=突发事件, 5=其他） */
    private Integer visitPurpose;

    /** 设施检查情况 */
    private String facilityCheck;

    /** 服务检查情况 */
    private String serviceCheck;

    /** 卫生检查情况 */
    private String hygieneCheck;

    /** 餐饮检查情况 */
    private String foodCheck;

    /** 安全检查情况 */
    private String safetyCheck;

    /** 综合评分（0-100） */
    private BigDecimal overallScore;

    /** 发现问题 */
    private String issuesFound;

    /** 改进建议 */
    private String improvementSuggestions;

    /** 探访照片（JSON 数组字符串） */
    private String images;

    private String remark;
}
