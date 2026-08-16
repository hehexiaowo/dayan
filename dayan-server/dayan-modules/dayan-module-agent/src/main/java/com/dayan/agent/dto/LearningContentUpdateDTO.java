package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习中心内容修改入参（Admin 端，id 走 path）。
 */
@Data
public class LearningContentUpdateDTO {

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

    private Integer status;
}
