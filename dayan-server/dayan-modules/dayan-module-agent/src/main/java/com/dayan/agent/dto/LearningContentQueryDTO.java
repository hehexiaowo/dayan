package com.dayan.agent.dto;

import lombok.Data;

/**
 * 学习中心内容查询入参（Admin 端）。
 */
@Data
public class LearningContentQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    /** 标题模糊 */
    private String title;
    /** 板块分类（1=渠道课程 2=外部课程 3=雁鸣中国） */
    private Integer category;
    /** 状态（1=上架 0=下架） */
    private Integer status;
}
