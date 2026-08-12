package com.dayan.agent.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索互动记录 VO。
 */
@Data
public class AgentLeadTraceVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 互动类型（1=浏览内容, 2=使用工具, 3=查看海报） */
    private Integer traceType;

    /** 业务编码 */
    private String bizCode;

    /** 展示标题 */
    private String bizTitle;

    /** 互动时间 */
    private LocalDateTime traceTime;
}
