package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.ParkCarePrice;
import org.apache.ibatis.annotations.Mapper;

/**
 * park_care_price 数据访问层。
 */
@Mapper
public interface ParkCarePriceMapper extends BaseMapper<ParkCarePrice> {
}
