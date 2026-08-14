package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkPricingCreateDTO;
import com.dayan.park.dto.ParkPricingQueryDTO;
import com.dayan.park.dto.ParkPricingReviseDTO;
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

    /**
     * 取某维度当前生效定价（is_current=1）。
     * 优先 billing_cycle=1（月）；无月价取任意周期中 id 最大一条。不存在返回 null。
     */
    ParkPricingVO getCurrentFee(String parkCode, Integer chargeType, String refType, String refCode);

    /** 详情 */
    ParkPricingVO getDetail(Long id);

    /** 新增（自动创建 pricing_item 主行；isCurrent=1 时清除同组旧当前价） */
    Long create(ParkPricingCreateDTO dto);

    /** 修改（if-null-update；描述性字段） */
    void update(Long id, ParkPricingUpdateDTO dto);

    /**
     * 调价（版本化）：以 id 记录为基线新建价格版本。
     * effectiveDate&lt;=今天 → 立即生效（旧当前价置 0，新记录 is_current=1）；
     * 未来 → 预约生效（新记录 pending_flag=1, is_current=0；同维度旧 pending 记录逻辑删除）。
     * 返回新记录 id。
     */
    Long revise(Long id, ParkPricingReviseDTO dto);

    /** 删除（级联删 pricing_item） */
    void delete(Long id);
}
