package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代理人分享记录创建入参。
 *
 * <p>share_code 由服务端 UUID 生成（uk_share_code 兜底）；share_time 默认当前时间。
 */
@Data
public class AgentShareRecordCreateDTO {

    @NotBlank(message = "代理人编码不能为空")
    @Size(max = 50)
    private String agentCode;

    /** 分享类型（1=内容, 2=场景, 3=机构, 4=权益, 5=课程） */
    @NotNull(message = "分享类型不能为空")
    private Integer shareType;

    @NotBlank(message = "分享对象编码不能为空")
    @Size(max = 64)
    private String bizCode;

    /** 分享渠道（1=微信, 2=朋友圈, 3=复制链接, 4=二维码, 5=短信） */
    private Integer shareChannel;

    @Size(max = 50)
    private String clientCode;
}
