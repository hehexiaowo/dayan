package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkFoodTypeCreateDTO;
import com.dayan.park.dto.ParkFoodTypeQueryDTO;
import com.dayan.park.dto.ParkFoodTypeUpdateDTO;
import com.dayan.park.vo.ParkFoodTypeVO;

import java.util.List;

/**
 * 餐饮类型（park_food_type）服务。
 */
public interface ParkFoodTypeService {

    PageResult<ParkFoodTypeVO> page(ParkFoodTypeQueryDTO query);

    List<ParkFoodTypeVO> listByPark(String parkCode);

    ParkFoodTypeVO getDetail(Long id);

    Long create(ParkFoodTypeCreateDTO dto);

    void update(Long id, ParkFoodTypeUpdateDTO dto);

    void delete(Long id);
}
