package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkFoodPriceCreateDTO;
import com.dayan.park.dto.ParkFoodPriceQueryDTO;
import com.dayan.park.dto.ParkFoodPriceUpdateDTO;
import com.dayan.park.vo.ParkFoodPriceVO;

import java.util.List;

/**
 * 餐饮价格（park_food_price）服务。
 *
 * <p>校验：effectiveDate &lt; expireDate；isCurrent=1 同 foodTypeCode 下唯一。
 */
public interface ParkFoodPriceService {

    PageResult<ParkFoodPriceVO> page(ParkFoodPriceQueryDTO query);

    List<ParkFoodPriceVO> listByFoodType(String parkCode, String foodTypeCode);

    ParkFoodPriceVO getDetail(Long id);

    Long create(ParkFoodPriceCreateDTO dto);

    void update(Long id, ParkFoodPriceUpdateDTO dto);

    void delete(Long id);
}
