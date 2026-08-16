package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 确认大纲（前端可先微调节点文本，回传完整 outline JSON） */
@Data
public class AiOutlineConfirmDTO {
    /** AiOutlineVO 序列化 JSON：{coverImage, nodes[]} */
    @NotBlank(message = "大纲不能为空")
    private String outline;
}
