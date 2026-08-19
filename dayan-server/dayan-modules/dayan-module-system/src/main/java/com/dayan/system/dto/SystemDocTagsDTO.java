package com.dayan.system.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 文件标签更新 DTO（≤10 个，空列表=清空） */
@Data
public class SystemDocTagsDTO {

    @Size(max = 10, message = "标签最多 10 个")
    private List<String> tags;
}
