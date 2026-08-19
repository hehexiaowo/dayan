package com.dayan.system.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增类目 DTO */
@Data
public class SystemCategoryAddDTO {

    @NotBlank(message = "类目名称不能为空")
    @Size(max = 100, message = "类目名称最长 100 字")
    private String categoryName;

    /** 父类目 ID（空=顶级类目） */
    private String parentCategoryId;
}
