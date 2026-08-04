package com.dayan.butler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 管家评价修改入参（非空字段才更新）。
 */
@Data
public class ButlerRatingUpdateDTO {

    @Min(value = 1, message = "评分最低 1 分")
    @Max(value = 5, message = "评分最高 5 分")
    private Integer rating;

    private String content;
    private Integer status;
}
