package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.ParkRoomType;
import org.apache.ibatis.annotations.Mapper;

/**
 * park_room_type 数据访问层。
 */
@Mapper
public interface ParkRoomTypeMapper extends BaseMapper<ParkRoomType> {
}
