package com.dayan.content.dto;

import lombok.Data;

/**
 * 多媒体资源更新入参。
 */
@Data
public class ContentMediaUpdateDTO {

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
}
