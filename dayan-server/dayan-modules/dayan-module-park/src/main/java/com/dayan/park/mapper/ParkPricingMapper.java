package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.ParkPricing;
import org.apache.ibatis.annotations.Mapper;

/** 机构统一定价方案 Mapper */
@Mapper
public interface ParkPricingMapper extends BaseMapper<ParkPricing> {
}
