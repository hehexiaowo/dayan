package com.dayan.agent.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习中心内容 VO（Agent 端）。
 *
 * <p>id 为雪花 ID，序列化为字符串防止前端精度丢失。
 */
@Data
public class LearningContentVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String contentCode;
    private String title;
    private String summary;
    /** 分类 1=视频课程 2=图文课程 3=雁鸣中国 */
    private Integer category;
    private String author;
    private String duration;
    private Integer viewCount;
    private String badge;
    private LocalDateTime publishTime;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
