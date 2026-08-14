package com.dayan.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 内容信息创建入参。
 *
 * <p>{@code contentCode} 由系统生成（CT 前缀），{@code contentStatus} 初始为 0（草稿）。
 */
@Data
public class ContentInfoCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200)
    private String title;

    @Size(max = 200)
    private String subtitle;

    /** 内容类型：1文章/2视频/3图片集/4专题/5问答 */
    @NotNull(message = "内容类型不能为空")
    private Integer contentType;

    @Size(max = 50)
    private String categoryCode;

    @Size(max = 50)
    private String authorName;

    private String authorAvatar;
    private String coverImage;
    private String summary;
    private String contentBody;

    /** 来源类型 */
    private Integer sourceType;
    private String sourceUrl;
    private String tags;
    /** 适用业态（逗号分隔 vital/care/sojourn），空=全部 */
    @Size(max = 64)
    private String networkTags;
    private Integer isTop;
    private Integer isRecommend;
    private Integer isComment;
    private Integer sortOrder;
    private String remark;
}
