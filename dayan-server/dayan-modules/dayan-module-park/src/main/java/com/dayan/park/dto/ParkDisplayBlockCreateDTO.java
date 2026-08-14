package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构展示板块创建入参。
 */
@Data
public class ParkDisplayBlockCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "板块类型不能为空")
    @Size(max = 50)
    private String blockType;

    @Size(max = 100)
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
