package com.dayan.content.vo;

import lombok.Data;

/**
 * 内容阅读统计（UV/PV）。
 *
 * <ul>
 *   <li>{@code pv}：阅读记录总条数（Page View）</li>
 *   <li>{@code uv}：按 readerCode 去重的访客数（Unique Visitor）</li>
 * </ul>
 */
@Data
public class ContentReadStatsVO {

    private String contentCode;
    /** 阅读次数（记录总数） */
    private Long pv;
    /** 独立访客数（按 readerCode 去重） */
    private Long uv;
}
