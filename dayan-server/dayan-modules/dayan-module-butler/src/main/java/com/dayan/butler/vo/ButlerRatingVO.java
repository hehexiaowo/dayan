package com.dayan.butler.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管家评价 VO。
 */
@Data
public class ButlerRatingVO {

    private Long id;
    /** 管家编码 */
    private String butlerCode;
    /** 客户编码 */
    private String clientCode;
    /** 关联服务记录编码 */
    private String serviceRecordCode;
    /** 评分：1-5 */
    private Integer rating;
    /** 评价内容 */
    private String content;
    /** 状态 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
