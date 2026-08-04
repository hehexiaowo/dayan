package com.dayan.butler.dto;

import lombok.Data;

/**
 * 管家-客户绑定关系查询入参。
 */
@Data
public class ButlerClientRelQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String butlerCode;
    private String clientCode;
    /** 状态：0=已解绑 / 1=有效 */
    private Integer status;
}
