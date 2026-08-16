package com.dayan.agent.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 标题重生成反馈 */
@Data
public class AiTitleRegenDTO {
    @Size(max = 200, message = "反馈不能超过 200 字")
    private String feedback;
}
