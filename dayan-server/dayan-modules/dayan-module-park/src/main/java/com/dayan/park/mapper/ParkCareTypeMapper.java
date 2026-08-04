package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.ParkCareType;
import org.apache.ibatis.annotations.Mapper;

/**
 * park_care_type 数据访问层。
 */
@Mapper
public interface ParkCareTypeMapper extends BaseMapper<ParkCareType> {
}
