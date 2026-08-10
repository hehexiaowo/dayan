package com.dayan.park.service;

import com.dayan.park.dto.RegionQueryDTO;
import com.dayan.park.vo.CategoryCountVO;
import com.dayan.park.vo.ParkInfoVO;
import com.dayan.park.vo.RegionDrillResult;

import java.util.List;

/**
 * Agent 端机构查询服务（只读，不含增删改）。
 *
 * <p>所有查询都强制 is_published=1 + operate_status=1 + deleted=0，
 * 确保 agent 端只能看到已上线、对外的机构。
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

    /**
     * 获取已发布机构详情（agent 端专用）。
     *
     * <p>与 admin 端 {@link ParkInfoService#getDetail} 不同，此方法只返回
     * is_published=1 + operate_status=1 + deleted=0 的机构，
     * 避免暴露待审核/已下架机构详情。
     *
     * @param parkCode 机构编码
     * @return 机构详情 VO
     * @throws com.dayan.common.core.exception.BusinessException 机构不存在或未发布
     */
    ParkInfoVO getPublishedDetail(String parkCode);
}
