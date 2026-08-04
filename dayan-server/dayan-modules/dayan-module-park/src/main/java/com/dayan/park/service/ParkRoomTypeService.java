package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkRoomTypeCreateDTO;
import com.dayan.park.dto.ParkRoomTypeQueryDTO;
import com.dayan.park.dto.ParkRoomTypeUpdateDTO;
import com.dayan.park.vo.ParkRoomTypeVO;

import java.util.List;

/**
 * 房型（park_room_type）服务。
 *
 * <p>校验：{@code totalRooms >= availableRooms}；roomTypeCode 同 parkCode 下唯一。
 */
public interface ParkRoomTypeService {

    PageResult<ParkRoomTypeVO> page(ParkRoomTypeQueryDTO query);

    List<ParkRoomTypeVO> listByPark(String parkCode);

    ParkRoomTypeVO getDetail(Long id);

    Long create(ParkRoomTypeCreateDTO dto);

    void update(Long id, ParkRoomTypeUpdateDTO dto);

    void delete(Long id);
}
