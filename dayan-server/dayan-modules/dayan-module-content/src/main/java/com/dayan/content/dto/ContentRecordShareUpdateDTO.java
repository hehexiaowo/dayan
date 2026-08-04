package com.dayan.content.dto;

import lombok.Data;

/**
 * 分享记录更新入参（主要用于回填点击/转化数据）。
 */
@Data
public class ContentRecordShareUpdateDTO {

    private Integer clickCount;
    private Integer convertCount;
}
