package com.dayan.park.vo;

import lombok.Data;

import java.util.List;

/**
 * 区域下钻结果。
 *
 * <p>当 level != park 时，{@link #items} 有值、{@link #parkList} 为 null（显示下级区域列表）。
 * 当 level == park 时，{@link #parkList} 有值、{@link #items} 为 null（显示机构卡片清单）。
 */
@Data
public class RegionDrillResult {

    /** 当前层级：province/city/district/park */
    private String level;

    /** 面包屑文案，如 "活力长居 / 北京 / 昌平区" */
    private String breadcrumb;

    /** 下级区域列表（level != park 时有值） */
    private List<RegionItem> items;

    /** 机构卡片清单（level == park 时有值） */
    private List<ParkCardVO> parkList;

    /** 当前层级地图中心点经度（范围内机构 AVG，无坐标数据时为 null） */
    private Double centerLng;

    /** 当前层级地图中心点纬度 */
    private Double centerLat;
}
