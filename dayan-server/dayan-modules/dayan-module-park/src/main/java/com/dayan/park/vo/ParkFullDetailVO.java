package com.dayan.park.vo;

import lombok.Data;

import java.util.List;

/**
 * Agent 端机构完整详情 VO（聚合主表 + 全部子实体）。
 *
 * <p>供 GET /agent-api/park/{parkCode}/full 接口返回，
 * 前端详情页 Tab 数据全部从此 VO 取。
 */
@Data
public class ParkFullDetailVO {

    /** 机构主信息 */
    private ParkInfoVO parkInfo;

    /** 媒体素材（banner 轮播图从此取 assetType=1 图片） */
    private List<ParkAssetVO> assets;

    /** 房型 */
    private List<ParkRoomTypeVO> roomTypes;

    /** 收费方案 */
    private List<ParkPricingVO> pricingList;

    /** 照护等级 */
    private List<ParkCareTypeVO> careTypes;

    /** 餐饮类型 */
    private List<ParkFoodTypeVO> foodTypes;

    /** 设施类型 */
    private List<ParkFacilityTypeVO> facilityTypes;

    /** 服务类型 */
    private List<ParkServiceTypeVO> serviceTypes;

    /** 周边配套（交通/景点/医疗/购物） */
    private List<ParkPeripheryVO> peripheries;

    /** 评分（单条，可能为 null） */
    private ParkScoreVO score;

    /** 图文展示板块 */
    private List<ParkDisplayBlockVO> displayBlocks;
}
