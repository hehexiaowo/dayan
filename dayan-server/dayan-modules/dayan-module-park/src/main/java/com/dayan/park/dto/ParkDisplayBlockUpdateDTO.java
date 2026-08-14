package com.dayan.park.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构展示板块更新入参。
 *
 * <p>parkCode、blockType 不可修改（板块类型决定 C 端渲染模板）。
 */
@Data
public class ParkDisplayBlockUpdateDTO {

    private String blockTitle;
    private String content;
    private String images;
    private String imageDescriptions;
    private Integer sortOrder;
    private Integer status;

    /** 适用业态（逗号分隔 vital/care/sojourn），空=全部 */
    @Size(max = 64, message = "业态串过长")
    private String networkTags;
}
