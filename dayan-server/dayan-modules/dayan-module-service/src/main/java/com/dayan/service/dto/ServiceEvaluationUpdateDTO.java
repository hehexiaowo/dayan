package com.dayan.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务评价（service_evaluation）更新入参（按 id 更新）。
 *
 * <p>支持修改评分/内容/匿名标记，以及回复相关字段（replyContent/replyByCode）。
 * 评分范围 1-5。
 */
@Data
public class ServiceEvaluationUpdateDTO {

    @Min(value = 1, message = "服务态度评分范围 1-5")
    @Max(value = 5, message = "服务态度评分范围 1-5")
    private Integer attitudeRating;

    @Min(value = 1, message = "专业度评分范围 1-5")
    @Max(value = 5, message = "专业度评分范围 1-5")
    private Integer professionalRating;

    @Min(value = 1, message = "响应速度评分范围 1-5")
    @Max(value = 5, message = "响应速度评分范围 1-5")
    private Integer responsivenessRating;

    @Min(value = 1, message = "满意度评分范围 1-5")
    @Max(value = 5, message = "满意度评分范围 1-5")
    private Integer satisfactionRating;

    private String content;
    private String imageUrls;
    private Integer isAnonymous;

    /** 回复内容（运营回复） */
    private String replyContent;
    /** 回复人编码 */
    private String replyByCode;
    /** 回复时间（一般由服务端写当前时间，可显式传入） */
    private LocalDateTime replyTime;

    private Integer status;
}
