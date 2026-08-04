package com.dayan.content.dto;

import lombok.Data;

/**
 * 内容信息更新入参（{@code contentCode} 不可改，由路径参数提供）。
 */
@Data
public class ContentInfoUpdateDTO {

    private String title;
    private String subtitle;
    private Integer contentType;
    private String categoryCode;
    private String authorName;
    private String authorAvatar;
    private String coverImage;
    private String summary;
    private String contentBody;
    private Integer sourceType;
    private String sourceUrl;
    private String tags;
    private Integer isTop;
    private Integer isRecommend;
    private Integer isComment;
    private Integer sortOrder;
    private String remark;
}
