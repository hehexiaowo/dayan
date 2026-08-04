package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.ParkServiceItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * park_service_item 数据访问层。
 */
@Mapper
public interface ParkServiceItemMapper extends BaseMapper<ParkServiceItem> {
}
