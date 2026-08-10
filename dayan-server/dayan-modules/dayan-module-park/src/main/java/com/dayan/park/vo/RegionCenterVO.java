package com.dayan.park.vo;

import lombok.Data;

/** 区域中心点（当前筛选范围内机构坐标的平均值）。 */
@Data
public class RegionCenterVO {
    private Double centerLng;
    private Double centerLat;
}
