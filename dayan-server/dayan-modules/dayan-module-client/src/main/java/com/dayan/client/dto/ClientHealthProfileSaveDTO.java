package com.dayan.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户健康档案保存入参（upsert：一客户一档案，有则更新无则新增）。
 */
@Data
public class ClientHealthProfileSaveDTO {

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 64)
    private String clientCode;

    /** 身高(cm) */
    private BigDecimal height;

    /** 体重(kg) */
    private BigDecimal weight;

    /** 血型 */
    private Integer bloodType;

    @Size(max = 32)
    private String bloodPressure;

    private BigDecimal bloodSugar;
    private Integer heartRate;

    /** 慢性病列表（JSON 字符串） */
    private String chronicDiseases;

    /** 过敏史（JSON 字符串） */
    private String allergyHistory;

    /** 手术史（JSON 字符串） */
    private String surgeryHistory;

    /** 家族病史（JSON 字符串） */
    private String familyHistory;

    /** 当前用药信息（JSON 字符串） */
    private String medicationInfo;

    private Integer mobilityLevel;
    private Integer cognitiveLevel;
    private Integer mentalStatus;
    private String dietPreference;
    private Integer sleepQuality;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private BigDecimal healthScore;
    private String remark;
}
