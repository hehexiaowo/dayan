package com.dayan.agent.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代理人电子名片 VO（Agent 端）。
 *
 * <p>注意：id 为雪花 ID（19 位），超过 JS 安全整数范围（2^53-1），
 * 使用 {@link ToStringSerializer} 序列化为字符串，防止前端精度丢失。
 */
@Data
public class AgentCardVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String cardCode;
    private String agentCode;
    private String channelCode;

    private String cardName;
    private String displayName;
    private String title;
    private String phone;
    private String wechat;
    private String email;
    private String company;
    private String address;
    private String avatar;
    private String intro;
    private String tags;

    private Integer sortOrder;

    /** 状态：1=启用 0=停用 */
    private Integer status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
