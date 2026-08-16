package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 段落勘误（最小化事实修订） */
@Data
public class AiReviseDTO {
    @NotBlank(message = "勘误意见不能为空")
    @Size(max = 500, message = "勘误意见不能超过 500 字")
    private String feedback;
    /** 锚文本：正文中唯一出现的片段，用于定位段落；空=全文意见 */
    @Size(max = 100, message = "锚文本不能超过 100 字")
    private String anchor;
}
