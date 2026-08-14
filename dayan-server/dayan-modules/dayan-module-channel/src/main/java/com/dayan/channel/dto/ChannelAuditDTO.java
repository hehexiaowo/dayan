package com.dayan.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 渠道审核入参。
 *
 * <p>审核动作：auditStatus 1=通过 / 2=驳回。
 */
@Data
public class ChannelAuditDTO {

    @NotBlank(message = "渠道编码不能为空")
    private String channelCode;

    /** 审核状态：1=通过 / 2=驳回 */
    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String auditRemark;
}
