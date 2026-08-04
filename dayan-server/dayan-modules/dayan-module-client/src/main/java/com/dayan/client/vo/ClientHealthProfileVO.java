package com.dayan.client.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户健康档案 VO。
 */
@Data
public class ClientHealthProfileVO {

    private Long id;
    private String clientCode;
    private BigDecimal height;
    private BigDecimal weight;
    private Integer bloodType;
    private String bloodPressure;
    private BigDecimal bloodSugar;
    private Integer heartRate;
    private String chronicDiseases;
    private String allergyHistory;
    private String surgeryHistory;
    private String familyHistory;
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
    private LocalDateTime lastAssessmentTime;
    private String remark;
    private LocalDateTime createdAt;
}
