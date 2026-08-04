package com.dayan.content.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享记录视图对象。
 */
@Data
public class ContentRecordShareVO {

    private Long id;
    private String contentCode;
    private String sharerType;
    private String sharerCode;
    private Integer shareChannel;
    private String shareUrl;
    private String shareTitle;
    private String shareDescription;
    private String shareImage;
    private Integer clickCount;
    private Integer convertCount;
    private LocalDateTime shareTime;
    private LocalDateTime createdAt;
}
