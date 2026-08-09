package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkPricingCreateDTO;
import com.dayan.park.dto.ParkPricingQueryDTO;
import com.dayan.park.dto.ParkPricingUpdateDTO;
import com.dayan.park.vo.ParkPricingVO;

import java.util.List;

/**
 * 机构统一定价方案服务。
 */
public interface ParkPricingService {

    /** 分页查询 */
    PageResult<ParkPricingVO> page(ParkPricingQueryDTO query);

    /** 按关联类型+编码列表（展开行专用） */
    List<ParkPricingVO> listByRef(String parkCode, String refType, String refCode);

    /** 按费类列表（如"押金/房间/照护/餐费"维度查看） */
    List<ParkPricingVO> listByChargeType(String parkCode, Integer chargeType);

    /** 详情 */
    ParkPricingVO getDetail(Long id);

    /** 新增（自动创建 pricing_item 主行；isCurrent=1 时清除同组旧当前价） */
    Long create(ParkPricingCreateDTO dto);

    /** 修改（if-null-update；isCurrent 变更触发 clearOtherCurrent） */
    void update(Long id, ParkPricingUpdateDTO dto);

    /** 删除（级联删 pricing_item） */
    void delete(Long id);
}
