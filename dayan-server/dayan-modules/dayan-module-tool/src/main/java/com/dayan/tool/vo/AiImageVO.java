package com.dayan.tool.vo;

import lombok.Data;

/** 配图结果（images JSON 数组元素） */
@Data
public class AiImageVO {
    private String placeholder;
    private String size;
    private String prompt;
    private String promptZh;
    private String fileKey;
    private String url;
    /** pending/generating/done/failed/skipped */
    private String status;
    private String error;
}
