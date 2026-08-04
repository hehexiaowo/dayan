package com.dayan.butler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管家评价创建入参。
 *
 * <p>评分 rating 取值 1-5。
 */
@Data
public class ButlerRatingCreateDTO {

    @NotBlank(message = "管家编码不能为空")
    private String butlerCode;

    @NotBlank(message = "客户编码不能为空")
    private String clientCode;

    /** 关联服务记录编码（可选） */
    private String serviceRecordCode;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低 1 分")
    @Max(value = 5, message = "评分最高 5 分")
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 状态，默认 1 */
    private Integer status;
}
