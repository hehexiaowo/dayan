package com.dayan.park.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 下级区域项（省份/城市/区县）。
 */
@Data
@AllArgsConstructor
public class RegionItem {

    /** 行政区划码（provinceCode/cityCode/districtCode） */
    private String code;

    /** 区域名（北京/南京市/昌平区） */
    private String name;

    /** 该区域下的机构数 */
    private Integer count;
}
