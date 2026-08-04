package com.dayan.client.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户收藏 VO。
 */
@Data
public class ClientFavoriteVO {

    private Long id;
    private String clientCode;
    private Integer targetType;
    private String targetCode;
    private String targetName;
    private String remark;
    private LocalDateTime createdAt;
}
