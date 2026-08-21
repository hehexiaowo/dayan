package com.dayan.tool.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 选择转写方案（单选） */
@Data
public class ArticleRewritePlanSelectDTO {

    /** 选中的转写方案ID（单选） */
    @NotBlank(message = "请选择一个转写方案")
    private String planId;
}
