package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.ParkInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * park_info 数据访问层。
 */
@Mapper
public interface ParkInfoMapper extends BaseMapper<ParkInfo> {
}
