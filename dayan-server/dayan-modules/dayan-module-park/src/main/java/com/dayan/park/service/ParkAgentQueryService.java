package com.dayan.park.service;

import com.dayan.park.dto.RegionQueryDTO;
import com.dayan.park.vo.CategoryCountVO;
import com.dayan.park.vo.RegionDrillResult;

import java.util.List;

/**
 * Agent 端机构查询服务（只读，不含增删改）。
 */
public interface ParkAgentQueryService {

    /**
     * 三分类机构数量统计（分类入口页用）。
     * 活力长居 = ability_type=1(CCRC)
     * 照护长居 = ability_type IN (2,3,4,7) 养老院/CB/认知症/NH
     * 旅居养老 = 无数据(available=false)
     */
    List<CategoryCountVO> countByCategory();

    /**
     * 区域下钻查询。
     */
    RegionDrillResult drillRegion(RegionQueryDTO query);
}
