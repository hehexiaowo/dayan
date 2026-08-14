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
    /** 渠道隔离用：本渠道已配置的内容编码集合 */
    private java.util.List<String> contentCodes;
    /** 业态过滤（单业态 vital/care/sojourn；空=不过滤） */
    private String network;
}
