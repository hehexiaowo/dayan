package com.dayan.tool.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 大纲重生成反馈 */
@Data
public class AiOutlineRegenDTO {
    @Size(max = 200, message = "反馈不能超过 200 字")
    private String feedback;
}
