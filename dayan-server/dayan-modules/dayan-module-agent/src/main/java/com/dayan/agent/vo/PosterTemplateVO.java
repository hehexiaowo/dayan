package com.dayan.agent.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 营销海报模板 VO（代理人端只读）。
 */
@Data
public class PosterTemplateVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String templateCode;
    private String title;
    private String subtitle;
    private String bodyText;
    private String coverImage;
    private String categoryCode;
    private String categoryName;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
