package com.dayan.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享记录创建入参。
 *
 * <p>记录分享事件（前端上报），id 由 ASSIGN_ID 自动生成。
 */
@Data
public class ContentRecordShareCreateDTO {

    @NotBlank(message = "内容编码不能为空")
    private String contentCode;

    private String sharerType;
    private String sharerCode;

    /** 分享渠道：1微信/2朋友圈/3QQ/4微博/5复制链接/99其它 */
    private Integer shareChannel;

    private String shareUrl;
    private String shareTitle;
    private String shareDescription;
    private String shareImage;
    private LocalDateTime shareTime;
}
