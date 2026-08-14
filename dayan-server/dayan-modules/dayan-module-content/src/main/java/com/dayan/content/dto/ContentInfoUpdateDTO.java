package com.dayan.content.dto;

import jakarta.validation.constraints.Size;
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
    /** 适用业态（逗号分隔 vital/care/sojourn），空串=清空恢复全部 */
    @Size(max = 64)
    private String networkTags;
    private Integer isTop;
    private Integer isRecommend;
    private Integer isComment;
    private Integer sortOrder;
    private String remark;
}
