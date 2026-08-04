package com.dayan.client.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 表 client_health_profile 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("client_health_profile")
public class ClientHealthProfile extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 客户编码 */
    private String clientCode;

    /** 身高(cm) */
    private BigDecimal height;

    /** 体重(kg) */
    private BigDecimal weight;

    /** 血型 */
    private Integer bloodType;

    /** 血压 */
    private String bloodPressure;

    /** 血糖(mmol/L) */
    private BigDecimal bloodSugar;

    /** 心率(次/分) */
    private Integer heartRate;

    /** 慢性病列表 */
    private String chronicDiseases;

    /** 过敏史 */
    private String allergyHistory;

    /** 手术史 */
    private String surgeryHistory;

    /** 家族病史 */
    private String familyHistory;

    /** 当前用药信息 */
    private String medicationInfo;

    /** 行动能力 */
    private Integer mobilityLevel;

    /** 认知能力 */
    private Integer cognitiveLevel;

    /** 心理状态 */
    private Integer mentalStatus;

    /** 饮食偏好 */
    private String dietPreference;

    /** 睡眠质量 */
    private Integer sleepQuality;

    /** 紧急联系人姓名 */
    private String emergencyContactName;

    /** 紧急联系人电话 */
    private String emergencyContactPhone;

    /** 紧急联系人关系 */
    private String emergencyContactRelation;

    /** 健康评分 */
    private BigDecimal healthScore;

    /** 最近评估时间 */
    private LocalDateTime lastAssessmentTime;

    /** 备注 */
    private String remark;
}
