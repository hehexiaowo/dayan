package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.ParkFoodPrice;
import org.apache.ibatis.annotations.Mapper;

/**
 * park_food_price 数据访问层。
 */
@Mapper
public interface ParkFoodPriceMapper extends BaseMapper<ParkFoodPrice> {
}
