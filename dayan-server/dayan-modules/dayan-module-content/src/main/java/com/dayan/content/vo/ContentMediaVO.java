package com.dayan.content.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 多媒体资源视图对象。
 */
@Data
public class ContentMediaVO {

    private Long id;
    private String contentCode;
    private Integer mediaType;
    private String mediaUrl;
    private String thumbnailUrl;
    private String mediaName;
    private String fileFormat;
    private Integer fileSize;
    private Integer width;
    private Integer height;
    private Integer duration;
    private String mediaDescription;
    private Integer isInBody;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
