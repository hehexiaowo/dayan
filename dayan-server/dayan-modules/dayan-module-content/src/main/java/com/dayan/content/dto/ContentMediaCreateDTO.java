package com.dayan.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 多媒体资源创建入参。
 */
@Data
public class ContentMediaCreateDTO {

    @NotBlank(message = "内容编码不能为空")
    private String contentCode;

    /** 媒体类型：1图片/2视频/3音频/4文件 */
    @NotNull(message = "媒体类型不能为空")
    private Integer mediaType;

    @NotBlank(message = "资源URL不能为空")
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
