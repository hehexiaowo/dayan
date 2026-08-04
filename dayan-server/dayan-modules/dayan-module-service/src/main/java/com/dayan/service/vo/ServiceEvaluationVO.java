package com.dayan.service.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务评价 VO。
 */
@Data
public class ServiceEvaluationVO {

    private Long id;
    private String sessionCode;
    private String clientCode;
    private String butlerCode;
    private String parkCode;
    private Integer attitudeRating;
    private Integer professionalRating;
    private Integer responsivenessRating;
    private Integer satisfactionRating;
    private String content;
    private String imageUrls;
    private Integer isAnonymous;
    private String replyContent;
    private LocalDateTime replyTime;
    private String replyByCode;
    private Integer status;
    private LocalDateTime createdAt;
}
