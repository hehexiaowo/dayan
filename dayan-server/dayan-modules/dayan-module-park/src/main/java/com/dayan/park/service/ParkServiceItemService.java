package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkServiceItemCreateDTO;
import com.dayan.park.dto.ParkServiceItemQueryDTO;
import com.dayan.park.dto.ParkServiceItemUpdateDTO;
import com.dayan.park.vo.ParkServiceItemVO;

import java.util.List;

/**
 * 机构服务项（park_service_item）服务。
 */
public interface ParkServiceItemService {

    PageResult<ParkServiceItemVO> page(ParkServiceItemQueryDTO query);

    List<ParkServiceItemVO> listByPark(String parkCode);

    ParkServiceItemVO getDetail(Long id);

    Long create(ParkServiceItemCreateDTO dto);

    void update(Long id, ParkServiceItemUpdateDTO dto);

    void delete(Long id);
}
