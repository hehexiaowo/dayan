package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习中心内容创建入参（Admin 端）。
 *
 * <p>{@code contentCode} 由系统生成（LC 前缀 + 5 位序列）。
 */
@Data
public class LearningContentCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200)
    private String title;

    @Size(max = 500)
    private String summary;

    @NotNull(message = "板块分类不能为空")
    private Integer category;

    @Size(max = 100)
    private String author;

    @Size(max = 20)
    private String duration;

    private String body;

    @Size(max = 20)
    private String badge;

    private LocalDateTime publishTime;

    private Integer sortOrder;

    /** 状态（1=上架 0=下架），缺省 1 */
    private Integer status;
}
