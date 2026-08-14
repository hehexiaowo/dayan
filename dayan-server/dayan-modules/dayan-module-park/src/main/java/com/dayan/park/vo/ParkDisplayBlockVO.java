package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构展示板块 VO。
 */
@Data
public class ParkDisplayBlockVO {

    private Long id;
    private String parkCode;
    private String blockType;
    private String blockTitle;
    private String content;
    private String images;
    private String imageDescriptions;
    private Integer sortOrder;
    private Integer status;

    /** 适用业态（空列表=全部业态） */
    private java.util.List<String> networkTags;
    private LocalDateTime createdAt;
}
