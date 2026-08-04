package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkRoomPriceCreateDTO;
import com.dayan.park.dto.ParkRoomPriceQueryDTO;
import com.dayan.park.dto.ParkRoomPriceUpdateDTO;
import com.dayan.park.vo.ParkRoomPriceVO;

import java.util.List;

/**
 * 房型价格（park_room_price）服务。
 *
 * <p>校验：effectiveDate &lt; expireDate；isCurrent=1 同 roomTypeCode 下唯一。
 */
public interface ParkRoomPriceService {

    PageResult<ParkRoomPriceVO> page(ParkRoomPriceQueryDTO query);

    List<ParkRoomPriceVO> listByRoomType(String parkCode, String roomTypeCode);

    ParkRoomPriceVO getDetail(Long id);

    Long create(ParkRoomPriceCreateDTO dto);

    void update(Long id, ParkRoomPriceUpdateDTO dto);

    void delete(Long id);
}
