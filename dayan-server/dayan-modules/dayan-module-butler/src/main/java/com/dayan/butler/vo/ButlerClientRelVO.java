package com.dayan.butler.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管家-客户绑定关系 VO。
 */
@Data
public class ButlerClientRelVO {

    private Long id;
    /** 管家编码 */
    private String butlerCode;
    /** 客户编码 */
    private String clientCode;
    /** 绑定时间 */
    private LocalDateTime bindTime;
    /** 状态：0=已解绑 / 1=有效 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
