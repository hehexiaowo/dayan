package com.dayan.tool.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 素材块（前端供材的最小单元） */
@Data
public class AiMaterialBlockDTO {
    /** ref=范文/kb=知识库/goods=商品/park=机构 */
    @NotBlank(message = "素材类型必填")
    private String type;
    @NotBlank(message = "素材标题必填")
    private String title;
    @NotBlank(message = "素材正文必填")
    private String text;
}
