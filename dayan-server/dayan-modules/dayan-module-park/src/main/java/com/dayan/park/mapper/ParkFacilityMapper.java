package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.ParkFacility;
import org.apache.ibatis.annotations.Mapper;

/**
 * park_facility 数据访问层。
 */
@Mapper
public interface ParkFacilityMapper extends BaseMapper<ParkFacility> {
}
