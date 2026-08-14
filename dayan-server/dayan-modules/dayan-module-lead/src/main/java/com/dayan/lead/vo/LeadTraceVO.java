package com.dayan.lead.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索互动时间线项（内容/工具/海报三类记录的统一视图）。
 *
 * <p>id 为雪花 ID（19 位），序列化为字符串防止前端精度丢失。
 */
@Data
public class LeadTraceVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 互动类型：1=浏览内容 2=使用工具 3=查看海报 */
    private Integer traceType;
    /** 业务编码（content_code/tool_code/template_code） */
    private String bizCode;
    /** 业务标题（冗余快照） */
    private String bizTitle;
    /** 互动时间 */
    private LocalDateTime traceTime;
}
