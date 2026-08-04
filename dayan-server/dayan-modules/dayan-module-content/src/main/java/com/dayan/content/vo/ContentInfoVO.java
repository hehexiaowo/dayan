package com.dayan.content.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容信息视图对象。
 */
@Data
public class ContentInfoVO {

    private Long id;
    private String contentCode;
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
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer collectCount;
    private LocalDateTime publishTime;
    private Integer sortOrder;
    /** 内容状态：0草稿/1待审/2通过/3拒绝/4下线 */
    private Integer contentStatus;
    private Integer auditStatus;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
