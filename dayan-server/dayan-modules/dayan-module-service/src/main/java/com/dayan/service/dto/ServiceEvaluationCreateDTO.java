package com.dayan.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 服务评价（service_evaluation）创建入参。
 *
 * <p>一会话一评价：同 sessionCode 仅允许 1 条评价，由应用层校验。
 * 4 维评分（attitudeRating/professionalRating/responsivenessRating/satisfactionRating，1-5）。
 * isAnonymous=1 时匿名展示。
 */
@Data
public class ServiceEvaluationCreateDTO {

    @NotBlank(message = "服务会话编码不能为空")
    private String sessionCode;

    private String clientCode;

    private String butlerCode;

    private String parkCode;

    /** 服务态度评分（1-5） */
    @Min(value = 1, message = "服务态度评分范围 1-5")
    @Max(value = 5, message = "服务态度评分范围 1-5")
    private Integer attitudeRating;

    /** 专业度评分（1-5） */
    @Min(value = 1, message = "专业度评分范围 1-5")
    @Max(value = 5, message = "专业度评分范围 1-5")
    private Integer professionalRating;

    /** 响应速度评分（1-5） */
    @Min(value = 1, message = "响应速度评分范围 1-5")
    @Max(value = 5, message = "响应速度评分范围 1-5")
    private Integer responsivenessRating;

    /** 满意度评分（1-5） */
    @Min(value = 1, message = "满意度评分范围 1-5")
    @Max(value = 5, message = "满意度评分范围 1-5")
    private Integer satisfactionRating;

    /** 评价内容 */
    private String content;

    /** 评价图片（JSON 数组字符串） */
    private String imageUrls;

    /** 是否匿名：0=否，1=是 */
    private Integer isAnonymous;
}
