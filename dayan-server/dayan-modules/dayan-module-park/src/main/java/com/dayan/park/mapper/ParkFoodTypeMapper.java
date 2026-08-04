package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.ParkFoodType;
import org.apache.ibatis.annotations.Mapper;

/**
 * park_food_type 数据访问层。
 */
@Mapper
public interface ParkFoodTypeMapper extends BaseMapper<ParkFoodType> {
}
