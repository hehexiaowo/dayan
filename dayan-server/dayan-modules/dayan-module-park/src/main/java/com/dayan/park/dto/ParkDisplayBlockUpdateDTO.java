package com.dayan.park.dto;

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
}
