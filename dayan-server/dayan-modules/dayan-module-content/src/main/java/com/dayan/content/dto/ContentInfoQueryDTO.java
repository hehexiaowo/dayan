package com.dayan.content.dto;

import lombok.Data;

/**
 * 内容信息查询入参。
 */
@Data
public class ContentInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String contentCode;
    private String title;
    private Integer contentType;
    private String categoryCode;
    private String authorName;
    /** 内容状态：0草稿/1待审/2通过/3拒绝/4下线 */
    private Integer contentStatus;
    private Integer auditStatus;
    private Integer isTop;
    private Integer isRecommend;
}
