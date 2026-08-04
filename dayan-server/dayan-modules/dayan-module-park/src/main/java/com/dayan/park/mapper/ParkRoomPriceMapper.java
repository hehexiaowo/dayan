package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.ParkRoomPrice;
import org.apache.ibatis.annotations.Mapper;

/**
 * park_room_price 数据访问层。
 */
@Mapper
public interface ParkRoomPriceMapper extends BaseMapper<ParkRoomPrice> {
}
